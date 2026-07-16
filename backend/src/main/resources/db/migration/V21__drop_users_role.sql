-- user_roles has been the source of truth for authorisation since V19, and every
-- read path now goes through it. Keeping users.role alongside would leave two
-- answers to the same question, and a disagreement between them is an
-- authorisation bug by construction (spec §5.1).

-- V14 built this index with a WHERE role = 'STUDENT' predicate, so the column
-- cannot be dropped while it exists. It served admin student lookups by phone;
-- the replacement keeps that access path without depending on the column, and
-- the student filter is answered from user_roles instead.
DROP INDEX ix_users_student_enabled_phone;

CREATE INDEX ix_users_enabled_phone ON users (enabled, phone_number, id);

CREATE INDEX ix_user_roles_student_user
    ON user_roles (user_id)
    WHERE role = 'STUDENT';

ALTER TABLE users DROP CONSTRAINT ck_users_role;
ALTER TABLE users DROP COLUMN role;
