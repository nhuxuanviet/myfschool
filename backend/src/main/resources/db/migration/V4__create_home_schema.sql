CREATE TABLE academic_years (
    id UUID PRIMARY KEY,
    code VARCHAR(16) NOT NULL,
    starts_on DATE NOT NULL,
    ends_on DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_academic_years_code UNIQUE (code),
    CONSTRAINT ck_academic_years_code CHECK (btrim(code) <> ''),
    CONSTRAINT ck_academic_years_dates CHECK (ends_on > starts_on)
);

CREATE TABLE academic_terms (
    id UUID PRIMARY KEY,
    academic_year_id UUID NOT NULL,
    code VARCHAR(16) NOT NULL,
    name VARCHAR(80) NOT NULL,
    starts_on DATE NOT NULL,
    ends_on DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_academic_terms_year_code UNIQUE (academic_year_id, code),
    CONSTRAINT fk_academic_terms_academic_year
        FOREIGN KEY (academic_year_id) REFERENCES academic_years (id) ON DELETE RESTRICT,
    CONSTRAINT ck_academic_terms_code CHECK (btrim(code) <> ''),
    CONSTRAINT ck_academic_terms_name CHECK (btrim(name) <> ''),
    CONSTRAINT ck_academic_terms_dates CHECK (ends_on > starts_on)
);

CREATE INDEX ix_academic_terms_active_dates
    ON academic_terms (starts_on, ends_on);

CREATE TABLE announcements (
    id UUID PRIMARY KEY,
    title VARCHAR(160) NOT NULL,
    body TEXT NOT NULL,
    audience VARCHAR(16) NOT NULL,
    audience_grade_level SMALLINT,
    published_at TIMESTAMPTZ NOT NULL,
    visible_from TIMESTAMPTZ NOT NULL,
    visible_until TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_announcements_title CHECK (btrim(title) <> ''),
    CONSTRAINT ck_announcements_body CHECK (btrim(body) <> ''),
    CONSTRAINT ck_announcements_audience CHECK (audience IN ('ALL', 'GRADE')),
    CONSTRAINT ck_announcements_audience_target CHECK (
        (audience = 'ALL' AND audience_grade_level IS NULL)
        OR
        (audience = 'GRADE' AND audience_grade_level BETWEEN 6 AND 12)
    ),
    CONSTRAINT ck_announcements_visibility CHECK (
        visible_until IS NULL OR (
            visible_until > visible_from AND published_at < visible_until
        )
    )
);

CREATE INDEX ix_announcements_audience_visibility
    ON announcements (
        audience,
        audience_grade_level,
        visible_from,
        visible_until,
        published_at DESC
    );
