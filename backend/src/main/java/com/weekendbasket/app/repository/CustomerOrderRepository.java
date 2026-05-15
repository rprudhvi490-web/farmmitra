package com.weekendbasket.app.repository;

import com.weekendbasket.app.model.CustomerOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {
    List<CustomerOrder> findByUserId(Long userId);
    List<CustomerOrder> findByCycleId(Long cycleId);
    Page<CustomerOrder> findByCycleId(Long cycleId, Pageable pageable);
    List<CustomerOrder> findByCycleIdAndStatusNot(Long cycleId, String status);
    Optional<CustomerOrder> findByOrderNumber(String orderNumber);
    boolean existsByUserIdAndCycleId(Long userId, Long cycleId);
    long countByCycleId(Long cycleId);
    long countByStatus(String status);

    // Count unique customers who ordered in a cycle (excluding cancelled)
    @Query("SELECT COUNT(DISTINCT o.user.id) FROM CustomerOrder o WHERE o.cycle.id = :cycleId AND o.status != 'CANCELLED'")
    long countUniqueCustomersByCycleId(@Param("cycleId") Long cycleId);

    // Count how many distinct cycles a user has ordered in
    @Query("SELECT COUNT(DISTINCT o.cycle.id) FROM CustomerOrder o WHERE o.user.id = :userId AND o.status != 'CANCELLED'")
    long countDistinctCyclesByUserId(@Param("userId") Long userId);

    // All users who have placed at least one non-cancelled order
    @Query("SELECT DISTINCT o.user.id FROM CustomerOrder o WHERE o.status != 'CANCELLED'")
    List<Long> findDistinctCustomerIds();

    // Orders by user with payment status for revenue calculation
    @Query("SELECT o FROM CustomerOrder o WHERE o.user.id = :userId AND o.status != 'CANCELLED'")
    List<CustomerOrder> findActiveOrdersByUserId(@Param("userId") Long userId);
}
