package com.weekendbasket.app.controller;

import com.weekendbasket.app.dto.TransportTrackingDto.*;
import com.weekendbasket.app.service.TransportTrackingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transport")
@RequiredArgsConstructor
public class TransportTrackingController {

    private final TransportTrackingService trackingService;

    @GetMapping("/{cycleId}")
    public ResponseEntity<List<TransportStageResponse>> getStages(@PathVariable Long cycleId) {
        return ResponseEntity.ok(trackingService.getStagesForCycle(cycleId));
    }

    @GetMapping("/{cycleId}/latest")
    public ResponseEntity<TransportStageResponse> getLatest(@PathVariable Long cycleId) {
        return ResponseEntity.ok(trackingService.getLatestStage(cycleId));
    }

    @PostMapping("/{cycleId}/stage")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TransportStageResponse> addStage(
            @PathVariable Long cycleId,
            @Valid @RequestBody AddStageRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(trackingService.addStage(cycleId, request, userDetails.getUsername()));
    }
}
