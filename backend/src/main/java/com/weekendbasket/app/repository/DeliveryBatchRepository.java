package com.weekendbasket.app.repository;

import com.weekendbasket.app.model.DeliveryBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeliveryBatchRepository extends JpaRepository<DeliveryBatch, Long> {
    List<DeliveryBatch> findByCycleId(Long cycleId);
    List<DeliveryBatch> findByAssignedToId(Long userId);
    long countByCycleIdAndStatusNot(Long cycleId, String status);
}
