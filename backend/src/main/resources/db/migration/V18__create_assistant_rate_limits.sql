CREATE TABLE assistant_rate_limits (
    user_id UUID PRIMARY KEY,
    window_started_at TIMESTAMPTZ NOT NULL,
    request_count INTEGER NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_assistant_rate_limits_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_assistant_rate_limits_request_count CHECK (request_count > 0)
);

CREATE INDEX idx_assistant_rate_limits_updated_at
    ON assistant_rate_limits (updated_at);

