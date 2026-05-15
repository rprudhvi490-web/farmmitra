-- V3__add_has_password.sql
-- Adds has_password flag to app_user
-- Existing users default to false — they must use OTP until they set a password

ALTER TABLE app_user ADD COLUMN has_password BOOLEAN NOT NULL DEFAULT FALSE;
