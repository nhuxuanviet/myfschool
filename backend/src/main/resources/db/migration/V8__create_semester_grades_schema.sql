CREATE TABLE student_term_subjects (
    id UUID PRIMARY KEY,
    student_id UUID NOT NULL,
    academic_term_id UUID NOT NULL,
    subject_id UUID NOT NULL,
    assessment_mode VARCHAR(16) NOT NULL,
    annual_lesson_count SMALLINT,
    display_order SMALLINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_student_term_subjects_student_term_subject
        UNIQUE (student_id, academic_term_id, subject_id),
    CONSTRAINT uk_student_term_subjects_student_term_display_order
        UNIQUE (student_id, academic_term_id, display_order),
    CONSTRAINT uk_student_term_subjects_id_mode
        UNIQUE (id, assessment_mode),
    CONSTRAINT fk_student_term_subjects_student
        FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE CASCADE,
    CONSTRAINT fk_student_term_subjects_academic_term
        FOREIGN KEY (academic_term_id) REFERENCES academic_terms (id) ON DELETE RESTRICT,
    CONSTRAINT fk_student_term_subjects_subject
        FOREIGN KEY (subject_id) REFERENCES subjects (id) ON DELETE RESTRICT,
    CONSTRAINT ck_student_term_subjects_assessment_mode
        CHECK (assessment_mode IN ('NUMERIC', 'REMARK')),
    CONSTRAINT ck_student_term_subjects_annual_lesson_count
        CHECK (
            (assessment_mode = 'NUMERIC'
                AND annual_lesson_count IS NOT NULL
                AND annual_lesson_count >= 35)
            OR
            (assessment_mode = 'REMARK' AND annual_lesson_count IS NULL)
        ),
    CONSTRAINT ck_student_term_subjects_display_order
        CHECK (display_order > 0)
);

CREATE INDEX ix_student_term_subjects_student_term_order
    ON student_term_subjects (student_id, academic_term_id, display_order, id);

CREATE TABLE grade_assessments (
    id UUID PRIMARY KEY,
    student_term_subject_id UUID NOT NULL,
    assessment_mode VARCHAR(16) NOT NULL,
    assessment_kind VARCHAR(16) NOT NULL,
    assessment_form VARCHAR(16) NOT NULL,
    display_label VARCHAR(120) NOT NULL,
    duration_minutes SMALLINT,
    status VARCHAR(20) NOT NULL,
    score NUMERIC(3, 1),
    outcome VARCHAR(16),
    assessed_on DATE,
    display_order SMALLINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_grade_assessments_subject_display_order
        UNIQUE (student_term_subject_id, display_order),
    CONSTRAINT fk_grade_assessments_subject_mode
        FOREIGN KEY (student_term_subject_id, assessment_mode)
        REFERENCES student_term_subjects (id, assessment_mode)
        ON DELETE CASCADE,
    CONSTRAINT ck_grade_assessments_mode
        CHECK (assessment_mode IN ('NUMERIC', 'REMARK')),
    CONSTRAINT ck_grade_assessments_kind
        CHECK (assessment_kind IN ('REGULAR', 'MIDTERM', 'FINAL')),
    CONSTRAINT ck_grade_assessments_form
        CHECK (assessment_form IN (
            'ORAL', 'WRITTEN', 'PRESENTATION', 'PRACTICAL', 'EXPERIMENT',
            'PRODUCT', 'PROJECT'
        )),
    CONSTRAINT ck_grade_assessments_display_label
        CHECK (btrim(display_label) <> ''),
    CONSTRAINT ck_grade_assessments_duration_minutes
        CHECK (duration_minutes IS NULL OR duration_minutes BETWEEN 1 AND 180),
    CONSTRAINT ck_grade_assessments_status
        CHECK (status IN ('RECORDED', 'MAKE_UP_REQUIRED', 'EXCUSED', 'ABSENT_FINALIZED')),
    CONSTRAINT ck_grade_assessments_score
        CHECK (score IS NULL OR score BETWEEN 0.0 AND 10.0),
    CONSTRAINT ck_grade_assessments_outcome
        CHECK (outcome IS NULL OR outcome IN ('ACHIEVED', 'NOT_ACHIEVED')),
    CONSTRAINT ck_grade_assessments_display_order
        CHECK (display_order > 0),
    CONSTRAINT ck_grade_assessments_result_by_status
        CHECK (
            (
                status IN ('RECORDED', 'ABSENT_FINALIZED')
                AND (
                    (score IS NOT NULL AND outcome IS NULL)
                    OR (score IS NULL AND outcome IS NOT NULL)
                )
            )
            OR
            (
                status IN ('MAKE_UP_REQUIRED', 'EXCUSED')
                AND score IS NULL
                AND outcome IS NULL
            )
        ),
    CONSTRAINT ck_grade_assessments_result_by_mode
        CHECK (
            (assessment_mode = 'NUMERIC' AND outcome IS NULL)
            OR
            (assessment_mode = 'REMARK' AND score IS NULL)
        )
);

CREATE UNIQUE INDEX ux_grade_assessments_subject_midterm
    ON grade_assessments (student_term_subject_id)
    WHERE assessment_kind = 'MIDTERM';

CREATE UNIQUE INDEX ux_grade_assessments_subject_final
    ON grade_assessments (student_term_subject_id)
    WHERE assessment_kind = 'FINAL';

CREATE INDEX ix_grade_assessments_subject_order
    ON grade_assessments (student_term_subject_id, display_order, id);
