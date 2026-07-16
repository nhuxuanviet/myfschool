CREATE TABLE student_forms (
    id UUID PRIMARY KEY,
    student_id UUID NOT NULL,
    form_type VARCHAR(32) NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    starts_on DATE,
    ends_on DATE,
    status VARCHAR(16) NOT NULL,
    submitted_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_student_forms_student
        FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE CASCADE,
    CONSTRAINT ck_student_forms_type
        CHECK (form_type IN (
            'LEAVE_OF_ABSENCE',
            'STUDENT_CONFIRMATION',
            'TRANSCRIPT_REQUEST',
            'STUDENT_CARD_REISSUE'
        )),
    CONSTRAINT ck_student_forms_reason CHECK (btrim(reason) <> ''),
    CONSTRAINT ck_student_forms_status
        CHECK (status IN ('SUBMITTED', 'IN_REVIEW', 'APPROVED', 'REJECTED', 'CANCELLED')),
    CONSTRAINT ck_student_forms_dates
        CHECK (
            (form_type = 'LEAVE_OF_ABSENCE'
                AND starts_on IS NOT NULL
                AND ends_on IS NOT NULL
                AND ends_on >= starts_on)
            OR
            (form_type <> 'LEAVE_OF_ABSENCE'
                AND starts_on IS NULL
                AND ends_on IS NULL)
        )
);

CREATE INDEX ix_student_forms_student_status_submitted
    ON student_forms (student_id, status, submitted_at DESC, id DESC);

CREATE TABLE student_form_status_history (
    id UUID PRIMARY KEY,
    form_id UUID NOT NULL,
    sequence_number INTEGER NOT NULL,
    status VARCHAR(16) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    note VARCHAR(500),
    CONSTRAINT fk_student_form_status_history_form
        FOREIGN KEY (form_id) REFERENCES student_forms (id) ON DELETE CASCADE,
    CONSTRAINT uk_student_form_status_history_sequence
        UNIQUE (form_id, sequence_number),
    CONSTRAINT ck_student_form_status_history_sequence
        CHECK (sequence_number > 0),
    CONSTRAINT ck_student_form_status_history_status
        CHECK (status IN ('SUBMITTED', 'IN_REVIEW', 'APPROVED', 'REJECTED', 'CANCELLED')),
    CONSTRAINT ck_student_form_status_history_note
        CHECK (note IS NULL OR btrim(note) <> '')
);
