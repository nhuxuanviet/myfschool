-- A free-text teacher name cannot carry authorisation: two teachers sharing a
-- name would see each other's classes (spec §5.3). This turns the name into a
-- foreign key so the timetable points at a person, not at a label.

ALTER TABLE class_timetable_entries ADD COLUMN teacher_id UUID;
ALTER TABLE timetable_overrides ADD COLUMN teacher_id UUID;

-- Every distinct name across both timetable tables becomes a teacher profile
-- without an account, which is exactly what §5.1 allows: the roster exists
-- before the credentials do.
--
-- Known and accepted loss: two real teachers who share a name collapse into one
-- profile, because the old data holds nothing that tells them apart. There is no
-- automatic fix; an administrator splits them by hand afterwards (spec §12).
WITH distinct_names AS (
    SELECT DISTINCT btrim(teacher_name) AS full_name
    FROM (
        SELECT teacher_name FROM class_timetable_entries
        UNION
        SELECT teacher_name FROM timetable_overrides
    ) AS names
    WHERE teacher_name IS NOT NULL AND btrim(teacher_name) <> ''
), numbered AS (
    SELECT full_name, row_number() OVER (ORDER BY full_name) AS position
    FROM distinct_names
)
INSERT INTO teacher_profiles (
    id, user_id, teacher_code, full_name, email, enabled, version, created_at, updated_at
)
SELECT
    md5('timetable-teacher:' || full_name)::uuid,
    NULL,
    'TKB' || lpad(position::text, 4, '0'),
    full_name,
    NULL,
    TRUE,
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM numbered
ON CONFLICT (teacher_code) DO NOTHING;

UPDATE class_timetable_entries entry
SET teacher_id = teacher.id
FROM teacher_profiles teacher
WHERE teacher.full_name = btrim(entry.teacher_name)
  AND entry.teacher_name IS NOT NULL;

UPDATE timetable_overrides override
SET teacher_id = teacher.id
FROM teacher_profiles teacher
WHERE teacher.full_name = btrim(override.teacher_name)
  AND override.teacher_name IS NOT NULL;

-- teacher_id stays nullable because teacher_name was: a slot may legitimately
-- have no teacher named yet. Forcing NOT NULL here would mean inventing a
-- teacher for those rows, which is worse than admitting the gap.
ALTER TABLE class_timetable_entries
    ADD CONSTRAINT fk_class_timetable_entries_teacher FOREIGN KEY (teacher_id)
    REFERENCES teacher_profiles (id) ON DELETE RESTRICT;
ALTER TABLE timetable_overrides
    ADD CONSTRAINT fk_timetable_overrides_teacher FOREIGN KEY (teacher_id)
    REFERENCES teacher_profiles (id) ON DELETE RESTRICT;

ALTER TABLE class_timetable_entries DROP CONSTRAINT ck_class_timetable_entries_teacher_name;
ALTER TABLE timetable_overrides DROP CONSTRAINT ck_timetable_overrides_teacher_name;
ALTER TABLE class_timetable_entries DROP COLUMN teacher_name;
ALTER TABLE timetable_overrides DROP COLUMN teacher_name;

CREATE INDEX ix_class_timetable_entries_teacher
    ON class_timetable_entries (teacher_id)
    WHERE teacher_id IS NOT NULL;
