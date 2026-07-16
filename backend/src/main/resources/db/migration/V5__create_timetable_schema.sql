CREATE TABLE subjects (
    id UUID PRIMARY KEY,
    code VARCHAR(32) NOT NULL,
    name VARCHAR(120) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_subjects_code UNIQUE (code),
    CONSTRAINT ck_subjects_code CHECK (btrim(code) <> ''),
    CONSTRAINT ck_subjects_name CHECK (btrim(name) <> '')
);

CREATE TABLE term_period_definitions (
    id UUID PRIMARY KEY,
    academic_term_id UUID NOT NULL,
    session VARCHAR(16) NOT NULL,
    period_number SMALLINT NOT NULL,
    start_time TIME(0) NOT NULL,
    end_time TIME(0) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_term_period_definitions_slot
        UNIQUE (academic_term_id, session, period_number),
    CONSTRAINT fk_term_period_definitions_academic_term
        FOREIGN KEY (academic_term_id) REFERENCES academic_terms (id) ON DELETE RESTRICT,
    CONSTRAINT ck_term_period_definitions_session
        CHECK (session IN ('MORNING', 'AFTERNOON')),
    CONSTRAINT ck_term_period_definitions_period_number
        CHECK (period_number BETWEEN 1 AND 5),
    CONSTRAINT ck_term_period_definitions_duration
        CHECK (end_time - start_time = INTERVAL '45 minutes')
);

CREATE TABLE class_timetable_entries (
    id UUID PRIMARY KEY,
    academic_term_id UUID NOT NULL,
    class_name VARCHAR(32) NOT NULL,
    day_of_week SMALLINT NOT NULL,
    session VARCHAR(16) NOT NULL,
    period_number SMALLINT NOT NULL,
    subject_id UUID NOT NULL,
    teacher_name VARCHAR(120),
    room VARCHAR(32),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_class_timetable_entries_slot
        UNIQUE (academic_term_id, class_name, day_of_week, session, period_number),
    CONSTRAINT fk_class_timetable_entries_academic_term
        FOREIGN KEY (academic_term_id) REFERENCES academic_terms (id) ON DELETE RESTRICT,
    CONSTRAINT fk_class_timetable_entries_period_definition
        FOREIGN KEY (academic_term_id, session, period_number)
        REFERENCES term_period_definitions (academic_term_id, session, period_number)
        ON DELETE RESTRICT,
    CONSTRAINT fk_class_timetable_entries_subject
        FOREIGN KEY (subject_id) REFERENCES subjects (id) ON DELETE RESTRICT,
    CONSTRAINT ck_class_timetable_entries_class_name CHECK (btrim(class_name) <> ''),
    CONSTRAINT ck_class_timetable_entries_day_of_week CHECK (day_of_week BETWEEN 1 AND 7),
    CONSTRAINT ck_class_timetable_entries_session
        CHECK (session IN ('MORNING', 'AFTERNOON')),
    CONSTRAINT ck_class_timetable_entries_period_number
        CHECK (period_number BETWEEN 1 AND 5),
    CONSTRAINT ck_class_timetable_entries_teacher_name
        CHECK (teacher_name IS NULL OR btrim(teacher_name) <> ''),
    CONSTRAINT ck_class_timetable_entries_room
        CHECK (room IS NULL OR btrim(room) <> '')
);

CREATE INDEX ix_class_timetable_entries_lookup
    ON class_timetable_entries (academic_term_id, class_name, day_of_week, session, period_number);

CREATE TABLE timetable_overrides (
    id UUID PRIMARY KEY,
    academic_term_id UUID NOT NULL,
    class_name VARCHAR(32) NOT NULL,
    lesson_date DATE NOT NULL,
    session VARCHAR(16) NOT NULL,
    period_number SMALLINT NOT NULL,
    override_type VARCHAR(16) NOT NULL,
    subject_id UUID,
    teacher_name VARCHAR(120),
    room VARCHAR(32),
    note VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_timetable_overrides_slot
        UNIQUE (academic_term_id, class_name, lesson_date, session, period_number),
    CONSTRAINT fk_timetable_overrides_academic_term
        FOREIGN KEY (academic_term_id) REFERENCES academic_terms (id) ON DELETE RESTRICT,
    CONSTRAINT fk_timetable_overrides_period_definition
        FOREIGN KEY (academic_term_id, session, period_number)
        REFERENCES term_period_definitions (academic_term_id, session, period_number)
        ON DELETE RESTRICT,
    CONSTRAINT fk_timetable_overrides_subject
        FOREIGN KEY (subject_id) REFERENCES subjects (id) ON DELETE RESTRICT,
    CONSTRAINT ck_timetable_overrides_class_name CHECK (btrim(class_name) <> ''),
    CONSTRAINT ck_timetable_overrides_session
        CHECK (session IN ('MORNING', 'AFTERNOON')),
    CONSTRAINT ck_timetable_overrides_period_number
        CHECK (period_number BETWEEN 1 AND 5),
    CONSTRAINT ck_timetable_overrides_type
        CHECK (override_type IN ('CANCELLED', 'REPLACED', 'ADDED')),
    CONSTRAINT ck_timetable_overrides_subject
        CHECK (
            (override_type = 'CANCELLED' AND subject_id IS NULL)
            OR
            (override_type IN ('REPLACED', 'ADDED') AND subject_id IS NOT NULL)
        ),
    CONSTRAINT ck_timetable_overrides_teacher_name
        CHECK (teacher_name IS NULL OR btrim(teacher_name) <> ''),
    CONSTRAINT ck_timetable_overrides_room
        CHECK (room IS NULL OR btrim(room) <> ''),
    CONSTRAINT ck_timetable_overrides_note
        CHECK (note IS NULL OR btrim(note) <> '')
);

CREATE INDEX ix_timetable_overrides_lookup
    ON timetable_overrides (academic_term_id, class_name, lesson_date, session, period_number);
