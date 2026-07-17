package vn.edu.fpt.myschool.teacher.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import vn.edu.fpt.myschool.teacher.application.port.TeacherStore;
import vn.edu.fpt.myschool.teacher.domain.TeacherWorkload;

@Repository
class JdbcTeacherStore implements TeacherStore {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    JdbcTeacherStore(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<UUID> findTeacherIdByUserId(UUID userId) {
        return jdbcTemplate.query(
                        """
                        SELECT id FROM teacher_profiles
                        WHERE user_id = :userId AND enabled = TRUE
                        """,
                        new MapSqlParameterSource("userId", userId),
                        (rs, rowNum) -> rs.getObject("id", UUID.class))
                .stream()
                .findFirst();
    }

    @Override
    public List<TeacherWorkload.AssignedClass> findAssignedClasses(
            UUID teacherId, UUID academicTermId) {
        return jdbcTemplate.query(
                """
                SELECT assignment.id AS assignment_id,
                       school_class.id AS class_id,
                       school_class.code AS class_code,
                       school_class.grade_level,
                       subject.id AS subject_id,
                       subject.code AS subject_code,
                       subject.name AS subject_name,
                       term.id AS academic_term_id,
                       term.code AS academic_term_code,
                       (SELECT count(*) FROM students student
                        WHERE student.class_id = school_class.id) AS student_count,
                       EXISTS (SELECT 1 FROM homeroom_assignments homeroom
                               WHERE homeroom.teacher_id = assignment.teacher_id
                                 AND homeroom.class_id = school_class.id) AS homeroom
                FROM teacher_subject_assignments assignment
                INNER JOIN school_classes school_class ON school_class.id = assignment.class_id
                INNER JOIN subjects subject ON subject.id = assignment.subject_id
                INNER JOIN academic_terms term ON term.id = assignment.academic_term_id
                WHERE assignment.teacher_id = :teacherId
                  AND (CAST(:academicTermId AS UUID) IS NULL
                       OR assignment.academic_term_id = :academicTermId)
                ORDER BY term.starts_on DESC, school_class.code, subject.name
                """,
                new MapSqlParameterSource("teacherId", teacherId)
                        .addValue("academicTermId", academicTermId),
                JdbcTeacherStore::mapAssignedClass);
    }

    @Override
    public List<TeacherWorkload.HomeroomClass> findHomeroomClasses(UUID teacherId) {
        return jdbcTemplate.query(
                """
                SELECT school_class.id AS class_id,
                       school_class.code AS class_code,
                       school_class.grade_level,
                       homeroom.academic_year_id,
                       (SELECT count(*) FROM students student
                        WHERE student.class_id = school_class.id) AS student_count
                FROM homeroom_assignments homeroom
                INNER JOIN school_classes school_class ON school_class.id = homeroom.class_id
                WHERE homeroom.teacher_id = :teacherId
                ORDER BY school_class.code
                """,
                new MapSqlParameterSource("teacherId", teacherId),
                (rs, rowNum) -> new TeacherWorkload.HomeroomClass(
                        rs.getObject("class_id", UUID.class),
                        rs.getString("class_code"),
                        rs.getInt("grade_level"),
                        rs.getObject("academic_year_id", UUID.class),
                        rs.getInt("student_count")));
    }

    /**
     * The teaching week is derived from the assignments, not from the class timetable alone:
     * a lesson only belongs to this teacher if an assignment says the subject in that class is
     * theirs for that term.
     */
    @Override
    public TeacherWorkload.TeachingWeek findTeachingWeek(UUID teacherId, LocalDate weekStart) {
        List<TeacherWorkload.ScheduledLesson> lessons = jdbcTemplate.query(
                """
                SELECT CAST(:weekStart AS date) + (entry.day_of_week - 1) AS lesson_date,
                       entry.session,
                       entry.period_number,
                       period_definition.start_time,
                       period_definition.end_time,
                       school_class.code AS class_code,
                       subject.code AS subject_code,
                       subject.name AS subject_name,
                       entry.room,
                       COALESCE(override.override_type, 'SCHEDULED') AS status
                FROM class_timetable_entries entry
                INNER JOIN teacher_subject_assignments assignment
                    ON assignment.class_id = entry.school_class_id
                    AND assignment.subject_id = entry.subject_id
                    AND assignment.academic_term_id = entry.academic_term_id
                INNER JOIN school_classes school_class ON school_class.id = entry.school_class_id
                INNER JOIN subjects subject ON subject.id = entry.subject_id
                INNER JOIN academic_terms term ON term.id = entry.academic_term_id
                INNER JOIN term_period_definitions period_definition
                    ON period_definition.academic_term_id = entry.academic_term_id
                    AND period_definition.session = entry.session
                    AND period_definition.period_number = entry.period_number
                LEFT JOIN timetable_overrides override
                    ON override.academic_term_id = entry.academic_term_id
                    AND override.school_class_id = entry.school_class_id
                    AND override.lesson_date = CAST(:weekStart AS date) + (entry.day_of_week - 1)
                    AND override.session = entry.session
                    AND override.period_number = entry.period_number
                WHERE assignment.teacher_id = :teacherId
                  AND CAST(:weekStart AS date) + (entry.day_of_week - 1)
                      BETWEEN term.starts_on AND term.ends_on
                ORDER BY lesson_date,
                         CASE entry.session WHEN 'MORNING' THEN 0 ELSE 1 END,
                         entry.period_number
                """,
                new MapSqlParameterSource("teacherId", teacherId)
                        .addValue("weekStart", weekStart),
                (rs, rowNum) -> new TeacherWorkload.ScheduledLesson(
                        rs.getDate("lesson_date").toLocalDate(),
                        rs.getString("session"),
                        rs.getInt("period_number"),
                        rs.getTime("start_time").toLocalTime(),
                        rs.getTime("end_time").toLocalTime(),
                        rs.getString("class_code"),
                        rs.getString("subject_code"),
                        rs.getString("subject_name"),
                        rs.getString("room"),
                        rs.getString("status")));
        return new TeacherWorkload.TeachingWeek(weekStart, lessons);
    }

    @Override
    public boolean isAssignedToClass(UUID teacherId, UUID classId, UUID academicTermId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT count(*) FROM teacher_subject_assignments
                WHERE teacher_id = :teacherId
                  AND class_id = :classId
                  AND (CAST(:academicTermId AS UUID) IS NULL
                       OR academic_term_id = :academicTermId)
                """,
                new MapSqlParameterSource("teacherId", teacherId)
                        .addValue("classId", classId)
                        .addValue("academicTermId", academicTermId),
                Integer.class);
        return count != null && count > 0;
    }

    @Override
    public boolean isAssignedToSubject(
            UUID teacherId, UUID classId, UUID subjectId, UUID academicTermId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT count(*) FROM teacher_subject_assignments
                WHERE teacher_id = :teacherId
                  AND class_id = :classId
                  AND subject_id = :subjectId
                  AND academic_term_id = :academicTermId
                """,
                new MapSqlParameterSource("teacherId", teacherId)
                        .addValue("classId", classId)
                        .addValue("subjectId", subjectId)
                        .addValue("academicTermId", academicTermId),
                Integer.class);
        return count != null && count > 0;
    }

    @Override
    public boolean isHomeroomTeacherOf(UUID teacherId, UUID classId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT count(*) FROM homeroom_assignments
                WHERE teacher_id = :teacherId AND class_id = :classId
                """,
                new MapSqlParameterSource("teacherId", teacherId).addValue("classId", classId),
                Integer.class);
        return count != null && count > 0;
    }

    @Override
    public List<TeacherWorkload.ClassStudent> findClassStudents(UUID classId) {
        return jdbcTemplate.query(
                """
                SELECT student.id AS student_id, student.student_code,
                       student.full_name, student.grade_level
                FROM students student
                WHERE student.class_id = :classId
                ORDER BY student.full_name, student.id
                """,
                new MapSqlParameterSource("classId", classId),
                (rs, rowNum) -> new TeacherWorkload.ClassStudent(
                        rs.getObject("student_id", UUID.class),
                        rs.getString("student_code"),
                        rs.getString("full_name"),
                        rs.getInt("grade_level")));
    }

    private static TeacherWorkload.AssignedClass mapAssignedClass(ResultSet rs, int rowNum)
            throws SQLException {
        return new TeacherWorkload.AssignedClass(
                rs.getObject("assignment_id", UUID.class),
                rs.getObject("class_id", UUID.class),
                rs.getString("class_code"),
                rs.getInt("grade_level"),
                rs.getObject("subject_id", UUID.class),
                rs.getString("subject_code"),
                rs.getString("subject_name"),
                rs.getObject("academic_term_id", UUID.class),
                rs.getString("academic_term_code"),
                rs.getInt("student_count"),
                rs.getBoolean("homeroom"));
    }
}
