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

/** Verifies the V20 invariants for teacher, parent and guardian-link records. */
@Testcontainers
@ActiveProfiles({"test", "e2e"})
@SpringBootTest
@Transactional
class GuardianLinkMigrationIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRESQL =
            new PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine"));

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID insertTeacher(String code, String fullName) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO teacher_profiles
                    (id, user_id, teacher_code, full_name, enabled, version, created_at, updated_at)
                VALUES (?, NULL, ?, ?, TRUE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                id, code, fullName);
        return id;
    }

    private UUID insertParent(String fullName) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO parent_profiles
                    (id, user_id, full_name, enabled, version, created_at, updated_at)
                VALUES (?, NULL, ?, TRUE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                id, fullName);
        return id;
    }

    private void link(UUID parentId, UUID studentId, String relationship, int contactOrder) {
        jdbcTemplate.update(
                """
                INSERT INTO parent_student_links
                    (id, parent_id, student_id, relationship, contact_order,
                     effective_from, effective_to, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, CURRENT_DATE, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                UUID.randomUUID(), parentId, studentId, relationship, contactOrder);
    }

    private UUID anyStudentId() {
        return jdbcTemplate.queryForObject("SELECT id FROM students LIMIT 1", UUID.class);
    }

    @Test
    void keepsSeveralTeacherProfilesThatHaveNoAccountYet() {
        insertTeacher("GV001", "Nguyễn Thị Lan");
        insertTeacher("GV002", "Phạm Văn Hùng");

        // V24 derives profiles from the seeded timetable, so the table is never empty here.
        // Count only the two this test inserted.
        Integer unaccounted = jdbcTemplate.queryForObject(
                """
                SELECT count(*) FROM teacher_profiles
                WHERE user_id IS NULL AND teacher_code IN ('GV001', 'GV002')
                """,
                Integer.class);
        assertThat(unaccounted).isEqualTo(2);
    }

    @Test
    void rejectsTwoTeachersSharingACode() {
        insertTeacher("GV010", "Trần Văn A");

        assertThatThrownBy(() -> insertTeacher("GV010", "Lê Thị B"))
                .hasMessageContaining("uk_teacher_profiles_code");
    }

    @Test
    void letsOneStudentHaveSeveralGuardians() {
        UUID studentId = anyStudentId();
        link(insertParent("Nguyễn Văn Cha"), studentId, "FATHER", 1);
        link(insertParent("Trần Thị Mẹ"), studentId, "MOTHER", 2);

        Integer guardians = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM parent_student_links WHERE student_id = ?",
                Integer.class,
                studentId);
        assertThat(guardians).isEqualTo(2);
    }

    @Test
    void letsOneParentLinkToSeveralStudents() {
        UUID parentId = insertParent("Nguyễn Văn Hai Con");
        java.util.List<UUID> students = jdbcTemplate.queryForList(
                "SELECT id FROM students LIMIT 2", UUID.class);
        assertThat(students).hasSize(2);

        link(parentId, students.get(0), "FATHER", 1);
        link(parentId, students.get(1), "FATHER", 1);

        Integer links = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM parent_student_links WHERE parent_id = ?",
                Integer.class,
                parentId);
        assertThat(links).isEqualTo(2);
    }

    @Test
    void rejectsASecondLinkInForceForTheSameParentAndStudent() {
        UUID parentId = insertParent("Nguyễn Văn A");
        UUID studentId = anyStudentId();
        link(parentId, studentId, "FATHER", 1);

        assertThatThrownBy(() -> link(parentId, studentId, "GUARDIAN", 2))
                .hasMessageContaining("uk_parent_student_links_active");
    }

    @Test
    void allowsRelinkingAfterTheEarlierLinkHasEnded() {
        UUID parentId = insertParent("Nguyễn Văn Quay Lại");
        UUID studentId = anyStudentId();
        link(parentId, studentId, "GUARDIAN", 1);
        jdbcTemplate.update(
                """
                UPDATE parent_student_links SET effective_to = CURRENT_DATE + 1
                WHERE parent_id = ? AND student_id = ?
                """,
                parentId, studentId);

        link(parentId, studentId, "FATHER", 1);

        Integer inForce = jdbcTemplate.queryForObject(
                """
                SELECT count(*) FROM parent_student_links
                WHERE parent_id = ? AND student_id = ? AND effective_to IS NULL
                """,
                Integer.class,
                parentId,
                studentId);
        assertThat(inForce).isEqualTo(1);
    }

    @Test
    void rejectsAnUnknownRelationship() {
        UUID parentId = insertParent("Trần Thị B");

        assertThatThrownBy(() -> link(parentId, anyStudentId(), "UNCLE", 1))
                .hasMessageContaining("ck_parent_student_links_relationship");
    }

    @Test
    void rejectsALinkThatEndsBeforeItStarts() {
        UUID parentId = insertParent("Lê Văn C");

        assertThatThrownBy(() -> jdbcTemplate.update(
                        """
                        INSERT INTO parent_student_links
                            (id, parent_id, student_id, relationship, contact_order,
                             effective_from, effective_to, created_at, updated_at)
                        VALUES (?, ?, ?, 'FATHER', 1, CURRENT_DATE, CURRENT_DATE - 1,
                                CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                        UUID.randomUUID(), parentId, anyStudentId()))
                .hasMessageContaining("ck_parent_student_links_effective_range");
    }
}
