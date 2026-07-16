package vn.edu.fpt.myschool.timetable.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import vn.edu.fpt.myschool.home.domain.AcademicTerm;
import vn.edu.fpt.myschool.timetable.application.port.TimetableStore;
import vn.edu.fpt.myschool.timetable.domain.SchoolSession;
import vn.edu.fpt.myschool.timetable.domain.TimetableLesson;
import vn.edu.fpt.myschool.timetable.domain.TimetableLessonOccurrence;
import vn.edu.fpt.myschool.timetable.domain.TimetableLessonStatus;
import vn.edu.fpt.myschool.timetable.domain.TimetableSubject;
import vn.edu.fpt.myschool.timetable.domain.TimetableTerm;

@Repository
class JdbcTimetableStore implements TimetableStore {

    private static final String OVERLAPPING_TERMS_SQL = """
            SELECT academic_terms.id,
                   academic_years.code AS academic_year,
                   academic_terms.code,
                   academic_terms.name,
                   academic_terms.starts_on,
                   academic_terms.ends_on
            FROM academic_terms
            INNER JOIN academic_years ON academic_years.id = academic_terms.academic_year_id
            WHERE academic_terms.starts_on <= :weekEnd
              AND academic_terms.ends_on >= :weekStart
            ORDER BY academic_terms.starts_on ASC,
                     academic_terms.ends_on ASC,
                     academic_terms.code ASC,
                     academic_years.code ASC,
                     academic_terms.id ASC
            """;

    private static final String LESSONS_SQL = """
            WITH regular_lessons AS (
                SELECT
                    CAST(:weekStart AS date) + (entry.day_of_week - 1) AS lesson_date,
                    entry.session,
                    entry.period_number,
                    period_definition.start_time,
                    period_definition.end_time,
                    subject.code AS regular_subject_code,
                    subject.name AS regular_subject_name,
                    regular_teacher.full_name AS regular_teacher_name,
                    entry.room AS regular_room
                FROM class_timetable_entries entry
                INNER JOIN academic_terms academic_term ON academic_term.id = entry.academic_term_id
                INNER JOIN term_period_definitions period_definition
                    ON period_definition.academic_term_id = entry.academic_term_id
                    AND period_definition.session = entry.session
                    AND period_definition.period_number = entry.period_number
                INNER JOIN subjects subject ON subject.id = entry.subject_id
                -- LEFT JOIN: a slot may have no teacher named yet, and that must not
                -- make the lesson itself disappear from the student's timetable.
                LEFT JOIN teacher_profiles regular_teacher ON regular_teacher.id = entry.teacher_id
                WHERE entry.academic_term_id = :academicTermId
                  AND entry.class_name = :className
                  AND CAST(:weekStart AS date) + (entry.day_of_week - 1)
                      BETWEEN academic_term.starts_on AND academic_term.ends_on
            ),
            regular_with_overrides AS (
                SELECT
                    regular.lesson_date,
                    regular.session,
                    regular.period_number,
                    regular.start_time,
                    regular.end_time,
                    CASE WHEN override.override_type = 'REPLACED'
                        THEN override_subject.code
                        ELSE regular.regular_subject_code
                    END AS subject_code,
                    CASE WHEN override.override_type = 'REPLACED'
                        THEN override_subject.name
                        ELSE regular.regular_subject_name
                    END AS subject_name,
                    CASE WHEN override.override_type = 'REPLACED'
                        THEN override_teacher.full_name
                        ELSE regular.regular_teacher_name
                    END AS teacher_name,
                    CASE WHEN override.override_type = 'REPLACED'
                        THEN override.room
                        ELSE regular.regular_room
                    END AS room,
                    COALESCE(override.override_type, 'SCHEDULED') AS lesson_status,
                    override.note
                FROM regular_lessons regular
                LEFT JOIN timetable_overrides override
                    ON override.academic_term_id = :academicTermId
                    AND override.class_name = :className
                    AND override.lesson_date = regular.lesson_date
                    AND override.session = regular.session
                    AND override.period_number = regular.period_number
                    AND override.override_type IN ('CANCELLED', 'REPLACED')
                LEFT JOIN subjects override_subject ON override_subject.id = override.subject_id
                LEFT JOIN teacher_profiles override_teacher ON override_teacher.id = override.teacher_id
            ),
            added_without_regular_slot AS (
                SELECT
                    override.lesson_date,
                    override.session,
                    override.period_number,
                    period_definition.start_time,
                    period_definition.end_time,
                    subject.code AS subject_code,
                    subject.name AS subject_name,
                    added_teacher.full_name AS teacher_name,
                    override.room,
                    override.override_type AS lesson_status,
                    override.note
                FROM timetable_overrides override
                INNER JOIN academic_terms academic_term ON academic_term.id = override.academic_term_id
                INNER JOIN term_period_definitions period_definition
                    ON period_definition.academic_term_id = override.academic_term_id
                    AND period_definition.session = override.session
                    AND period_definition.period_number = override.period_number
                INNER JOIN subjects subject ON subject.id = override.subject_id
                LEFT JOIN teacher_profiles added_teacher ON added_teacher.id = override.teacher_id
                WHERE override.academic_term_id = :academicTermId
                  AND override.class_name = :className
                  AND override.lesson_date BETWEEN :weekStart AND :weekEnd
                  AND override.override_type = 'ADDED'
                  AND override.lesson_date BETWEEN academic_term.starts_on AND academic_term.ends_on
                  AND NOT EXISTS (
                      SELECT 1
                      FROM class_timetable_entries entry
                      WHERE entry.academic_term_id = override.academic_term_id
                        AND entry.class_name = override.class_name
                        AND entry.day_of_week = EXTRACT(ISODOW FROM override.lesson_date)::smallint
                        AND entry.session = override.session
                        AND entry.period_number = override.period_number
                  )
            ),
            effective_lessons AS (
                SELECT * FROM regular_with_overrides
                UNION ALL
                SELECT * FROM added_without_regular_slot
            )
            SELECT lesson_date,
                   session,
                   period_number,
                   start_time,
                   end_time,
                   subject_code,
                   subject_name,
                   teacher_name,
                   room,
                   lesson_status,
                   note
            FROM effective_lessons
            ORDER BY lesson_date,
                     CASE session WHEN 'MORNING' THEN 1 ELSE 2 END,
                     start_time,
                     period_number
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    JdbcTimetableStore(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<TimetableTerm> findAcademicTermsOverlapping(
            LocalDate weekStart,
            LocalDate weekEnd) {
        return jdbcTemplate.query(
                        OVERLAPPING_TERMS_SQL,
                        new MapSqlParameterSource()
                                .addValue("weekStart", java.sql.Date.valueOf(weekStart), Types.DATE)
                                .addValue("weekEnd", java.sql.Date.valueOf(weekEnd), Types.DATE),
                        this::mapTerm);
    }

    @Override
    public List<TimetableLessonOccurrence> findLessons(
            UUID academicTermId,
            String className,
            LocalDate weekStart,
            LocalDate weekEnd) {
        return jdbcTemplate.query(
                LESSONS_SQL,
                new MapSqlParameterSource()
                        .addValue("academicTermId", academicTermId)
                        .addValue("className", className)
                        .addValue("weekStart", java.sql.Date.valueOf(weekStart), Types.DATE)
                        .addValue("weekEnd", java.sql.Date.valueOf(weekEnd), Types.DATE),
                this::mapLessonOccurrence);
    }

    private TimetableTerm mapTerm(ResultSet resultSet, int rowNumber) throws SQLException {
        return new TimetableTerm(
                resultSet.getObject("id", UUID.class),
                new AcademicTerm(
                        resultSet.getString("academic_year"),
                        resultSet.getString("code"),
                        resultSet.getString("name"),
                        resultSet.getObject("starts_on", LocalDate.class),
                        resultSet.getObject("ends_on", LocalDate.class)));
    }

    private TimetableLessonOccurrence mapLessonOccurrence(ResultSet resultSet, int rowNumber)
            throws SQLException {
        return new TimetableLessonOccurrence(
                resultSet.getObject("lesson_date", LocalDate.class),
                new TimetableLesson(
                        SchoolSession.valueOf(resultSet.getString("session")),
                        resultSet.getInt("period_number"),
                        resultSet.getObject("start_time", LocalTime.class),
                        resultSet.getObject("end_time", LocalTime.class),
                        new TimetableSubject(
                                resultSet.getString("subject_code"),
                                resultSet.getString("subject_name")),
                        resultSet.getString("teacher_name"),
                        resultSet.getString("room"),
                        TimetableLessonStatus.valueOf(resultSet.getString("lesson_status")),
                        resultSet.getString("note")));
    }
}
