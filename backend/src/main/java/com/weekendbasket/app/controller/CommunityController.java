package com.weekendbasket.app.controller;

import com.weekendbasket.app.dto.CommunityDto.*;
import com.weekendbasket.app.service.CommunityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/communities")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CommunityResponse>> getAll() {
        return ResponseEntity.ok(communityService.getAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommunityResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(communityService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<CommunityResponse> create(@Valid @RequestBody CreateCommunityRequest request) {
        return ResponseEntity.ok(communityService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<CommunityResponse> update(@PathVariable Long id, @Valid @RequestBody CreateCommunityRequest request) {
        return ResponseEntity.ok(communityService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        communityService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
