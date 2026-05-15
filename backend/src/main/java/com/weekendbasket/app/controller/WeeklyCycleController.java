package com.weekendbasket.app.controller;

import com.weekendbasket.app.dto.WeeklyCycleDto.*;
import com.weekendbasket.app.service.WeeklyCycleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cycles")
@RequiredArgsConstructor
public class WeeklyCycleController {

    private final WeeklyCycleService cycleService;

    @GetMapping("/current")
    public ResponseEntity<CycleResponse> getCurrent() {
        return ResponseEntity.ok(cycleService.getCurrent());
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CycleResponse>> getAll() {
        return ResponseEntity.ok(cycleService.getAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CycleResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(cycleService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CycleResponse> createCycle(@RequestBody CreateCycleRequest req) {
        return ResponseEntity.ok(cycleService.createCycle(req));
    }

    @PostMapping("/open")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CycleResponse> openNewCycle() {
        return ResponseEntity.ok(cycleService.openNewCycle());
    }

    @PutMapping("/{id}/open")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CycleResponse> reOpenCycle(@PathVariable Long id) {
        return ResponseEntity.ok(cycleService.updateStatus(id, "OPEN"));
    }

    @PutMapping("/{id}/close")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CycleResponse> closeCycle(@PathVariable Long id) {
        return ResponseEntity.ok(cycleService.closeCycle(id, "ADMIN_CLOSE"));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CycleResponse> updateStatus(@PathVariable Long id, @RequestBody java.util.Map<String, String> body) {
        return ResponseEntity.ok(cycleService.updateStatus(id, body.get("status")));
    }
}
