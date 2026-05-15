package com.weekendbasket.app.otp;

/**
 * Strategy for sending OTP to a phone number.
 * Implementations: DevOtpStrategy (log to console), FirebaseOtpStrategy (real SMS).
 * Toggle via application.properties: otp.provider=dev|firebase
 */
public interface OtpSendStrategy {
    /**
     * Send OTP to the given phone number.
     * @param phoneNumber Indian phone number e.g. 9876543210
     * @param otp         6-digit OTP code
     */
    void send(String phoneNumber, String otp);
}
