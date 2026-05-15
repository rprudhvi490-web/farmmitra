package com.weekendbasket.app.controller;

import com.weekendbasket.app.dto.DashboardDto.*;
import com.weekendbasket.app.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public ResponseEntity<WeeklyStatsResponse> getWeeklyStats() {
        return ResponseEntity.ok(dashboardService.getWeeklyStats());
    }

    @GetMapping("/cycle/{cycleId}")
    public ResponseEntity<CycleSummaryResponse> getCycleSummary(@PathVariable Long cycleId) {
        return ResponseEntity.ok(dashboardService.getCycleSummary(cycleId));
    }

    // All cycles with revenue breakdown (generated vs collected vs pending)
    @GetMapping("/history")
    public ResponseEntity<CycleHistoryResponse> getCycleHistory() {
        return ResponseEntity.ok(dashboardService.getCycleHistory());
    }

    // Customer analytics — loyalty tags, repeat orders, spend
    @GetMapping("/customers")
    public ResponseEntity<CustomerAnalyticsResponse> getCustomerAnalytics() {
        return ResponseEntity.ok(dashboardService.getCustomerAnalytics());
    }
}
