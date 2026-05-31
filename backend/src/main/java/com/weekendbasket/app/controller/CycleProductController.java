package com.weekendbasket.app.controller;

import com.weekendbasket.app.dto.CycleProductDto.*;
import com.weekendbasket.app.service.CycleProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cycle-products")
@RequiredArgsConstructor
public class CycleProductController {

    private final CycleProductService cycleProductService;

    // Get stock limits for a cycle
    @GetMapping("/{cycleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CycleProductResponse>> getForCycle(@PathVariable Long cycleId) {
        return ResponseEntity.ok(cycleProductService.getForCycle(cycleId));
    }

    // Auto-suggest based on last closed cycle
    @GetMapping("/suggestions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<StockSuggestion>> getSuggestions() {
        return ResponseEntity.ok(cycleProductService.getSuggestions());
    }

    // Bulk set stock limits for a cycle
    @PutMapping("/{cycleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CycleProductResponse>> bulkSetStock(
            @PathVariable Long cycleId,
            @Valid @RequestBody BulkSetStockRequest request) {
        return ResponseEntity.ok(cycleProductService.bulkSetStock(cycleId, request));
    }
}
