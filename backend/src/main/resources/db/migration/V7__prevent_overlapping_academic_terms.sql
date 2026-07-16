CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE academic_terms
    ADD CONSTRAINT ex_academic_terms_year_date_range
    EXCLUDE USING gist (
        academic_year_id WITH =,
        daterange(starts_on, ends_on, '[]') WITH &&
    );
