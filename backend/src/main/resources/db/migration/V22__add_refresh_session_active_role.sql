-- A refresh session must remember which role it was opened for, otherwise a
-- refresh cannot know which role to mint the next access token for. Until now
-- the mobile flow assumed STUDENT, which stops being true the moment a teacher
-- or parent signs in.
ALTER TABLE refresh_sessions ADD COLUMN active_role VARCHAR(32);

-- Sessions created before this migration predate multi-role accounts, so their
-- active role is the single role their user holds.
UPDATE refresh_sessions rs
SET active_role = ur.role
FROM user_roles ur
WHERE ur.user_id = rs.user_id;

-- An account with no role cannot be loaded at all, so no session can survive
-- without one. Any leftover row here would be unusable anyway.
DELETE FROM refresh_sessions WHERE active_role IS NULL;

ALTER TABLE refresh_sessions ALTER COLUMN active_role SET NOT NULL;

ALTER TABLE refresh_sessions
    ADD CONSTRAINT ck_refresh_sessions_active_role
    CHECK (active_role IN ('STUDENT', 'TEACHER', 'PARENT', 'ADMIN'));
