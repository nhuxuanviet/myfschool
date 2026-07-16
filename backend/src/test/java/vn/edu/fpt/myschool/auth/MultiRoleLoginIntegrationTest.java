package vn.edu.fpt.myschool.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@ActiveProfiles({"test", "e2e"})
@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Transactional
class MultiRoleLoginIntegrationTest {

    private static final String STUDENT_PHONE = "0912345678";
    private static final String STUDENT_PASSWORD = "Student@123";
    private static final String ADMIN_PHONE = "0900000000";
    private static final String ADMIN_PASSWORD = "Admin@123";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRESQL =
            new PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtDecoder jwtDecoder;

    private UUID userIdOf(String phoneNumber) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE phone_number = ?", UUID.class, phoneNumber);
    }

    /** Turns the seeded student into a student who is also a teacher. */
    private void alsoMakeTeacher(String phoneNumber) {
        UUID userId = userIdOf(phoneNumber);
        jdbcTemplate.update(
                "INSERT INTO user_roles (user_id, role, created_at) VALUES (?, 'TEACHER', CURRENT_TIMESTAMP)",
                userId);
        jdbcTemplate.update(
                """
                INSERT INTO teacher_profiles
                    (id, user_id, teacher_code, full_name, enabled, version, created_at, updated_at)
                VALUES (?, ?, 'GV900', 'Giáo viên kiêm nhiệm', TRUE, 0,
                        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                UUID.randomUUID(), userId);
    }

    private String loginBody(String phone, String password, String role) {
        return role == null
                ? "{\"phoneNumber\":\"%s\",\"password\":\"%s\"}".formatted(phone, password)
                : "{\"phoneNumber\":\"%s\",\"password\":\"%s\",\"role\":\"%s\"}"
                        .formatted(phone, password, role);
    }

    @Test
    void aSingleRoleAccountGoesStraightInAndReportsItsRole() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(STUDENT_PHONE, STUDENT_PASSWORD, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeRole").value("STUDENT"))
                .andExpect(jsonPath("$.availableRoles").isArray())
                .andExpect(jsonPath("$.availableRoles[0]").value("STUDENT"))
                .andExpect(jsonPath("$.student.studentCode").value("SE1913001"));
    }

    /**
     * Administration is web-only, so an admin must not be able to open a mobile session even
     * though the account holds exactly one role and would otherwise be waved straight through.
     */
    @Test
    void anAdminCannotOpenAMobileSession() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(ADMIN_PHONE, ADMIN_PASSWORD, null)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(ADMIN_PHONE, ADMIN_PASSWORD, "ADMIN")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anAccountWithSeveralRolesMustSayWhichOneToOpen() throws Exception {
        alsoMakeTeacher(STUDENT_PHONE);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(STUDENT_PHONE, STUDENT_PASSWORD, null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ROLE_SELECTION_REQUIRED"));
    }

    @Test
    void aChosenRoleDecidesTheTokenAndTheProfile() throws Exception {
        alsoMakeTeacher(STUDENT_PHONE);

        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(STUDENT_PHONE, STUDENT_PASSWORD, "TEACHER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeRole").value("TEACHER"))
                .andExpect(jsonPath("$.student").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(JsonPath.<java.util.List<String>>read(body, "$.availableRoles"))
                .containsExactlyInAnyOrder("STUDENT", "TEACHER");
        assertThat(jwtDecoder.decode(JsonPath.read(body, "$.accessToken")).getClaimAsString("role"))
                .isEqualTo("TEACHER");
    }

    @Test
    void aRoleTheAccountDoesNotHoldIsRefused() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(STUDENT_PHONE, STUDENT_PASSWORD, "TEACHER")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void switchingRoleOpensANewSessionAndRetiresTheOldOne() throws Exception {
        alsoMakeTeacher(STUDENT_PHONE);
        String studentSession = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(STUDENT_PHONE, STUDENT_PASSWORD, "STUDENT")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String refreshToken = JsonPath.read(studentSession, "$.refreshToken");

        String switched = mockMvc.perform(post("/api/v1/auth/switch-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\",\"role\":\"TEACHER\"}".formatted(refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeRole").value("TEACHER"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(jwtDecoder.decode(JsonPath.read(switched, "$.accessToken")).getClaimAsString("role"))
                .isEqualTo("TEACHER");

        // The old session must not survive the switch, or the account would hold two live
        // sessions and a stale token could still answer for the role left behind.
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\"}".formatted(refreshToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aTeacherSessionRefreshesIntoItsOwnRoleRatherThanStudent() throws Exception {
        alsoMakeTeacher(STUDENT_PHONE);
        String session = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(STUDENT_PHONE, STUDENT_PASSWORD, "TEACHER")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\"}"
                                .formatted(JsonPath.<String>read(session, "$.refreshToken"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeRole").value("TEACHER"));
    }

    @Test
    void switchingToARoleTheAccountDoesNotHoldIsRefused() throws Exception {
        String session = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(STUDENT_PHONE, STUDENT_PASSWORD, null)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        mockMvc.perform(post("/api/v1/auth/switch-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\",\"role\":\"PARENT\"}"
                                .formatted(JsonPath.<String>read(session, "$.refreshToken"))))
                .andExpect(status().isUnauthorized());
    }
}
