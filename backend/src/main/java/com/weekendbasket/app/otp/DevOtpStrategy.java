package com.weekendbasket.app.otp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "otp.provider", havingValue = "dev", matchIfMissing = true)
public class DevOtpStrategy implements OtpSendStrategy {

    private static final Logger log = LogManager.getLogger(DevOtpStrategy.class);

    @Override
    public void send(String phoneNumber, String otp) {
        // DEV MODE — OTP printed to logs, no real SMS sent
        log.info("╔══════════════════════════════════════╗");
        log.info("║  DEV OTP  →  {}  →  {}  ║", phoneNumber, otp);
        log.info("╚══════════════════════════════════════╝");
    }
}
