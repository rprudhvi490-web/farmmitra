package com.weekendbasket.app.controller;

import com.weekendbasket.app.dto.MasterTableDto.*;
import com.weekendbasket.app.service.MasterTableService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/masters")
@RequiredArgsConstructor
public class MasterTableController {

    private final MasterTableService masterTableService;

    @GetMapping
    public ResponseEntity<List<MasterResponse>> getAll() {
        return ResponseEntity.ok(masterTableService.getAll());
    }

    @GetMapping("/{type}")
    public ResponseEntity<List<MasterResponse>> getByType(@PathVariable String type) {
        return ResponseEntity.ok(masterTableService.getByType(type));
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<MasterResponse> create(@Valid @RequestBody CreateMasterRequest request) {
        return ResponseEntity.ok(masterTableService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<MasterResponse> update(@PathVariable Long id, @Valid @RequestBody CreateMasterRequest request) {
        return ResponseEntity.ok(masterTableService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        masterTableService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
