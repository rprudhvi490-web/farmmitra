package com.weekendbasket.app.repository;

import com.weekendbasket.app.model.DeliveryBatchOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeliveryBatchOrderRepository extends JpaRepository<DeliveryBatchOrder, Long> {
    List<DeliveryBatchOrder> findByBatchId(Long batchId);
    boolean existsByOrderId(Long orderId);
}
