package com.weekendbasket.app.repository;

import com.weekendbasket.app.model.WeeklyCycle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WeeklyCycleRepository extends JpaRepository<WeeklyCycle, Long> {
    Optional<WeeklyCycle> findTopByStatusOrderByOrderOpenAtDesc(String status);
    boolean existsByStatus(String status);
}
