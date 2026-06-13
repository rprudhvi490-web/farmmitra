CREATE TABLE otp_tracker (
    tracker_date DATE PRIMARY KEY,
    request_count INT NOT NULL DEFAULT 0
);