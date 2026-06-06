-- V7: Track issued JWT tokens per user for active session monitoring
CREATE TABLE user_token (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT       NOT NULL REFERENCES app_user(id),
    token_hash    VARCHAR(64)  NOT NULL,          -- SHA-256 hex of the raw token
    issued_at     TIMESTAMP    NOT NULL,
    expired_at    TIMESTAMP    NOT NULL,
    last_used_at  TIMESTAMP    NOT NULL,
    device_hint   VARCHAR(200),                   -- User-Agent snippet
    CONSTRAINT uq_user_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_user_token_user_id   ON user_token(user_id);
CREATE INDEX idx_user_token_last_used ON user_token(last_used_at);
