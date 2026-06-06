package com.weekendbasket.app.service;

import com.weekendbasket.app.dto.OrderDto.*;
import com.weekendbasket.app.event.OrderPlacedEvent;
import com.weekendbasket.app.exception.ResourceNotFoundException;
import com.weekendbasket.app.exception.WeekendBasketException;
import com.weekendbasket.app.model.*;
import com.weekendbasket.app.order.DiscountStrategy;
import com.weekendbasket.app.order.OrderPlacementContext;
import com.weekendbasket.app.order.OrderPlacementRule;
import com.weekendbasket.app.repository.*;
import com.weekendbasket.app.statemachine.StateTransitionWorker;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final Logger log = LogManager.getLogger(OrderService.class);

    private final CustomerOrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final WeeklyCycleRepository cycleRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final StateTransitionWorker stateTransitionWorker;
    private final DiscountStrategy discountStrategy;
    private final List<OrderPlacementRule> placementRules;
    private final ApplicationEventPublisher eventPublisher;
    private final CycleProductService cycleProductService;

    @Transactional
    public OrderResponse placeOrder(String phoneNumber, PlaceOrderRequest request) {
        // Run chain — each rule validates and enriches context
        OrderPlacementContext context = new OrderPlacementContext(phoneNumber, request);
        placementRules.forEach(rule -> rule.validate(context));

        // Build items using products already resolved by chain (no re-fetch)
        List<OrderItem> items = request.items().stream().map(r -> {
            Product product = context.getResolvedProducts().get(r.productId());
            BigDecimal total = product.getPricePerUnit().multiply(r.quantity());
            return OrderItem.builder()
                    .product(product)
                    .quantity(r.quantity())
                    .unitPrice(product.getPricePerUnit())
                    .totalPrice(total)
                    .build();
        }).toList();

        BigDecimal total = items.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discount = discountStrategy.calculate(context.getUser(), total);

        CustomerOrder order = CustomerOrder.builder()
                .orderNumber(generateOrderNumber())
                .user(context.getUser())
                .cycle(context.getCycle())
                .totalAmount(total)
                .referralDiscount(discount)
                .amountToCollect(total.subtract(discount))
                .notes(request.notes())
                .build();
        orderRepository.save(order);
        items.forEach(i -> i.setOrder(order));
        orderItemRepository.saveAll(items);
        eventPublisher.publishEvent(new OrderPlacedEvent(
                order.getId(), order.getUser().getId(),
                order.getOrderNumber(), order.getTotalAmount().toPlainString()));
        return toResponse(order, items);
    }

    @Transactional(readOnly = true)
    public OrderResponse getMyOrder(String phoneNumber, Long orderId) {
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        CustomerOrder order = findOrder(orderId);
        if (!order.getUser().getId().equals(user.getId())) {
            throw new WeekendBasketException("Access denied.");
        }
        return toResponse(order, orderItemRepository.findByOrderId(orderId));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(String phoneNumber) {
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return orderRepository.findByUserId(user.getId()).stream()
                .map(o -> toResponse(o, orderItemRepository.findByOrderId(o.getId())))
                .toList();
    }

    @Transactional
    public OrderResponse cancelOrder(String phoneNumber, Long orderId) {
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        CustomerOrder order = findOrder(orderId);
        if (!order.getUser().getId().equals(user.getId())) {
            throw new WeekendBasketException("Access denied.");
        }
        if (!"OPEN".equalsIgnoreCase(order.getCycle().getStatus())) {
            throw new WeekendBasketException(
                    "Procurement has started. To cancel your order, please call customer support.");
        }
        stateTransitionWorker.apply("ORDER", order.getStatus(), "CANCELLED", "CUSTOMER_CANCEL");
        order.setStatus("CANCELLED");
        orderRepository.save(order);
        // Release reserved stock back to cycle
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        items.forEach(i -> cycleProductService.releaseStock(
                order.getCycle().getId(), i.getProduct().getId(), i.getQuantity()));
        return toResponse(order, items);
    }

    @Transactional
    public OrderResponse adminCancelOrder(Long orderId) {
        CustomerOrder order = findOrder(orderId);
        stateTransitionWorker.apply("ORDER", order.getStatus(), "CANCELLED", "ADMIN_CANCEL");
        order.setStatus("CANCELLED");
        orderRepository.save(order);
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        items.forEach(i -> cycleProductService.releaseStock(
                order.getCycle().getId(), i.getProduct().getId(), i.getQuantity()));
        return toResponse(order, items);
    }

    // Admin — paginated
    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrdersByCycle(Long cycleId, Pageable pageable) {
        return orderRepository.findByCycleId(cycleId, pageable)
                .map(o -> toResponse(o, orderItemRepository.findByOrderId(o.getId())));
    }

    // Admin — non-paginated (used internally by procurement/delivery)
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByCycle(Long cycleId) {
        return orderRepository.findByCycleId(cycleId).stream()
                .map(o -> toResponse(o, orderItemRepository.findByOrderId(o.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId) {
        CustomerOrder order = findOrder(orderId);
        return toResponse(order, orderItemRepository.findByOrderId(orderId));
    }

    @Transactional
    public OrderResponse updateStatus(Long orderId, String status) {
        CustomerOrder order = findOrder(orderId);
        stateTransitionWorker.apply("ORDER", order.getStatus(), status, "ADMIN_UPDATE");
        order.setStatus(status.toUpperCase());
        orderRepository.save(order);
        return toResponse(order, orderItemRepository.findByOrderId(orderId));
    }

    @Transactional
    public void markPaid(Long orderId) {
        CustomerOrder order = findOrder(orderId);
        order.setPaymentStatus("PAID");
        orderRepository.save(order);
        log.info("Order {} marked as PAID", order.getOrderNumber());
    }

    @Transactional
    public OrderResponse assignDeliverySlot(Long orderId, String slot) {
        CustomerOrder order = findOrder(orderId);
        order.setDeliverySlot(slot.toUpperCase());
        orderRepository.save(order);
        return toResponse(order, orderItemRepository.findByOrderId(orderId));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private String generateOrderNumber() {
        String year = String.valueOf(LocalDateTime.now().getYear());
        long count = orderRepository.count() + 1;
        return String.format("WB-%s-%04d", year, count);
    }

    private CustomerOrder findOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
    }

    private OrderResponse toResponse(CustomerOrder o, List<OrderItem> items) {
        List<OrderItemResponse> itemResponses = items.stream()
                .map(i -> new OrderItemResponse(
                        i.getProduct().getId(), i.getProduct().getName(),
                        i.getProduct().getUnit(), i.getQuantity(),
                        i.getUnitPrice(), i.getTotalPrice()))
                .toList();
        return new OrderResponse(
                o.getId(), o.getOrderNumber(),
                o.getCycle().getId(), o.getCycle().getCycleLabel(),
                o.getCycle().getStatus(),
                o.getStatus(), o.getDeliverySlot(),
                o.getTotalAmount(), o.getReferralDiscount(), o.getAmountToCollect(),
                o.getPaymentMethod(), o.getPaymentStatus(), o.getNotes(), itemResponses);
    }
}
