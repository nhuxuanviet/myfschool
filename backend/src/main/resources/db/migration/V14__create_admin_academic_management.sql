ALTER TABLE academic_years ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE academic_terms ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE subjects ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE subjects ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

CREATE TABLE school_classes (
    id UUID PRIMARY KEY,
    academic_year_id UUID NOT NULL,
    code VARCHAR(32) NOT NULL,
    name VARCHAR(80) NOT NULL,
    grade_level SMALLINT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_school_classes_year_code UNIQUE (academic_year_id, code),
    CONSTRAINT fk_school_classes_academic_year
        FOREIGN KEY (academic_year_id) REFERENCES academic_years (id) ON DELETE RESTRICT,
    CONSTRAINT ck_school_classes_code CHECK (btrim(code) <> ''),
    CONSTRAINT ck_school_classes_name CHECK (btrim(name) <> ''),
    CONSTRAINT ck_school_classes_grade_level CHECK (grade_level BETWEEN 6 AND 12),
    CONSTRAINT ck_school_classes_version CHECK (version >= 0)
);

ALTER TABLE students ADD COLUMN class_id UUID;
ALTER TABLE students ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE students
    ADD CONSTRAINT fk_students_school_class
    FOREIGN KEY (class_id) REFERENCES school_classes (id) ON DELETE RESTRICT;
ALTER TABLE students ADD CONSTRAINT ck_students_version CHECK (version >= 0);

WITH latest_year AS (
    SELECT id
    FROM academic_years
    ORDER BY ends_on DESC, id
    LIMIT 1
), existing_classes AS (
    SELECT DISTINCT s.class_name, s.grade_level, y.id AS academic_year_id
    FROM students s
    CROSS JOIN latest_year y
)
INSERT INTO school_classes (
    id, academic_year_id, code, name, grade_level, enabled, version, created_at, updated_at
)
SELECT
    md5(existing_classes.academic_year_id::text || ':' || existing_classes.class_name)::uuid,
    existing_classes.academic_year_id,
    existing_classes.class_name,
    existing_classes.class_name,
    existing_classes.grade_level,
    TRUE,
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM existing_classes
ON CONFLICT (academic_year_id, code) DO NOTHING;

UPDATE students student
SET class_id = school_class.id
FROM school_classes school_class
WHERE student.class_name = school_class.code
  AND student.grade_level = school_class.grade_level
  AND student.class_id IS NULL;

CREATE INDEX ix_school_classes_year_grade_enabled
    ON school_classes (academic_year_id, grade_level, enabled, code);
CREATE INDEX ix_students_admin_filters
    ON students (grade_level, class_id, student_code, id);
CREATE INDEX ix_students_normalized_name
    ON students (lower(full_name), id);
CREATE INDEX ix_users_student_enabled_phone
    ON users (enabled, phone_number, id)
    WHERE role = 'STUDENT';

CREATE TABLE admin_audit_events (
    id UUID PRIMARY KEY,
    actor_user_id UUID,
    action VARCHAR(64) NOT NULL,
    entity_type VARCHAR(64) NOT NULL,
    entity_id UUID NOT NULL,
    changed_fields JSONB NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_admin_audit_events_actor
        FOREIGN KEY (actor_user_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT ck_admin_audit_events_action CHECK (btrim(action) <> ''),
    CONSTRAINT ck_admin_audit_events_entity_type CHECK (btrim(entity_type) <> ''),
    CONSTRAINT ck_admin_audit_events_changed_fields CHECK (jsonb_typeof(changed_fields) = 'object')
);

CREATE INDEX ix_admin_audit_events_occurred_at
    ON admin_audit_events (occurred_at DESC, id DESC);
CREATE INDEX ix_admin_audit_events_entity
    ON admin_audit_events (entity_type, entity_id, occurred_at DESC);
