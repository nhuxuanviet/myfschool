CREATE TABLE school_clubs (
    id UUID PRIMARY KEY,
    category VARCHAR(16) NOT NULL,
    name VARCHAR(160) NOT NULL,
    description TEXT NOT NULL,
    advisor_name VARCHAR(120) NOT NULL,
    meeting_schedule VARCHAR(200) NOT NULL,
    location VARCHAR(160) NOT NULL,
    audience VARCHAR(16) NOT NULL,
    audience_grade_level SMALLINT,
    capacity INTEGER,
    application_deadline TIMESTAMPTZ,
    accepting_applications BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_school_clubs_category
        CHECK (category IN ('ACADEMIC', 'SPORTS', 'ARTS', 'SKILLS', 'COMMUNITY', 'MEDIA')),
    CONSTRAINT ck_school_clubs_name CHECK (btrim(name) <> ''),
    CONSTRAINT ck_school_clubs_description CHECK (btrim(description) <> ''),
    CONSTRAINT ck_school_clubs_advisor CHECK (btrim(advisor_name) <> ''),
    CONSTRAINT ck_school_clubs_schedule CHECK (btrim(meeting_schedule) <> ''),
    CONSTRAINT ck_school_clubs_location CHECK (btrim(location) <> ''),
    CONSTRAINT ck_school_clubs_audience CHECK (audience IN ('ALL', 'GRADE')),
    CONSTRAINT ck_school_clubs_audience_target
        CHECK (
            (audience = 'ALL' AND audience_grade_level IS NULL)
            OR
            (audience = 'GRADE' AND audience_grade_level BETWEEN 6 AND 12)
        ),
    CONSTRAINT ck_school_clubs_capacity CHECK (capacity IS NULL OR capacity > 0)
);

CREATE INDEX ix_school_clubs_visibility_category
    ON school_clubs (audience, audience_grade_level, category, name);

CREATE TABLE student_club_memberships (
    id UUID PRIMARY KEY,
    club_id UUID NOT NULL,
    student_id UUID NOT NULL,
    status VARCHAR(16) NOT NULL,
    applied_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_student_club_memberships_club_student UNIQUE (club_id, student_id),
    CONSTRAINT fk_student_club_memberships_club
        FOREIGN KEY (club_id) REFERENCES school_clubs (id) ON DELETE CASCADE,
    CONSTRAINT fk_student_club_memberships_student
        FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE CASCADE,
    CONSTRAINT ck_student_club_memberships_status
        CHECK (status IN ('PENDING', 'ACTIVE', 'REJECTED', 'WITHDRAWN'))
);

CREATE INDEX ix_student_club_memberships_club_status
    ON student_club_memberships (club_id, status);

CREATE INDEX ix_student_club_memberships_student_status
    ON student_club_memberships (student_id, status);
