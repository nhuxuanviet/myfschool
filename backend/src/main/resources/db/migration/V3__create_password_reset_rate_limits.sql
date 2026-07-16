CREATE TABLE password_reset_rate_limits (
    phone_hash VARCHAR(64) PRIMARY KEY,
    window_started_at TIMESTAMPTZ NOT NULL,
    request_count INTEGER NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_password_reset_rate_limit_phone_hash CHECK (length(phone_hash) = 64),
    CONSTRAINT ck_password_reset_rate_limit_request_count CHECK (request_count > 0)
);

CREATE INDEX ix_password_reset_rate_limits_updated_at
    ON password_reset_rate_limits (updated_at);
