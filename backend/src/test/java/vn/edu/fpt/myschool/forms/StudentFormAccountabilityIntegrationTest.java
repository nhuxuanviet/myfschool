package vn.edu.fpt.myschool.forms;

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

/** V28: a form now records who raised it and who moved it. */
@Testcontainers
@ActiveProfiles({"test", "e2e"})
@SpringBootTest
@Transactional
class StudentFormAccountabilityIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRESQL =
            new PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine"));

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void attributesEveryExistingFormToTheStudentWhoRaisedIt() {
        Integer unattributed = jdbcTemplate.queryForObject(
                """
                SELECT count(*) FROM student_forms
                WHERE submitted_by IS NULL OR submitted_by_role IS NULL
                """,
                Integer.class);
        Integer total = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM student_forms", Integer.class);

        assertThat(total).isGreaterThan(0);
        assertThat(unattributed).isZero();
    }

    @Test
    void pointsEachFormAtTheAccountOfItsOwnStudent() {
        Integer mismatched = jdbcTemplate.queryForObject(
                """
                SELECT count(*) FROM student_forms form
                INNER JOIN students student ON student.id = form.student_id
                WHERE form.submitted_by IS DISTINCT FROM student.user_id
                """,
                Integer.class);
        assertThat(mismatched).isZero();
    }

    /** A guardian filing for their child is the reason submitter and subject are separate. */
    @Test
    void acceptsAFormRaisedByAGuardianRatherThanTheStudent() {
        UUID studentId = jdbcTemplate.queryForObject(
                "SELECT id FROM students LIMIT 1", UUID.class);
        UUID guardianUserId = jdbcTemplate.queryForObject(
                """
                SELECT id FROM users WHERE id NOT IN (SELECT user_id FROM students)
                LIMIT 1
                """,
                UUID.class);
        UUID formId = UUID.randomUUID();

        jdbcTemplate.update(
                """
                INSERT INTO student_forms (
                    id, student_id, form_type, reason, starts_on, ends_on, status,
                    submitted_by, submitted_by_role, submitted_at, updated_at
                ) VALUES (?, ?, 'LEAVE_OF_ABSENCE', 'Con bị sốt', CURRENT_DATE, CURRENT_DATE,
                          'SUBMITTED', ?, 'PARENT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                formId, studentId, guardianUserId);

        assertThat(jdbcTemplate.queryForObject(
                        "SELECT submitted_by_role FROM student_forms WHERE id = ?",
                        String.class, formId))
                .isEqualTo("PARENT");
    }

    @Test
    void rejectsASubmitterRoleThatCannotRaiseAForm() {
        UUID studentId = jdbcTemplate.queryForObject(
                "SELECT id FROM students LIMIT 1", UUID.class);
        UUID userId = jdbcTemplate.queryForObject("SELECT id FROM users LIMIT 1", UUID.class);

        // A teacher does not file a student's leave request on their own behalf.
        assertThatThrownBy(() -> jdbcTemplate.update(
                        """
                        INSERT INTO student_forms (
                            id, student_id, form_type, reason, starts_on, ends_on, status,
                            submitted_by, submitted_by_role, submitted_at, updated_at
                        ) VALUES (?, ?, 'LEAVE_OF_ABSENCE', 'Sai vai', CURRENT_DATE, CURRENT_DATE,
                                  'SUBMITTED', ?, 'TEACHER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                        UUID.randomUUID(), studentId, userId))
                .hasMessageContaining("ck_student_forms_submitted_by_role");
    }

    @Test
    void recordsWhoMovedTheFormAndInWhatCapacity() {
        UUID formId = jdbcTemplate.queryForObject(
                "SELECT id FROM student_forms LIMIT 1", UUID.class);
        UUID actorId = jdbcTemplate.queryForObject("SELECT id FROM users LIMIT 1", UUID.class);
        Integer nextSequence = jdbcTemplate.queryForObject(
                "SELECT COALESCE(max(sequence_number), 0) + 1 FROM student_form_status_history WHERE form_id = ?",
                Integer.class, formId);

        jdbcTemplate.update(
                """
                INSERT INTO student_form_status_history (
                    id, form_id, sequence_number, status, actor_user_id, actor_role,
                    occurred_at, note
                ) VALUES (?, ?, ?, 'APPROVED', ?, 'TEACHER', CURRENT_TIMESTAMP, 'GVCN duyệt')
                """,
                UUID.randomUUID(), formId, nextSequence, actorId);

        var row = jdbcTemplate.queryForMap(
                """
                SELECT actor_user_id, actor_role FROM student_form_status_history
                WHERE form_id = ? AND sequence_number = ?
                """,
                formId, nextSequence);
        assertThat(row.get("actor_user_id")).isEqualTo(actorId);
        assertThat(row.get("actor_role")).isEqualTo("TEACHER");
    }

    @Test
    void rejectsAnActorRoleOutsideTheFourRoles() {
        UUID formId = jdbcTemplate.queryForObject(
                "SELECT id FROM student_forms LIMIT 1", UUID.class);
        UUID actorId = jdbcTemplate.queryForObject("SELECT id FROM users LIMIT 1", UUID.class);

        assertThatThrownBy(() -> jdbcTemplate.update(
                        """
                        INSERT INTO student_form_status_history (
                            id, form_id, sequence_number, status, actor_user_id, actor_role,
                            occurred_at
                        ) VALUES (?, ?, 999, 'APPROVED', ?, 'PRINCIPAL', CURRENT_TIMESTAMP)
                        """,
                        UUID.randomUUID(), formId, actorId))
                .hasMessageContaining("ck_student_form_status_history_actor_role");
    }
}
