package com.weekendbasket.app.service;

import com.weekendbasket.app.dto.DashboardDto.*;
import com.weekendbasket.app.model.CustomerOrder;
import com.weekendbasket.app.model.User;
import com.weekendbasket.app.model.UserProfile;
import com.weekendbasket.app.model.WeeklyCycle;
import com.weekendbasket.app.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository userRepository;
    private final CustomerOrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final WeeklyCycleRepository cycleRepository;
    private final ProcurementSheetRepository procurementRepository;
    private final UserProfileRepository userProfileRepository;

    @Transactional(readOnly = true)
    public WeeklyStatsResponse getWeeklyStats() {
        long totalUsers    = userRepository.count();
        long totalOrders   = orderRepository.count();
        long totalDelivered = orderRepository.countByStatus("DELIVERED");
        BigDecimal totalRevenue = orderRepository.findAll().stream()
                .filter(o -> !"CANCELLED".equals(o.getStatus()))
                .map(CustomerOrder::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long totalProducts  = productRepository.count();
        long activeProducts = productRepository.countByAvailableTrue();
        long totalCategories = categoryRepository.count();

        CycleSummaryResponse currentCycle = cycleRepository
                .findTopByStatusOrderByOrderOpenAtDesc("OPEN")
                .map(this::buildCycleSummary)
                .orElse(null);

        return new WeeklyStatsResponse(totalUsers, totalOrders, totalDelivered,
                totalRevenue, totalProducts, activeProducts, totalCategories, currentCycle);
    }

    @Transactional(readOnly = true)
    public CycleSummaryResponse getCycleSummary(Long cycleId) {
        WeeklyCycle cycle = cycleRepository.findById(cycleId)
                .orElseThrow(() -> new com.weekendbasket.app.exception.ResourceNotFoundException(
                        "Cycle not found: " + cycleId));
        return buildCycleSummary(cycle);
    }

    // ── Cycle History ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public CycleHistoryResponse getCycleHistory() {
        List<WeeklyCycle> allCycles = cycleRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(WeeklyCycle::getOrderOpenAt).reversed())
                .toList();

        List<CycleHistoryEntry> entries = allCycles.stream()
                .map(this::buildCycleHistoryEntry)
                .toList();

        BigDecimal allTimeRevenue   = entries.stream().map(CycleHistoryEntry::totalRevenue).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal allTimeCollected = entries.stream().map(CycleHistoryEntry::collectedRevenue).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal allTimePending   = allTimeRevenue.subtract(allTimeCollected);
        long allTimeOrders          = entries.stream().mapToLong(CycleHistoryEntry::totalOrders).sum();

        // Unique customers across all cycles
        long allTimeUniqueCustomers = orderRepository.findDistinctCustomerIds().size();

        return new CycleHistoryResponse(
                entries,
                allCycles.size(),
                allTimeRevenue,
                allTimeCollected,
                allTimePending,
                allTimeOrders,
                allTimeUniqueCustomers
        );
    }

    // ── Customer Analytics ────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public CustomerAnalyticsResponse getCustomerAnalytics() {
        List<Long> customerIds = orderRepository.findDistinctCustomerIds();

        List<CustomerOrderSummary> summaries = customerIds.stream()
                .map(this::buildCustomerSummary)
                .sorted(Comparator.comparingLong(CustomerOrderSummary::cyclesParticipated).reversed())
                .toList();

        long newCustomers      = summaries.stream().filter(c -> "NEW".equals(c.loyaltyTag())).count();
        long regularCustomers  = summaries.stream().filter(c -> "REGULAR".equals(c.loyaltyTag())).count();
        long loyalCustomers    = summaries.stream().filter(c -> "LOYAL".equals(c.loyaltyTag())).count();
        long championCustomers = summaries.stream().filter(c -> "CHAMPION".equals(c.loyaltyTag())).count();

        return new CustomerAnalyticsResponse(
                summaries,
                summaries.size(),
                newCustomers,
                regularCustomers,
                loyalCustomers,
                championCustomers
        );
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private CycleSummaryResponse buildCycleSummary(WeeklyCycle cycle) {
        List<CustomerOrder> orders = orderRepository.findByCycleId(cycle.getId());
        long total     = orders.size();
        long cancelled = orders.stream().filter(o -> "CANCELLED".equals(o.getStatus())).count();
        long delivered = orders.stream().filter(o -> "DELIVERED".equals(o.getStatus())).count();
        BigDecimal revenue = orders.stream()
                .filter(o -> !"CANCELLED".equals(o.getStatus()))
                .map(CustomerOrder::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long procItems   = procurementRepository.countByCycleId(cycle.getId());
        long pendingProc = procurementRepository.countByCycleIdAndStatus(cycle.getId(), "PENDING");

        return new CycleSummaryResponse(cycle.getId(), cycle.getCycleLabel(), cycle.getStatus(),
                total, cancelled, delivered, revenue, procItems, pendingProc);
    }

    private CycleHistoryEntry buildCycleHistoryEntry(WeeklyCycle cycle) {
        List<CustomerOrder> orders = orderRepository.findByCycleId(cycle.getId());
        List<CustomerOrder> active = orders.stream().filter(o -> !"CANCELLED".equals(o.getStatus())).toList();

        long totalOrders     = active.size();
        long cancelledOrders = orders.size() - active.size();
        long deliveredOrders = active.stream().filter(o -> "DELIVERED".equals(o.getStatus())).count();
        long uniqueCustomers = orderRepository.countUniqueCustomersByCycleId(cycle.getId());

        BigDecimal totalRevenue = active.stream()
                .map(CustomerOrder::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal collectedRevenue = active.stream()
                .filter(o -> "PAID".equals(o.getPaymentStatus()))
                .map(CustomerOrder::getAmountToCollect)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal pendingRevenue = totalRevenue.subtract(collectedRevenue);

        return new CycleHistoryEntry(
                cycle.getId(), cycle.getCycleLabel(), cycle.getStatus(),
                cycle.getOrderOpenAt(), cycle.getOrderCloseAt(),
                cycle.getDeliveryDateSat(), cycle.getDeliveryDateSun(),
                totalOrders, cancelledOrders, deliveredOrders, uniqueCustomers,
                totalRevenue, collectedRevenue, pendingRevenue
        );
    }

    private CustomerOrderSummary buildCustomerSummary(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return null;

        UserProfile profile = userProfileRepository.findByUserId(userId).orElse(null);
        List<CustomerOrder> orders = orderRepository.findActiveOrdersByUserId(userId);

        long totalOrders        = orders.size();
        long cyclesParticipated = orderRepository.countDistinctCyclesByUserId(userId);

        BigDecimal totalSpent = orders.stream()
                .map(CustomerOrder::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPaid = orders.stream()
                .filter(o -> "PAID".equals(o.getPaymentStatus()))
                .map(CustomerOrder::getAmountToCollect)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String loyaltyTag = cyclesParticipated >= 7 ? "CHAMPION"
                : cyclesParticipated >= 4 ? "LOYAL"
                : cyclesParticipated >= 2 ? "REGULAR"
                : "NEW";

        return new CustomerOrderSummary(
                userId,
                user.getPhoneNumber(),
                user.getUsername(),
                profile != null ? profile.getFlatNumber() : null,
                profile != null ? profile.getBlock() : null,
                totalOrders,
                cyclesParticipated,
                totalSpent,
                totalPaid,
                loyaltyTag
        );
    }
}
