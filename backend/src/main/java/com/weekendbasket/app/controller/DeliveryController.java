package com.weekendbasket.app.controller;

import com.weekendbasket.app.dto.DeliveryDto.*;
import com.weekendbasket.app.repository.UserRepository;
import com.weekendbasket.app.service.DeliveryBatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/delivery")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryBatchService batchService;
    private final UserRepository userRepository;

    @GetMapping("/batches/cycle/{cycleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<BatchResponse>> getBatchesByCycle(@PathVariable Long cycleId) {
        return ResponseEntity.ok(batchService.getBatchesByCycle(cycleId));
    }

    @GetMapping("/batches/my")
    @PreAuthorize("hasAnyRole('ADMIN', 'DELIVERY')")
    public ResponseEntity<List<BatchResponse>> getMyBatches(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userRepository.findByPhoneNumber(userDetails.getUsername())
                .orElseThrow().getId();
        return ResponseEntity.ok(batchService.getMyBatches(userId));
    }

    @GetMapping("/batches/{batchId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DELIVERY')")
    public ResponseEntity<BatchResponse> getById(@PathVariable Long batchId) {
        return ResponseEntity.ok(batchService.getById(batchId));
    }

    @PostMapping("/batches")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BatchResponse> createBatch(@Valid @RequestBody CreateBatchRequest request) {
        return ResponseEntity.ok(batchService.createBatch(request));
    }

    @PutMapping("/batches/{batchId}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BatchResponse> assignStaff(
            @PathVariable Long batchId,
            @RequestParam Long staffUserId) {
        return ResponseEntity.ok(batchService.assignStaff(batchId, staffUserId));
    }

    @PutMapping("/batches/{batchId}/orders/{orderId}/delivered")
    @PreAuthorize("hasAnyRole('ADMIN', 'DELIVERY')")
    public ResponseEntity<Void> markOrderDelivered(
            @PathVariable Long batchId,
            @PathVariable Long orderId) {
        batchService.markOrderDelivered(batchId, orderId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/batches/{batchId}/start")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BatchResponse> startBatch(@PathVariable Long batchId) {
        return ResponseEntity.ok(batchService.updateBatchStatus(batchId, "IN_PROGRESS", "ADMIN_UPDATE"));
    }

    @PutMapping("/batches/{batchId}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'DELIVERY')")
    public ResponseEntity<BatchResponse> completeBatch(@PathVariable Long batchId) {
        return ResponseEntity.ok(batchService.updateBatchStatus(batchId, "DONE", "DELIVERY_COMPLETE"));
    }
}
