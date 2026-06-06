package com.weekendbasket.app.controller;

import com.weekendbasket.app.dto.ProcurementDto.*;
import com.weekendbasket.app.service.ProcurementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/procurement")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT')")
public class ProcurementController {

    private final ProcurementService procurementService;

    @GetMapping("/{cycleId}")
    public ResponseEntity<ProcurementSheetResponse> getForCycle(@PathVariable Long cycleId) {
        return ResponseEntity.ok(procurementService.getForCycle(cycleId));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<ProcurementItemResponse> updateItem(
            @PathVariable Long itemId,
            @RequestBody UpdateProcurementRequest request) {
        return ResponseEntity.ok(procurementService.updateItem(itemId, request));
    }

    @GetMapping("/{cycleId}/export")
    public ResponseEntity<byte[]> exportExcel(@PathVariable Long cycleId) {
        byte[] data = procurementService.exportExcel(cycleId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=procurement-" + cycleId + ".xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    // Mark ALL items as PROCURED at once — triggers GOODS_LOADED automatically
    @PutMapping("/{cycleId}/mark-all-procured")
    public ResponseEntity<Void> markAllProcured(@PathVariable Long cycleId) {
        procurementService.markAllProcured(cycleId);
        return ResponseEntity.noContent().build();
    }
}
