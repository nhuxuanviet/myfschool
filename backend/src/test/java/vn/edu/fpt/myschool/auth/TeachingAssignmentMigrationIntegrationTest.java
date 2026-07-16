package vn.edu.fpt.myschool.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/** Verifies the V23 invariants that make relationship-based authorisation possible. */
@Testcontainers
@ActiveProfiles({"test", "e2e"})
@SpringBootTest
@Transactional
class TeachingAssignmentMigrationIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRESQL =
            new PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine"));

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID insertTeacher(String code) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO teacher_profiles
                    (id, user_id, teacher_code, full_name, enabled, version, created_at, updated_at)
                VALUES (?, NULL, ?, ?, TRUE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                id, code, "Giáo viên " + code);
        return id;
    }

    private UUID firstOf(String table) {
        return jdbcTemplate.queryForObject("SELECT id FROM " + table + " LIMIT 1", UUID.class);
    }

    private void assign(UUID teacherId, UUID classId, UUID subjectId, UUID termId) {
        jdbcTemplate.update(
                """
                INSERT INTO teacher_subject_assignments
                    (id, teacher_id, class_id, subject_id, academic_term_id, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                UUID.randomUUID(), teacherId, classId, subjectId, termId);
    }

    private void assignHomeroom(UUID teacherId, UUID classId, UUID yearId) {
        jdbcTemplate.update(
                """
                INSERT INTO homeroom_assignments
                    (id, teacher_id, class_id, academic_year_id, created_at, updated_at)
                VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                UUID.randomUUID(), teacherId, classId, yearId);
    }

    @Test
    void refusesTwoTeachersOnTheSameClassSubjectAndTerm() {
        UUID classId = firstOf("school_classes");
        UUID subjectId = firstOf("subjects");
        UUID termId = firstOf("academic_terms");
        assign(insertTeacher("GV501"), classId, subjectId, termId);

        assertThatThrownBy(() -> assign(insertTeacher("GV502"), classId, subjectId, termId))
                .hasMessageContaining("uk_teacher_subject_assignments_slot");
    }

    @Test
    void refusesTwoHomeroomTeachersForTheSameClassAndYear() {
        UUID classId = firstOf("school_classes");
        UUID yearId = firstOf("academic_years");
        assignHomeroom(insertTeacher("GV511"), classId, yearId);

        assertThatThrownBy(() -> assignHomeroom(insertTeacher("GV512"), classId, yearId))
                .hasMessageContaining("uk_homeroom_assignments_class_year");
    }

    /**
     * Homeroom duty and subject teaching are independent, exactly as the school works: one
     * person may be the homeroom teacher of one class while teaching a subject in another.
     */
    @Test
    void letsOneTeacherBeAHomeroomTeacherAndTeachElsewhere() {
        UUID teacherId = insertTeacher("GV521");
        UUID classId = firstOf("school_classes");
        assignHomeroom(teacherId, classId, firstOf("academic_years"));
        assign(teacherId, classId, firstOf("subjects"), firstOf("academic_terms"));

        Integer homeroom = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM homeroom_assignments WHERE teacher_id = ?",
                Integer.class, teacherId);
        Integer teaching = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM teacher_subject_assignments WHERE teacher_id = ?",
                Integer.class, teacherId);
        assertThat(homeroom).isEqualTo(1);
        assertThat(teaching).isEqualTo(1);
    }

    @Test
    void letsOneTeacherCoverSeveralSubjectsAndClasses() {
        UUID teacherId = insertTeacher("GV531");
        UUID termId = firstOf("academic_terms");
        var classes = jdbcTemplate.queryForList("SELECT id FROM school_classes LIMIT 2", UUID.class);
        var subjects = jdbcTemplate.queryForList("SELECT id FROM subjects LIMIT 2", UUID.class);
        assertThat(classes).hasSize(2);
        assertThat(subjects).hasSize(2);

        assign(teacherId, classes.get(0), subjects.get(0), termId);
        assign(teacherId, classes.get(1), subjects.get(0), termId);
        assign(teacherId, classes.get(0), subjects.get(1), termId);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM teacher_subject_assignments WHERE teacher_id = ?",
                Integer.class, teacherId);
        assertThat(count).isEqualTo(3);
    }

    /**
     * A teacher who still carries an assignment cannot be deleted out from under it, because
     * that would orphan the only record of who was responsible for a grade book.
     */
    @Test
    void refusesToDeleteATeacherWhoStillHoldsAnAssignment() {
        UUID teacherId = insertTeacher("GV541");
        assign(teacherId, firstOf("school_classes"), firstOf("subjects"), firstOf("academic_terms"));

        assertThatThrownBy(() ->
                        jdbcTemplate.update("DELETE FROM teacher_profiles WHERE id = ?", teacherId))
                .hasMessageContaining("fk_teacher_subject_assignments_teacher");
    }

    @Test
    void refusesAnAssignmentToATeacherThatDoesNotExist() {
        assertThatThrownBy(() -> assign(
                        UUID.randomUUID(),
                        firstOf("school_classes"),
                        firstOf("subjects"),
                        firstOf("academic_terms")))
                .hasMessageContaining("fk_teacher_subject_assignments_teacher");
    }
}
