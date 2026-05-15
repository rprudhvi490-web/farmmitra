package com.weekendbasket.app.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class DashboardDto {

    public record CycleSummaryResponse(
            Long cycleId,
            String cycleLabel,
            String status,
            long totalOrders,
            long cancelledOrders,
            long deliveredOrders,
            BigDecimal totalRevenue,
            long totalProcurementItems,
            long pendingProcurementItems
    ) {}

    public record WeeklyStatsResponse(
            long totalUsers,
            long totalOrders,
            long totalDeliveredOrders,
            BigDecimal totalRevenue,
            long totalProducts,
            long activeProducts,
            long totalCategories,
            CycleSummaryResponse currentCycle
    ) {}

    // ── Cycle History (all cycles with financials) ────────────────────────────
    public record CycleHistoryEntry(
            Long cycleId,
            String cycleLabel,
            String status,
            LocalDateTime orderOpenAt,
            LocalDateTime orderCloseAt,
            LocalDate deliveryDateSat,
            LocalDate deliveryDateSun,
            long totalOrders,
            long cancelledOrders,
            long deliveredOrders,
            long uniqueCustomers,
            BigDecimal totalRevenue,       // sum of all non-cancelled orders
            BigDecimal collectedRevenue,   // sum of PAID orders only
            BigDecimal pendingRevenue      // totalRevenue - collectedRevenue
    ) {}

    public record CycleHistoryResponse(
            List<CycleHistoryEntry> cycles,
            // Aggregates across all cycles
            long totalCycles,
            BigDecimal allTimeRevenue,
            BigDecimal allTimeCollected,
            BigDecimal allTimePending,
            long allTimeOrders,
            long allTimeUniqueCustomers
    ) {}

    // ── Customer Analytics ────────────────────────────────────────────────────
    public record CustomerOrderSummary(
            Long userId,
            String phoneNumber,
            String username,
            String flatNumber,
            String block,
            long totalOrders,          // across all cycles
            long cyclesParticipated,   // how many cycles they ordered in
            BigDecimal totalSpent,
            BigDecimal totalPaid,
            String loyaltyTag          // NEW / REGULAR / LOYAL / CHAMPION
    ) {}

    public record CustomerAnalyticsResponse(
            List<CustomerOrderSummary> customers,
            long totalCustomers,
            long newCustomers,         // ordered in only 1 cycle
            long regularCustomers,     // 2-3 cycles
            long loyalCustomers,       // 4-6 cycles
            long championCustomers     // 7+ cycles
    ) {}
}
