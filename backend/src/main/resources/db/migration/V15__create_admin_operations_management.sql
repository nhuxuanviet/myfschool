ALTER TABLE term_period_definitions ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE class_timetable_entries
    ADD COLUMN school_class_id UUID,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

UPDATE class_timetable_entries entry
SET school_class_id = school_class.id
FROM school_classes school_class
JOIN academic_terms term ON term.academic_year_id = school_class.academic_year_id
WHERE entry.academic_term_id = term.id
  AND entry.class_name = school_class.code;

ALTER TABLE class_timetable_entries
    ADD CONSTRAINT fk_class_timetable_entries_school_class
        FOREIGN KEY (school_class_id) REFERENCES school_classes (id) ON DELETE RESTRICT;

ALTER TABLE timetable_overrides
    ADD COLUMN school_class_id UUID,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

UPDATE timetable_overrides override_entry
SET school_class_id = school_class.id
FROM school_classes school_class
JOIN academic_terms term ON term.academic_year_id = school_class.academic_year_id
WHERE override_entry.academic_term_id = term.id
  AND override_entry.class_name = school_class.code;

ALTER TABLE timetable_overrides
    ADD CONSTRAINT fk_timetable_overrides_school_class
        FOREIGN KEY (school_class_id) REFERENCES school_classes (id) ON DELETE RESTRICT;

ALTER TABLE student_term_subjects ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE grade_assessments ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE student_forms ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE announcements ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE school_events
    ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE student_event_registrations ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE school_clubs
    ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE student_club_memberships ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

CREATE INDEX ix_admin_timetable_entries_class_term
    ON class_timetable_entries (school_class_id, academic_term_id, day_of_week, session, period_number);
CREATE INDEX ix_admin_timetable_overrides_class_term_date
    ON timetable_overrides (school_class_id, academic_term_id, lesson_date, session, period_number);
CREATE INDEX ix_admin_grade_subjects_student_term
    ON student_term_subjects (student_id, academic_term_id, subject_id);
CREATE INDEX ix_admin_grade_assessments_status_date
    ON grade_assessments (status, assessed_on DESC, id);
CREATE INDEX ix_admin_forms_status_updated
    ON student_forms (status, updated_at DESC, id DESC);
CREATE INDEX ix_admin_announcements_published
    ON announcements (published_at DESC, id DESC);
CREATE INDEX ix_admin_events_enabled_start
    ON school_events (enabled, starts_at DESC, id DESC);
CREATE INDEX ix_admin_clubs_enabled_name
    ON school_clubs (enabled, name, id);
CREATE INDEX ix_admin_audit_actor_time
    ON admin_audit_events (actor_user_id, occurred_at DESC, id DESC);
