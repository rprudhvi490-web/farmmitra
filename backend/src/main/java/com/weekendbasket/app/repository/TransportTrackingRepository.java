package com.weekendbasket.app.repository;

import com.weekendbasket.app.model.TransportTracking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransportTrackingRepository extends JpaRepository<TransportTracking, Long> {
    List<TransportTracking> findByCycleIdOrderByCreatedOnAsc(Long cycleId);
    Optional<TransportTracking> findTopByCycleIdOrderByCreatedOnDesc(Long cycleId);
}
