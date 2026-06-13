-- V8: Remove otp_verification table
-- Firebase Phone Auth now handles all OTP generation, delivery and verification.
-- This table is no longer needed.
DROP TABLE IF EXISTS otp_verification;
