package com.weekendbasket.app.repository;

import com.weekendbasket.app.model.UserToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserTokenRepository extends JpaRepository<UserToken, Long> {

    Optional<UserToken> findByTokenHash(String tokenHash);

    // Active = not invalidated + not expired
    @Query("""
        SELECT ut FROM UserToken ut
        WHERE ut.user.id = :userId
          AND ut.expiredAt > :now
          AND ut.tokenHash NOT IN (
              SELECT it.token FROM InvalidatedToken it
          )
        ORDER BY ut.lastUsedAt DESC
    """)
    List<UserToken> findActiveSessions(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE UserToken ut SET ut.lastUsedAt = :now WHERE ut.tokenHash = :hash")
    void updateLastUsed(@Param("hash") String hash, @Param("now") LocalDateTime now);
}
