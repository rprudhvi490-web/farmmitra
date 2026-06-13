package com.weekendbasket.app.repository;

import com.weekendbasket.app.model.OtpTracker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;

@Repository
public interface OtpTrackerRepository extends JpaRepository<OtpTracker, LocalDate> {
}