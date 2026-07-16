CREATE TABLE school_events (
    id UUID PRIMARY KEY,
    category VARCHAR(16) NOT NULL,
    title VARCHAR(160) NOT NULL,
    description TEXT NOT NULL,
    location VARCHAR(160) NOT NULL,
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL,
    audience VARCHAR(16) NOT NULL,
    audience_grade_level SMALLINT,
    capacity INTEGER,
    registration_deadline TIMESTAMPTZ,
    cancellation_deadline TIMESTAMPTZ,
    registration_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_school_events_category
        CHECK (category IN ('ACADEMIC', 'CULTURAL', 'SPORTS', 'CLUB', 'CAREER')),
    CONSTRAINT ck_school_events_title CHECK (btrim(title) <> ''),
    CONSTRAINT ck_school_events_description CHECK (btrim(description) <> ''),
    CONSTRAINT ck_school_events_location CHECK (btrim(location) <> ''),
    CONSTRAINT ck_school_events_dates CHECK (ends_at > starts_at),
    CONSTRAINT ck_school_events_audience
        CHECK (audience IN ('ALL', 'GRADE')),
    CONSTRAINT ck_school_events_audience_target
        CHECK (
            (audience = 'ALL' AND audience_grade_level IS NULL)
            OR
            (audience = 'GRADE' AND audience_grade_level BETWEEN 6 AND 12)
        ),
    CONSTRAINT ck_school_events_capacity CHECK (capacity IS NULL OR capacity > 0),
    CONSTRAINT ck_school_events_registration_deadline
        CHECK (registration_deadline IS NULL OR registration_deadline <= starts_at),
    CONSTRAINT ck_school_events_cancellation_deadline
        CHECK (cancellation_deadline IS NULL OR cancellation_deadline <= starts_at)
);

CREATE INDEX ix_school_events_visibility_schedule
    ON school_events (audience, audience_grade_level, starts_at, ends_at);

CREATE INDEX ix_school_events_category_schedule
    ON school_events (category, starts_at, ends_at);

CREATE TABLE student_event_registrations (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL,
    student_id UUID NOT NULL,
    status VARCHAR(16) NOT NULL,
    registered_at TIMESTAMPTZ NOT NULL,
    cancelled_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_student_event_registrations_event_student
        UNIQUE (event_id, student_id),
    CONSTRAINT fk_student_event_registrations_event
        FOREIGN KEY (event_id) REFERENCES school_events (id) ON DELETE CASCADE,
    CONSTRAINT fk_student_event_registrations_student
        FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE CASCADE,
    CONSTRAINT ck_student_event_registrations_status
        CHECK (status IN ('REGISTERED', 'CANCELLED')),
    CONSTRAINT ck_student_event_registrations_status_timestamps
        CHECK (
            (status = 'REGISTERED' AND cancelled_at IS NULL)
            OR
            (status = 'CANCELLED' AND cancelled_at >= registered_at)
        )
);

CREATE INDEX ix_student_event_registrations_event_status
    ON student_event_registrations (event_id, status);
