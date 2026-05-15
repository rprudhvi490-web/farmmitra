package com.weekendbasket.app.repository;

import com.weekendbasket.app.model.ProcurementSheet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProcurementSheetRepository extends JpaRepository<ProcurementSheet, Long> {
    List<ProcurementSheet> findByCycleId(Long cycleId);
    boolean existsByCycleId(Long cycleId);
    long countByCycleId(Long cycleId);
    long countByCycleIdAndStatus(Long cycleId, String status);
}
