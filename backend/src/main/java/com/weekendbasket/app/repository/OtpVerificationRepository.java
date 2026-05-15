package com.weekendbasket.app.repository;

import com.weekendbasket.app.model.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    Optional<OtpVerification> findTopByPhoneNumberAndVerifiedFalseOrderByCreatedOnDesc(String phoneNumber);

    @Modifying
    @Query("DELETE FROM OtpVerification o WHERE o.expiresAt < :now OR o.verified = true")
    void deleteExpiredAndVerified(@Param("now") LocalDateTime now);
}
