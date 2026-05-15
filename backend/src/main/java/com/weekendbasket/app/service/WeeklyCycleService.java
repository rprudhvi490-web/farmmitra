package com.weekendbasket.app.service;

import com.weekendbasket.app.dto.WeeklyCycleDto.*;
import com.weekendbasket.app.event.CycleClosedEvent;
import com.weekendbasket.app.event.CycleOpenedEvent;
import com.weekendbasket.app.exception.ResourceNotFoundException;
import com.weekendbasket.app.exception.WeekendBasketException;
import com.weekendbasket.app.model.WeeklyCycle;
import com.weekendbasket.app.repository.WeeklyCycleRepository;
import com.weekendbasket.app.statemachine.StateTransition;
import com.weekendbasket.app.statemachine.StateTransitionWorker;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WeeklyCycleService {

    private static final Logger log = LogManager.getLogger(WeeklyCycleService.class);
    private static final DateTimeFormatter LABEL_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final WeeklyCycleRepository cycleRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final StateTransitionWorker stateTransitionWorker;

    public CycleResponse getCurrent() {
        return cycleRepository.findTopByStatusOrderByOrderOpenAtDesc("OPEN")
                .map(this::toResponse)
                .orElse(null);
    }

    public List<CycleResponse> getAll() {
        return cycleRepository.findAll().stream().map(this::toResponse).toList();
    }

    public CycleResponse getById(Long id) {
        return toResponse(find(id));
    }

    @Transactional
    public CycleResponse createCycle(CreateCycleRequest req) {
        if (cycleRepository.existsByStatus("OPEN")) {
            throw new WeekendBasketException("A cycle is already OPEN. Close it before creating a new one.");
        }
        WeeklyCycle cycle = WeeklyCycle.builder()
                .cycleLabel(req.cycleLabel())
                .orderOpenAt(req.orderOpenAt())
                .orderCloseAt(req.orderCloseAt())
                .deliveryDateSat(req.deliveryDateSat())
                .deliveryDateSun(req.deliveryDateSun())
                .status("OPEN")
                .build();
        cycleRepository.save(cycle);
        eventPublisher.publishEvent(new CycleOpenedEvent(
                cycle.getId(), cycle.getCycleLabel(),
                cycle.getOrderCloseAt().toString()));
        log.info("Weekly cycle created manually: {}", cycle.getCycleLabel());
        return toResponse(cycle);
    }

    @Transactional
    public CycleResponse openNewCycle() {
        if (cycleRepository.existsByStatus("OPEN")) {
            throw new WeekendBasketException("A cycle is already OPEN. Close it before opening a new one.");
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        // next Wednesday 14:00
        LocalDate wednesday = today.plusDays((3 - today.getDayOfWeek().getValue() + 7) % 7);
        LocalDate saturday  = today.plusDays((6 - today.getDayOfWeek().getValue() + 7) % 7);
        LocalDate sunday    = saturday.plusDays(1);

        WeeklyCycle cycle = WeeklyCycle.builder()
                .cycleLabel("Week of " + today.format(LABEL_FMT))
                .orderOpenAt(now)
                .orderCloseAt(wednesday.atTime(14, 0))
                .deliveryDateSat(saturday)
                .deliveryDateSun(sunday)
                .status("OPEN")
                .build();
        cycleRepository.save(cycle);
        eventPublisher.publishEvent(new CycleOpenedEvent(
                cycle.getId(), cycle.getCycleLabel(),
                cycle.getOrderCloseAt().toString()));
        log.info("Weekly cycle opened: {}", cycle.getCycleLabel());
        return toResponse(cycle);
    }

    @Transactional
    public CycleResponse closeCycle(Long id, String action) {
        WeeklyCycle cycle = find(id);
        StateTransition transition = stateTransitionWorker.apply("CYCLE", cycle.getStatus(), "CLOSED", action);
        cycle.setStatus("CLOSED");
        cycleRepository.save(cycle);
        if ("CYCLE_CLOSED_EVENT".equals(transition.getSideEffect())) {
            eventPublisher.publishEvent(new CycleClosedEvent(cycle.getId()));
        }
        log.info("Weekly cycle closed via {}: {}", action, cycle.getCycleLabel());
        return toResponse(cycle);
    }

    @Transactional
    public CycleResponse updateStatus(Long id, String targetStatus) {
        WeeklyCycle cycle = find(id);
        stateTransitionWorker.apply("CYCLE", cycle.getStatus(), targetStatus, "ADMIN_UPDATE");
        cycle.setStatus(targetStatus.toUpperCase());
        cycleRepository.save(cycle);
        return toResponse(cycle);
    }

    // Called by scheduler
    @Transactional
    public void scheduledOpen() {
        if (!cycleRepository.existsByStatus("OPEN")) {
            openNewCycle();
        }
    }

    // Called by scheduler
    @Transactional
    public void scheduledClose() {
        cycleRepository.findTopByStatusOrderByOrderOpenAtDesc("OPEN")
                .ifPresent(c -> closeCycle(c.getId(), "SCHEDULER_CLOSE"));
    }

    private WeeklyCycle find(Long id) {
        return cycleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cycle not found with id: " + id));
    }

    private CycleResponse toResponse(WeeklyCycle c) {
        Long remaining = null;
        if ("OPEN".equals(c.getStatus()) && c.getOrderCloseAt() != null) {
            remaining = c.getOrderCloseAt().toEpochSecond(ZoneOffset.UTC)
                    - LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
            if (remaining < 0) remaining = 0L;
        }
        return new CycleResponse(c.getId(), c.getCycleLabel(), c.getStatus(),
                c.getOrderOpenAt(), c.getOrderCloseAt(),
                c.getDeliveryDateSat(), c.getDeliveryDateSun(), remaining);
    }
}
