CREATE TABLE users (
    id UUID PRIMARY KEY,
    phone_number VARCHAR(10) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(32) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    credentials_updated_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_users_phone_number UNIQUE (phone_number),
    CONSTRAINT ck_users_phone_number CHECK (phone_number ~ '^0[35789][0-9]{8}$'),
    CONSTRAINT ck_users_role CHECK (role IN ('STUDENT'))
);

CREATE TABLE students (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    student_code VARCHAR(32) NOT NULL,
    full_name VARCHAR(120) NOT NULL,
    grade_level SMALLINT NOT NULL,
    class_name VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_students_user_id UNIQUE (user_id),
    CONSTRAINT uk_students_student_code UNIQUE (student_code),
    CONSTRAINT fk_students_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_students_grade_level CHECK (grade_level BETWEEN 6 AND 12)
);

CREATE TABLE refresh_sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    family_id UUID NOT NULL,
    parent_session_id UUID,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    revocation_reason VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_refresh_sessions_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_sessions_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_refresh_sessions_parent FOREIGN KEY (parent_session_id)
        REFERENCES refresh_sessions (id) ON DELETE SET NULL,
    CONSTRAINT ck_refresh_sessions_token_hash CHECK (length(token_hash) = 64),
    CONSTRAINT ck_refresh_sessions_expiry CHECK (expires_at > created_at)
);

CREATE INDEX ix_refresh_sessions_user_active
    ON refresh_sessions (user_id)
    WHERE revoked_at IS NULL;
CREATE INDEX ix_refresh_sessions_family
    ON refresh_sessions (family_id);

CREATE TABLE password_reset_challenges (
    id UUID PRIMARY KEY,
    user_id UUID,
    phone_hash VARCHAR(64) NOT NULL,
    otp_hash VARCHAR(64) NOT NULL,
    attempts SMALLINT NOT NULL DEFAULT 0,
    expires_at TIMESTAMPTZ NOT NULL,
    verified_at TIMESTAMPTZ,
    reset_token_hash VARCHAR(64),
    reset_token_expires_at TIMESTAMPTZ,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_password_reset_token_hash UNIQUE (reset_token_hash),
    CONSTRAINT fk_password_reset_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_password_reset_phone_hash CHECK (length(phone_hash) = 64),
    CONSTRAINT ck_password_reset_otp_hash CHECK (length(otp_hash) = 64),
    CONSTRAINT ck_password_reset_token_hash CHECK (
        reset_token_hash IS NULL OR length(reset_token_hash) = 64
    ),
    CONSTRAINT ck_password_reset_attempts CHECK (attempts >= 0),
    CONSTRAINT ck_password_reset_expiry CHECK (expires_at > created_at),
    CONSTRAINT ck_password_reset_verification CHECK (
        (verified_at IS NULL AND reset_token_hash IS NULL AND reset_token_expires_at IS NULL)
        OR
        (verified_at IS NOT NULL AND reset_token_hash IS NOT NULL AND reset_token_expires_at IS NOT NULL)
    )
);

CREATE INDEX ix_password_reset_user_active
    ON password_reset_challenges (user_id)
    WHERE used_at IS NULL;
