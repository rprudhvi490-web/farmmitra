package com.weekendbasket.app.repository;

import com.weekendbasket.app.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByOrderId(Long orderId);

    @Query("""
            SELECT oi.product.id, SUM(oi.quantity)
            FROM OrderItem oi
            JOIN oi.order o
            WHERE o.cycle.id = :cycleId AND o.status != 'CANCELLED'
            GROUP BY oi.product.id
            """)
    List<Object[]> aggregateByProduct(@Param("cycleId") Long cycleId);
}
