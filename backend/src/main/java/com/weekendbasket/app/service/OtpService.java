package com.weekendbasket.app.service;

import com.weekendbasket.app.exception.OtpExpiredException;
import com.weekendbasket.app.exception.OtpMaxAttemptsException;
import com.weekendbasket.app.model.OtpVerification;
import com.weekendbasket.app.otp.OtpSendStrategy;
import com.weekendbasket.app.repository.OtpVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

//@Service
//@RequiredArgsConstructor
public class OtpService {

    private static final Logger log = LogManager.getLogger(OtpService.class);

//    private final OtpVerificationRepository otpRepository;
//    private final OtpSendStrategy otpSendStrategy;

    @Value("${otp.expiry.minutes}")
    private int otpExpiryMinutes;

    @Value("${otp.max.attempts}")
    private int maxAttempts;

    @Transactional
    public String generateAndSaveOtp(String phoneNumber) {
        String otp = String.format("%06d", new Random().nextInt(999999));
        LocalDateTime now = LocalDateTime.now();

        OtpVerification otpVerification = OtpVerification.builder()
                .phoneNumber(phoneNumber)
                .otpCode(otp)
                .createdOn(now)
                .expiresAt(now.plusMinutes(otpExpiryMinutes))
                .attempts(0)
                .verified(false)
                .build();

//        otpRepository.save(otpVerification);

        // Delegate to active strategy — dev logs it, firebase sends real SMS
//        otpSendStrategy.send(phoneNumber, otp);

        return otp;
    }

//    @Transactional
//    public void verifyOtp(String phoneNumber, String otpCode) {
//        OtpVerification otp = otpRepository
//                .findTopByPhoneNumberAndVerifiedFalseOrderByCreatedOnDesc(phoneNumber)
//                .orElseThrow(() -> new OtpExpiredException("No active OTP found. Please request a new OTP."));
//
//        if (otp.getAttempts() >= maxAttempts) {
//            throw new OtpMaxAttemptsException("Maximum OTP attempts exceeded. Please request a new OTP.");
//        }
//
//        if (LocalDateTime.now().isAfter(otp.getExpiresAt())) {
//            throw new OtpExpiredException("OTP has expired. Please request a new OTP.");
//        }
//
//        if (!otp.getOtpCode().equals(otpCode)) {
//            otp.setAttempts(otp.getAttempts() + 1);
//            otpRepository.save(otp);
//            throw new OtpExpiredException("Invalid OTP. " + (maxAttempts - otp.getAttempts()) + " attempts remaining.");
//        }
//
//        otp.setVerified(true);
//        otpRepository.save(otp);
//        log.info("OTP verified for phone: {}", phoneNumber);
//    }
}
