-- Authorisation comes from a relationship, never from a role alone (spec §3).
-- These two tables are that relationship: a TEACHER account can touch nothing
-- until a row here says which class, subject and term they are responsible for.

-- One teacher is responsible for a given class-subject-term. Co-teaching is not
-- modelled: the school names one person accountable for the grade book, and a
-- second teacher would make "who may publish these marks" ambiguous.
CREATE TABLE teacher_subject_assignments (
    id UUID PRIMARY KEY,
    teacher_id UUID NOT NULL,
    class_id UUID NOT NULL,
    subject_id UUID NOT NULL,
    academic_term_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_teacher_subject_assignments_slot
        UNIQUE (class_id, subject_id, academic_term_id),
    CONSTRAINT fk_teacher_subject_assignments_teacher FOREIGN KEY (teacher_id)
        REFERENCES teacher_profiles (id) ON DELETE RESTRICT,
    CONSTRAINT fk_teacher_subject_assignments_class FOREIGN KEY (class_id)
        REFERENCES school_classes (id) ON DELETE RESTRICT,
    CONSTRAINT fk_teacher_subject_assignments_subject FOREIGN KEY (subject_id)
        REFERENCES subjects (id) ON DELETE RESTRICT,
    CONSTRAINT fk_teacher_subject_assignments_term FOREIGN KEY (academic_term_id)
        REFERENCES academic_terms (id) ON DELETE RESTRICT
);

-- Every class has exactly one homeroom teacher per year. Being a homeroom
-- teacher and teaching a subject are independent relationships: the same person
-- may hold both, in different classes, and neither implies the other.
CREATE TABLE homeroom_assignments (
    id UUID PRIMARY KEY,
    teacher_id UUID NOT NULL,
    class_id UUID NOT NULL,
    academic_year_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_homeroom_assignments_class_year UNIQUE (class_id, academic_year_id),
    CONSTRAINT fk_homeroom_assignments_teacher FOREIGN KEY (teacher_id)
        REFERENCES teacher_profiles (id) ON DELETE RESTRICT,
    CONSTRAINT fk_homeroom_assignments_class FOREIGN KEY (class_id)
        REFERENCES school_classes (id) ON DELETE RESTRICT,
    CONSTRAINT fk_homeroom_assignments_year FOREIGN KEY (academic_year_id)
        REFERENCES academic_years (id) ON DELETE RESTRICT
);

-- Every teacher-facing query starts from "what am I assigned to", so the
-- teacher side of both relationships is the leading column.
CREATE INDEX ix_teacher_subject_assignments_teacher
    ON teacher_subject_assignments (teacher_id, academic_term_id, class_id);
CREATE INDEX ix_teacher_subject_assignments_class
    ON teacher_subject_assignments (class_id, academic_term_id);
CREATE INDEX ix_homeroom_assignments_teacher
    ON homeroom_assignments (teacher_id, academic_year_id);
