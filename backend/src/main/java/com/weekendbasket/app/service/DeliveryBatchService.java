package com.weekendbasket.app.service;

import com.weekendbasket.app.dto.DeliveryDto.*;
import com.weekendbasket.app.event.BatchCompletedEvent;
import com.weekendbasket.app.event.OrderDeliveredEvent;
import com.weekendbasket.app.exception.ResourceNotFoundException;
import com.weekendbasket.app.exception.WeekendBasketException;
import com.weekendbasket.app.model.*;
import com.weekendbasket.app.repository.*;
import com.weekendbasket.app.statemachine.StateTransition;
import com.weekendbasket.app.statemachine.StateTransitionWorker;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DeliveryBatchService {

    private static final Logger log = LogManager.getLogger(DeliveryBatchService.class);

    private final DeliveryBatchRepository batchRepository;
    private final DeliveryBatchOrderRepository batchOrderRepository;
    private final CustomerOrderRepository orderRepository;
    private final WeeklyCycleRepository cycleRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;
    private final StateTransitionWorker stateTransitionWorker;
    private final ApplicationEventPublisher eventPublisher;
    private final TransportTrackingService transportTrackingService;

    @Transactional(readOnly = true)
    public List<BatchResponse> getBatchesByCycle(Long cycleId) {
        return batchRepository.findByCycleId(cycleId).stream()
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<BatchResponse> getMyBatches(Long staffUserId) {
        return batchRepository.findByAssignedToId(staffUserId).stream()
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public BatchResponse getById(Long batchId) {
        return toResponse(find(batchId));
    }

    @Transactional
    public BatchResponse createBatch(CreateBatchRequest request) {
        WeeklyCycle cycle = cycleRepository.findById(request.cycleId())
                .orElseThrow(() -> new ResourceNotFoundException("Cycle not found: " + request.cycleId()));

        User assignedTo = null;
        if (request.assignedToUserId() != null) {
            assignedTo = userRepository.findById(request.assignedToUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("Staff user not found: " + request.assignedToUserId()));
        }

        DeliveryBatch batch = DeliveryBatch.builder()
                .cycle(cycle)
                .batchLabel(request.batchLabel())
                .deliveryDate(request.deliveryDate())
                .assignedTo(assignedTo)
                .build();
        batchRepository.save(batch);

        for (Long orderId : request.orderIds()) {
            if (batchOrderRepository.existsByOrderId(orderId)) {
                throw new WeekendBasketException("Order " + orderId + " is already assigned to a batch.");
            }
            CustomerOrder order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
            batchOrderRepository.save(DeliveryBatchOrder.builder().batch(batch).order(order).build());
        }

        log.info("Delivery batch created: {} with {} orders", batch.getBatchLabel(), request.orderIds().size());

        // Auto-trigger IN_TRANSIT when first batch is created for a cycle
        transportTrackingService.autoAddStage(request.cycleId(), "IN_TRANSIT",
                "Delivery batches created — goods in transit to community");

        return toResponse(batch);
    }

    @Transactional
    public BatchResponse assignStaff(Long batchId, Long staffUserId) {
        DeliveryBatch batch = find(batchId);
        User staff = userRepository.findById(staffUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff user not found: " + staffUserId));
        batch.setAssignedTo(staff);
        batchRepository.save(batch);

        // Auto-trigger ARRIVED + PACKING + DISPATCHED when staff assigned
        Long cycleId = batch.getCycle().getId();
        transportTrackingService.autoAddStage(cycleId, "ARRIVED",
                "Goods arrived — delivery staff assigned");
        transportTrackingService.autoAddStage(cycleId, "PACKING",
                "Packing in progress");
        transportTrackingService.autoAddStage(cycleId, "DISPATCHED",
                "Out for delivery — delivery staff dispatched");

        return toResponse(batch);
    }

    @Transactional
    public void markOrderDelivered(Long batchId, Long orderId) {
        DeliveryBatchOrder batchOrder = batchOrderRepository.findByBatchId(batchId)
                .stream()
                .filter(bo -> bo.getOrder().getId().equals(orderId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order " + orderId + " not found in batch " + batchId));
        CustomerOrder order = batchOrder.getOrder();

        // Already delivered — idempotent, skip
        if ("DELIVERED".equals(order.getStatus())) {
            log.info("Order {} already delivered, skipping", orderId);
            return;
        }

        // Auto-advance through intermediate states via state machine
        // PLACED → CONFIRMED → PACKED → DELIVERED
        if ("PLACED".equals(order.getStatus())) {
            stateTransitionWorker.apply("ORDER", order.getStatus(), "CONFIRMED", "ADMIN_UPDATE");
            order.setStatus("CONFIRMED");
            orderRepository.save(order);

            stateTransitionWorker.apply("ORDER", order.getStatus(), "PACKED", "ADMIN_UPDATE");
            order.setStatus("PACKED");
            orderRepository.save(order);
        } else if ("CONFIRMED".equals(order.getStatus())) {
            stateTransitionWorker.apply("ORDER", order.getStatus(), "PACKED", "ADMIN_UPDATE");
            order.setStatus("PACKED");
            orderRepository.save(order);
        }

        stateTransitionWorker.apply("ORDER", order.getStatus(), "DELIVERED", "ADMIN_UPDATE");
        order.setStatus("DELIVERED");
        orderRepository.save(order);
        eventPublisher.publishEvent(new OrderDeliveredEvent(order.getId(), order.getUser().getId()));
    }

    @Transactional
    public BatchResponse updateBatchStatus(Long batchId, String targetStatus, String action) {
        DeliveryBatch batch = find(batchId);
        StateTransition transition = stateTransitionWorker.apply(
                "DELIVERY_BATCH", batch.getStatus(), targetStatus, action);
        batch.setStatus(targetStatus.toUpperCase());
        batchRepository.save(batch);

        // When first batch starts → move cycle to DELIVERING
        if ("IN_PROGRESS".equals(targetStatus)) {
            WeeklyCycle cycle = batch.getCycle();
            if ("CLOSED".equals(cycle.getStatus()) || "PROCUREMENT".equals(cycle.getStatus())) {
                stateTransitionWorker.apply("CYCLE", cycle.getStatus(), "DELIVERING", "ADMIN_UPDATE");
                cycle.setStatus("DELIVERING");
                cycleRepository.save(cycle);
            }
        }

        // If batch is DONE, publish event — listener will check if all batches done
        if ("BATCH_COMPLETED_EVENT".equals(transition.getSideEffect())) {
            // Mark all orders in this batch as DELIVERED
            batchOrderRepository.findByBatchId(batchId).forEach(bo -> {
                CustomerOrder order = bo.getOrder();
                stateTransitionWorker.apply("ORDER", order.getStatus(), "DELIVERED", "ADMIN_UPDATE");
                order.setStatus("DELIVERED");
                orderRepository.save(order);
                eventPublisher.publishEvent(new OrderDeliveredEvent(order.getId(), order.getUser().getId()));
            });
            eventPublisher.publishEvent(new BatchCompletedEvent(batchId, batch.getCycle().getId()));
        }

        return toResponse(batch);
    }

    // Async listener — when all batches for a cycle are DONE, auto-complete the cycle
    @EventListener
    @Async("appTaskExecutor")
    @Transactional
    public void onBatchCompleted(BatchCompletedEvent event) {
        long pendingBatches = batchRepository.countByCycleIdAndStatusNot(event.cycleId(), "DONE");
        if (pendingBatches == 0) {
            WeeklyCycle cycle = cycleRepository.findById(event.cycleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Cycle not found: " + event.cycleId()));
            stateTransitionWorker.apply("CYCLE", cycle.getStatus(), "COMPLETED", "ADMIN_UPDATE");
            cycle.setStatus("COMPLETED");
            cycleRepository.save(cycle);
            log.info("All batches done — cycle {} auto-completed", cycle.getCycleLabel());
        }
    }

    private DeliveryBatch find(Long id) {
        return batchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery batch not found: " + id));
    }

    private BatchResponse toResponse(DeliveryBatch b) {
        List<BatchOrderSummary> orders = batchOrderRepository.findByBatchId(b.getId()).stream()
                .map(bo -> {
                    CustomerOrder o = bo.getOrder();
                    UserProfile profile = profileRepository.findByUserId(o.getUser().getId()).orElse(null);
                    String name = profile != null
                            ? profile.getFirstName() + " " + profile.getLastName()
                            : o.getUser().getUsername();
                    return new BatchOrderSummary(
                            o.getId(), o.getOrderNumber(), name,
                            profile != null ? profile.getFlatNumber() : null,
                            profile != null ? profile.getBlock() : null,
                            o.getDeliverySlot(), o.getStatus());
                }).toList();

        return new BatchResponse(
                b.getId(), b.getBatchLabel(),
                b.getCycle().getId(), b.getCycle().getCycleLabel(),
                b.getDeliveryDate(),
                b.getAssignedTo() != null ? b.getAssignedTo().getId() : null,
                b.getAssignedTo() != null ? b.getAssignedTo().getUsername() : null,
                b.getStatus(), orders);
    }
}
