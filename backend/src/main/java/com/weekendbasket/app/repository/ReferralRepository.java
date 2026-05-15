package com.weekendbasket.app.repository;

import com.weekendbasket.app.model.Referral;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReferralRepository extends JpaRepository<Referral, Long> {
    Optional<Referral> findByReferredIdAndStatus(Long referredId, String status);
    List<Referral> findByReferrerId(Long referrerId);
    boolean existsByReferredId(Long referredId);
}
