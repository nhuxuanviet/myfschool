package vn.edu.fpt.myschool.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/** Verifies that V19 makes user_roles a faithful copy of the legacy users.role column. */
@Testcontainers
@ActiveProfiles({"test", "e2e"})
@SpringBootTest
class UserRolesMigrationIntegrationTest {

    private static final String SEEDED_STUDENT_PHONE = "0912345678";
    private static final String SEEDED_ADMIN_PHONE = "0900000000";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRESQL =
            new PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine"));

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private List<String> rolesOf(String phoneNumber) {
        return jdbcTemplate.queryForList(
                """
                SELECT ur.role FROM user_roles ur
                JOIN users u ON u.id = ur.user_id
                WHERE u.phone_number = ?
                """,
                String.class,
                phoneNumber);
    }

    @Test
    void copiesExactlyOneRoleRowForEveryExistingUser() {
        Integer users = jdbcTemplate.queryForObject("SELECT count(*) FROM users", Integer.class);
        Integer roles = jdbcTemplate.queryForObject("SELECT count(*) FROM user_roles", Integer.class);
        assertThat(roles).isEqualTo(users);
    }

    @Test
    void migratesTheSeededStudentAndAdminToTheirOwnRole() {
        assertThat(rolesOf(SEEDED_STUDENT_PHONE)).containsExactly("STUDENT");
        assertThat(rolesOf(SEEDED_ADMIN_PHONE)).containsExactly("ADMIN");
    }

    @Test
    void agreesWithTheLegacyColumnItReplaces() {
        Integer mismatches = jdbcTemplate.queryForObject(
                """
                SELECT count(*) FROM users u
                LEFT JOIN user_roles ur ON ur.user_id = u.id AND ur.role = u.role
                WHERE ur.user_id IS NULL
                """,
                Integer.class);
        assertThat(mismatches).isZero();
    }

    @Test
    void acceptsTheTwoRolesIntroducedForThisRelease() {
        jdbcTemplate.update(
                """
                INSERT INTO user_roles (user_id, role, created_at)
                SELECT id, 'TEACHER', CURRENT_TIMESTAMP FROM users
                WHERE phone_number = ?
                """,
                SEEDED_ADMIN_PHONE);
        try {
            assertThat(rolesOf(SEEDED_ADMIN_PHONE)).containsExactlyInAnyOrder("ADMIN", "TEACHER");
        } finally {
            // The container is shared by every test in this class, so a leaked row
            // would silently corrupt the counts the other tests assert on.
            jdbcTemplate.update("DELETE FROM user_roles WHERE role = 'TEACHER'");
        }
    }

    @Test
    void rejectsARoleOutsideTheAllowedSet() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                        """
                        INSERT INTO user_roles (user_id, role, created_at)
                        SELECT id, 'PRINCIPAL', CURRENT_TIMESTAMP FROM users
                        WHERE phone_number = ?
                        """,
                        SEEDED_STUDENT_PHONE))
                .hasMessageContaining("ck_user_roles_role");
    }
}
