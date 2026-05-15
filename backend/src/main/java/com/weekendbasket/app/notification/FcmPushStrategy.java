package com.weekendbasket.app.notification;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
public class FcmPushStrategy implements NotificationSendStrategy {

    private static final Logger log = LogManager.getLogger(FcmPushStrategy.class);

    @Override
    public void sendToUser(Long userId, String title, String body, String type) {
        // TODO Phase 7 — integrate Firebase Admin SDK
        // 1. Fetch active FCM tokens for userId from FcmTokenRepository
        // 2. Send FCM message to each token
        // 3. On UNREGISTERED error → mark token inactive
        log.info("FCM push (stub) → userId={} title={}", userId, title);
    }

    @Override
    public void broadcast(String title, String body, String type) {
        // TODO Phase 7 — fetch all active tokens, send in batches of 500
        log.info("FCM broadcast (stub) → title={}", title);
    }
}
