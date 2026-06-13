package com.weekendbasket.app.scheduler;

import com.weekendbasket.app.repository.InvalidatedTokenRepository;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class CleanupScheduler {

    private static final Logger log = LogManager.getLogger(CleanupScheduler.class);

    private final InvalidatedTokenRepository invalidatedTokenRepository;

    @Value("${jwt.expiration.ms}")
    private long jwtExpirationMs;

    @Scheduled(cron = "${scheduler.token.cleanup.cron}")
    @Async("appTaskExecutor")
    @Transactional
    public void cleanupExpiredTokens() {
        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(jwtExpirationMs / 1000);
        invalidatedTokenRepository.deleteOlderThan(cutoff);
        log.info("Expired invalidated tokens cleaned up");
    }
}
