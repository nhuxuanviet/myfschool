-- students.class_name and students.class_id have both described a student's class
-- since V14. Two answers to one question is how they drift apart, and class_id is
-- the one that carries a relationship (spec §5.3).

-- Any student still without a class_id gets one derived from the name they carry,
-- creating the class under the latest academic year if it is missing. On a fresh
-- database this matches nothing, because the table is empty until the seeders run.
WITH latest_year AS (
    SELECT id FROM academic_years ORDER BY ends_on DESC, id LIMIT 1
), missing_classes AS (
    SELECT DISTINCT student.class_name, student.grade_level, year.id AS academic_year_id
    FROM students student
    CROSS JOIN latest_year year
    WHERE student.class_id IS NULL
)
INSERT INTO school_classes (
    id, academic_year_id, code, name, grade_level, enabled, version, created_at, updated_at
)
SELECT
    md5(missing.academic_year_id::text || ':' || missing.class_name)::uuid,
    missing.academic_year_id,
    missing.class_name,
    missing.class_name,
    missing.grade_level,
    TRUE,
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM missing_classes missing
ON CONFLICT (academic_year_id, code) DO NOTHING;

UPDATE students student
SET class_id = school_class.id
FROM school_classes school_class
WHERE student.class_id IS NULL
  AND student.class_name = school_class.code
  AND student.grade_level = school_class.grade_level;

ALTER TABLE students DROP COLUMN class_name;
