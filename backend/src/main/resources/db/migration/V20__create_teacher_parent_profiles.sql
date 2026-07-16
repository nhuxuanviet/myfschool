-- user_id is nullable on purpose: a school enters its teacher roster before it
-- issues accounts. A profile without an account can still be assigned to a class
-- and appear on a timetable; it just cannot sign in. PostgreSQL allows several
-- NULLs under a UNIQUE constraint, so unaccounted profiles do not collide.
CREATE TABLE teacher_profiles (
    id UUID PRIMARY KEY,
    user_id UUID,
    teacher_code VARCHAR(32) NOT NULL,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(190),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_teacher_profiles_user_id UNIQUE (user_id),
    CONSTRAINT uk_teacher_profiles_code UNIQUE (teacher_code),
    CONSTRAINT fk_teacher_profiles_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT ck_teacher_profiles_code CHECK (btrim(teacher_code) <> ''),
    CONSTRAINT ck_teacher_profiles_full_name CHECK (btrim(full_name) <> ''),
    CONSTRAINT ck_teacher_profiles_version CHECK (version >= 0)
);

CREATE TABLE parent_profiles (
    id UUID PRIMARY KEY,
    user_id UUID,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(190),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_parent_profiles_user_id UNIQUE (user_id),
    CONSTRAINT fk_parent_profiles_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT ck_parent_profiles_full_name CHECK (btrim(full_name) <> ''),
    CONSTRAINT ck_parent_profiles_version CHECK (version >= 0)
);

CREATE TABLE parent_student_links (
    id UUID PRIMARY KEY,
    parent_id UUID NOT NULL,
    student_id UUID NOT NULL,
    relationship VARCHAR(32) NOT NULL,
    contact_order SMALLINT NOT NULL DEFAULT 1,
    effective_from DATE NOT NULL,
    effective_to DATE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_parent_student_links_parent FOREIGN KEY (parent_id)
        REFERENCES parent_profiles (id) ON DELETE CASCADE,
    CONSTRAINT fk_parent_student_links_student FOREIGN KEY (student_id)
        REFERENCES students (id) ON DELETE CASCADE,
    CONSTRAINT ck_parent_student_links_relationship
        CHECK (relationship IN ('FATHER', 'MOTHER', 'GUARDIAN')),
    CONSTRAINT ck_parent_student_links_contact_order CHECK (contact_order >= 1),
    CONSTRAINT ck_parent_student_links_effective_range
        CHECK (effective_to IS NULL OR effective_to > effective_from)
);

-- A parent and a student may have only one link in force at a time. Ended links
-- are kept for the audit trail, so the uniqueness applies to open links only.
CREATE UNIQUE INDEX uk_parent_student_links_active
    ON parent_student_links (parent_id, student_id)
    WHERE effective_to IS NULL;

CREATE INDEX ix_parent_student_links_student
    ON parent_student_links (student_id, contact_order);
CREATE INDEX ix_parent_student_links_parent_active
    ON parent_student_links (parent_id)
    WHERE effective_to IS NULL;
