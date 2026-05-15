package com.weekendbasket.app.service;

import com.weekendbasket.app.dto.TransportTrackingDto.*;
import com.weekendbasket.app.event.TransportStageUpdatedEvent;
import com.weekendbasket.app.exception.ResourceNotFoundException;
import com.weekendbasket.app.model.TransportTracking;
import com.weekendbasket.app.model.WeeklyCycle;
import com.weekendbasket.app.repository.TransportTrackingRepository;
import com.weekendbasket.app.repository.WeeklyCycleRepository;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransportTrackingService {

    private static final Logger log = LogManager.getLogger(TransportTrackingService.class);

    private final TransportTrackingRepository trackingRepository;
    private final WeeklyCycleRepository cycleRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<TransportStageResponse> getStagesForCycle(Long cycleId) {
        return trackingRepository.findByCycleIdOrderByCreatedOnAsc(cycleId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public TransportStageResponse getLatestStage(Long cycleId) {
        return trackingRepository.findTopByCycleIdOrderByCreatedOnDesc(cycleId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("No transport stages found for cycle: " + cycleId));
    }

    @Transactional
    public TransportStageResponse addStage(Long cycleId, AddStageRequest request, String updatedBy) {
        WeeklyCycle cycle = cycleRepository.findById(cycleId)
                .orElseThrow(() -> new ResourceNotFoundException("Cycle not found: " + cycleId));

        TransportTracking tracking = TransportTracking.builder()
                .cycle(cycle)
                .stage(request.stage().toUpperCase())
                .notes(request.notes())
                .updatedBy(updatedBy)
                .createdOn(LocalDateTime.now())
                .build();
        trackingRepository.save(tracking);
        eventPublisher.publishEvent(new TransportStageUpdatedEvent(
                cycleId, tracking.getStage(), tracking.getNotes()));
        return toResponse(tracking);
    }

    // Auto-triggered by system actions — no manual input needed
    @Transactional
    public void autoAddStage(Long cycleId, String stage, String notes) {
        // Skip if this stage already exists for this cycle
        boolean exists = trackingRepository.findByCycleIdOrderByCreatedOnAsc(cycleId)
                .stream().anyMatch(t -> t.getStage().equalsIgnoreCase(stage));
        if (exists) {
            log.info("Transport stage {} already recorded for cycle {}, skipping", stage, cycleId);
            return;
        }
        WeeklyCycle cycle = cycleRepository.findById(cycleId)
                .orElseThrow(() -> new ResourceNotFoundException("Cycle not found: " + cycleId));
        TransportTracking tracking = TransportTracking.builder()
                .cycle(cycle)
                .stage(stage.toUpperCase())
                .notes(notes)
                .updatedBy("SYSTEM")
                .createdOn(LocalDateTime.now())
                .build();
        trackingRepository.save(tracking);
        eventPublisher.publishEvent(new TransportStageUpdatedEvent(cycleId, stage, notes));
        log.info("Auto transport stage {} added for cycle {}", stage, cycleId);
    }

    private TransportStageResponse toResponse(TransportTracking t) {
        return new TransportStageResponse(
                t.getId(), t.getCycle().getId(),
                t.getStage(), t.getNotes(),
                t.getUpdatedBy(), t.getCreatedOn());
    }
}
