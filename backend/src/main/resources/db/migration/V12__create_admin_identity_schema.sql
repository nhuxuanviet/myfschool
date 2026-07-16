ALTER TABLE users DROP CONSTRAINT ck_users_role;

ALTER TABLE users
    ADD CONSTRAINT ck_users_role CHECK (role IN ('STUDENT', 'ADMIN'));

CREATE TABLE admin_profiles (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    full_name VARCHAR(120) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_admin_profiles_user_id UNIQUE (user_id),
    CONSTRAINT fk_admin_profiles_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_admin_profiles_full_name CHECK (btrim(full_name) <> '')
);

CREATE TABLE admin_login_attempts (
    identifier_hash VARCHAR(64) PRIMARY KEY,
    window_started_at TIMESTAMPTZ NOT NULL,
    attempt_count INTEGER NOT NULL,
    blocked_until TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_admin_login_attempts_hash CHECK (length(identifier_hash) = 64),
    CONSTRAINT ck_admin_login_attempts_count CHECK (attempt_count > 0),
    CONSTRAINT ck_admin_login_attempts_block CHECK (
        blocked_until IS NULL OR blocked_until >= window_started_at
    )
);

CREATE INDEX ix_admin_login_attempts_updated_at
    ON admin_login_attempts (updated_at);

CREATE TABLE security_audit_events (
    id UUID PRIMARY KEY,
    user_id UUID,
    event_type VARCHAR(64) NOT NULL,
    identifier_hash VARCHAR(64),
    occurred_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_security_audit_events_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT ck_security_audit_events_type CHECK (btrim(event_type) <> ''),
    CONSTRAINT ck_security_audit_events_hash CHECK (
        identifier_hash IS NULL OR length(identifier_hash) = 64
    )
);

CREATE INDEX ix_security_audit_events_occurred_at
    ON security_audit_events (occurred_at DESC);

CREATE INDEX ix_security_audit_events_user
    ON security_audit_events (user_id, occurred_at DESC)
    WHERE user_id IS NOT NULL;
