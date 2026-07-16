CREATE INDEX ix_users_role_enabled
    ON users (role, enabled);

CREATE INDEX ix_student_forms_status_submitted
    ON student_forms (status, submitted_at DESC, id DESC);

CREATE INDEX ix_school_events_starts_at
    ON school_events (starts_at, id);

CREATE INDEX ix_student_club_memberships_status_updated
    ON student_club_memberships (status, updated_at DESC, id DESC);

CREATE INDEX ix_grade_assessments_updated_at
    ON grade_assessments (updated_at DESC, id DESC);
