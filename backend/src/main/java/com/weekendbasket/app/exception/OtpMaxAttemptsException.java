package com.weekendbasket.app.exception;

public class OtpMaxAttemptsException extends RuntimeException {
    public OtpMaxAttemptsException(String message) {
        super(message);
    }
}
