CREATE TABLE user_roles (
    user_id UUID NOT NULL,
    role VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_user_roles_role
        CHECK (role IN ('STUDENT', 'TEACHER', 'PARENT', 'ADMIN'))
);

-- users.role is the current source of truth. Copy it verbatim; do not infer
-- additional roles. The column itself is dropped in V21, once every read path
-- has moved to this table.
INSERT INTO user_roles (user_id, role, created_at)
SELECT id, role, CURRENT_TIMESTAMP FROM users;

CREATE INDEX ix_user_roles_role_user ON user_roles (role, user_id);
