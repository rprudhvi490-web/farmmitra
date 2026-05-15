package com.weekendbasket.app.repository;

import com.weekendbasket.app.model.FcmToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {
    List<FcmToken> findByUserIdAndActiveTrue(Long userId);
    List<FcmToken> findByActiveTrue();
    Optional<FcmToken> findByToken(String token);
}
