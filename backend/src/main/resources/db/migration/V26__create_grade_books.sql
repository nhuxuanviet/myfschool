-- A grade book is the unit a teacher is responsible for: one class, one subject,
-- one term — exactly the shape of a teaching assignment (spec §5.4).
--
-- published_at and locked_at are two different things and must not be merged
-- (spec §5.5). Publishing is the subject teacher saying students and guardians
-- may see these marks. Locking is the administration saying the teacher may no
-- longer edit them directly. A book can be published and still open.
CREATE TABLE grade_books (
    id UUID PRIMARY KEY,
    class_id UUID NOT NULL,
    subject_id UUID NOT NULL,
    academic_term_id UUID NOT NULL,
    published_at TIMESTAMPTZ,
    published_by UUID,
    locked_at TIMESTAMPTZ,
    locked_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_grade_books_slot UNIQUE (class_id, subject_id, academic_term_id),
    CONSTRAINT fk_grade_books_class FOREIGN KEY (class_id)
        REFERENCES school_classes (id) ON DELETE RESTRICT,
    CONSTRAINT fk_grade_books_subject FOREIGN KEY (subject_id)
        REFERENCES subjects (id) ON DELETE RESTRICT,
    CONSTRAINT fk_grade_books_term FOREIGN KEY (academic_term_id)
        REFERENCES academic_terms (id) ON DELETE RESTRICT,
    CONSTRAINT fk_grade_books_published_by FOREIGN KEY (published_by)
        REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT fk_grade_books_locked_by FOREIGN KEY (locked_by)
        REFERENCES users (id) ON DELETE SET NULL,
    -- Migrated books were already visible before publication auditing existed, so
    -- they may have a publication timestamp without an actor. New publications
    -- still cannot name an actor without also recording when they happened.
    CONSTRAINT ck_grade_books_published CHECK (
        published_by IS NULL OR published_at IS NOT NULL),
    CONSTRAINT ck_grade_books_locked CHECK (
        (locked_at IS NULL AND locked_by IS NULL)
        OR (locked_at IS NOT NULL AND locked_by IS NOT NULL)),
    CONSTRAINT ck_grade_books_version CHECK (version >= 0)
);

-- A column is a thing, not a convention. Making it an entity is what keeps a
-- class's grade table aligned by construction instead of hoping every row was
-- typed consistently, and renaming a column becomes one row changing.
CREATE TABLE grade_columns (
    id UUID PRIMARY KEY,
    grade_book_id UUID NOT NULL,
    assessment_kind VARCHAR(16) NOT NULL,
    assessment_form VARCHAR(16) NOT NULL,
    display_label VARCHAR(120) NOT NULL,
    duration_minutes SMALLINT,
    display_order SMALLINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_grade_columns_book_order UNIQUE (grade_book_id, display_order),
    CONSTRAINT fk_grade_columns_book FOREIGN KEY (grade_book_id)
        REFERENCES grade_books (id) ON DELETE CASCADE,
    CONSTRAINT ck_grade_columns_kind
        CHECK (assessment_kind IN ('REGULAR', 'MIDTERM', 'FINAL')),
    CONSTRAINT ck_grade_columns_form
        CHECK (assessment_form IN (
            'ORAL', 'WRITTEN', 'PRESENTATION', 'PRACTICAL', 'EXPERIMENT',
            'PRODUCT', 'PROJECT')),
    CONSTRAINT ck_grade_columns_label CHECK (btrim(display_label) <> ''),
    CONSTRAINT ck_grade_columns_duration
        CHECK (duration_minutes IS NULL OR duration_minutes BETWEEN 1 AND 180),
    CONSTRAINT ck_grade_columns_display_order CHECK (display_order > 0)
);

-- Circular 22/2021 gives a subject one midterm and one final mark per term.
CREATE UNIQUE INDEX ux_grade_columns_book_midterm
    ON grade_columns (grade_book_id) WHERE assessment_kind = 'MIDTERM';
CREATE UNIQUE INDEX ux_grade_columns_book_final
    ON grade_columns (grade_book_id) WHERE assessment_kind = 'FINAL';

-- One grade book per (class, subject, term) implied by the marks that already
-- exist. The class comes from the student, because that is the only place the
-- old data records it.
INSERT INTO grade_books (
    id, class_id, subject_id, academic_term_id, published_at, published_by,
    version, created_at, updated_at
)
SELECT
    md5('grade-book:' || student.class_id::text || ':' || term_subject.subject_id::text
        || ':' || term_subject.academic_term_id::text)::uuid,
    student.class_id,
    term_subject.subject_id,
    term_subject.academic_term_id,
    -- Every mark that exists today is already visible to students, so the books
    -- start published. Introducing the gate must not blank the app (spec §12).
    CURRENT_TIMESTAMP,
    NULL,
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM student_term_subjects term_subject
INNER JOIN students student ON student.id = term_subject.student_id
WHERE student.class_id IS NOT NULL
GROUP BY student.class_id, term_subject.subject_id, term_subject.academic_term_id
ON CONFLICT (class_id, subject_id, academic_term_id) DO NOTHING;

ALTER TABLE grade_assessments ADD COLUMN grade_column_id UUID;

-- Columns are derived from the mark rows that already exist, keyed by the shape
-- the teacher used. A mark that matches no column would be data loss, so the
-- grouping covers every distinct shape rather than a fixed list.
INSERT INTO grade_columns (
    id, grade_book_id, assessment_kind, assessment_form, display_label,
    duration_minutes, display_order, created_at, updated_at
)
SELECT
    md5('grade-column:' || book.id::text || ':' || assessment.display_order::text)::uuid,
    book.id,
    max(assessment.assessment_kind),
    max(assessment.assessment_form),
    max(assessment.display_label),
    max(assessment.duration_minutes),
    assessment.display_order,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM grade_assessments assessment
INNER JOIN student_term_subjects term_subject
    ON term_subject.id = assessment.student_term_subject_id
INNER JOIN students student ON student.id = term_subject.student_id
INNER JOIN grade_books book
    ON book.class_id = student.class_id
    AND book.subject_id = term_subject.subject_id
    AND book.academic_term_id = term_subject.academic_term_id
GROUP BY book.id, assessment.display_order
ON CONFLICT (grade_book_id, display_order) DO NOTHING;

UPDATE grade_assessments assessment
SET grade_column_id = grade_column.id
FROM student_term_subjects term_subject
INNER JOIN students student ON student.id = term_subject.student_id
INNER JOIN grade_books book
    ON book.class_id = student.class_id
    AND book.subject_id = term_subject.subject_id
    AND book.academic_term_id = term_subject.academic_term_id
INNER JOIN grade_columns grade_column ON grade_column.grade_book_id = book.id
WHERE term_subject.id = assessment.student_term_subject_id
  AND grade_column.display_order = assessment.display_order;

ALTER TABLE grade_assessments
    ADD CONSTRAINT fk_grade_assessments_column FOREIGN KEY (grade_column_id)
    REFERENCES grade_columns (id) ON DELETE CASCADE;

-- A teacher creates a column and fills it in over days. "Not entered yet" is
-- ordinary and must be representable; without PENDING the only way to show an
-- empty cell is to not create the row, which is what made the table ragged.
ALTER TABLE grade_assessments DROP CONSTRAINT ck_grade_assessments_status;
ALTER TABLE grade_assessments ADD CONSTRAINT ck_grade_assessments_status
    CHECK (status IN ('PENDING', 'RECORDED', 'MAKE_UP_REQUIRED', 'EXCUSED', 'ABSENT_FINALIZED'));

ALTER TABLE grade_assessments DROP CONSTRAINT ck_grade_assessments_result_by_status;
ALTER TABLE grade_assessments ADD CONSTRAINT ck_grade_assessments_result_by_status CHECK (
    (status IN ('RECORDED', 'ABSENT_FINALIZED')
        AND ((score IS NOT NULL AND outcome IS NULL) OR (score IS NULL AND outcome IS NOT NULL)))
    OR (status IN ('PENDING', 'MAKE_UP_REQUIRED', 'EXCUSED')
        AND score IS NULL AND outcome IS NULL));

-- Once a book is locked the teacher asks instead of edits, and the request
-- carries what it was, what it should be, and why (spec §7.1).
CREATE TABLE grade_change_requests (
    id UUID PRIMARY KEY,
    grade_assessment_id UUID NOT NULL,
    requested_by UUID NOT NULL,
    old_score NUMERIC(3, 1),
    old_outcome VARCHAR(16),
    new_score NUMERIC(3, 1),
    new_outcome VARCHAR(16),
    reason VARCHAR(500) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    decided_by UUID,
    decided_at TIMESTAMPTZ,
    decision_note VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_grade_change_requests_assessment FOREIGN KEY (grade_assessment_id)
        REFERENCES grade_assessments (id) ON DELETE CASCADE,
    CONSTRAINT fk_grade_change_requests_requester FOREIGN KEY (requested_by)
        REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_grade_change_requests_decider FOREIGN KEY (decided_by)
        REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT ck_grade_change_requests_status
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    CONSTRAINT ck_grade_change_requests_reason CHECK (btrim(reason) <> ''),
    CONSTRAINT ck_grade_change_requests_new_score
        CHECK (new_score IS NULL OR new_score BETWEEN 0.0 AND 10.0),
    CONSTRAINT ck_grade_change_requests_new_outcome
        CHECK (new_outcome IS NULL OR new_outcome IN ('ACHIEVED', 'NOT_ACHIEVED')),
    -- A request must propose exactly one kind of value, matching the subject's mode.
    CONSTRAINT ck_grade_change_requests_proposal CHECK (
        (new_score IS NOT NULL AND new_outcome IS NULL)
        OR (new_score IS NULL AND new_outcome IS NOT NULL)),
    -- A decision without a decider, or a decider without a decision, is a gap
    -- in the trail that approval exists to leave.
    CONSTRAINT ck_grade_change_requests_decision CHECK (
        (status = 'PENDING' AND decided_by IS NULL AND decided_at IS NULL)
        OR (status IN ('APPROVED', 'REJECTED') AND decided_by IS NOT NULL AND decided_at IS NOT NULL))
);

CREATE INDEX ix_grade_change_requests_pending
    ON grade_change_requests (created_at DESC)
    WHERE status = 'PENDING';
CREATE INDEX ix_grade_change_requests_assessment
    ON grade_change_requests (grade_assessment_id, created_at DESC);
CREATE INDEX ix_grade_columns_book_order ON grade_columns (grade_book_id, display_order);
CREATE INDEX ix_grade_assessments_column ON grade_assessments (grade_column_id);
