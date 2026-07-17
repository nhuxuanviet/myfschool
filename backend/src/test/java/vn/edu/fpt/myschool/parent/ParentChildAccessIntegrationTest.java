package vn.edu.fpt.myschool.parent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import vn.edu.fpt.myschool.auth.application.AuthService;
import vn.edu.fpt.myschool.auth.domain.UserRole;

/**
 * The R4 gate: a guardian reaches their own child and no one else's.
 *
 * <p>Two students exist and the guardian is linked to one, so "mine" and "not mine" are both real
 * rather than one child and an empty world.
 */
@Testcontainers
@ActiveProfiles({"test", "e2e"})
@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Transactional
class ParentChildAccessIntegrationTest {

    private static final String PARENT_PHONE = "0955000001";
    private static final String PASSWORD = "Parent@123";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRESQL =
            new PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AuthService authService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private UUID parentId;
    private UUID ownChildId;
    private UUID otherChildId;
    private UUID linkId;

    private String token() {
        return authService.login(PARENT_PHONE, PASSWORD, UserRole.PARENT).accessToken();
    }

    @BeforeEach
    void setUp() {
        UUID userId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO users (id, phone_number, password_hash, enabled,
                                   credentials_updated_at, created_at, updated_at)
                VALUES (?, ?, ?, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                userId, PARENT_PHONE, passwordEncoder.encode(PASSWORD));
        jdbcTemplate.update(
                "INSERT INTO user_roles (user_id, role, created_at) VALUES (?, 'PARENT', CURRENT_TIMESTAMP)",
                userId);
        parentId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO parent_profiles
                    (id, user_id, full_name, enabled, version, created_at, updated_at)
                VALUES (?, ?, 'Nguyễn Văn Phụ Huynh', TRUE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                parentId, userId);

        List<UUID> students = jdbcTemplate.queryForList(
                "SELECT id FROM students ORDER BY student_code LIMIT 2", UUID.class);
        ownChildId = students.get(0);
        otherChildId = students.get(1);

        linkId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO parent_student_links
                    (id, parent_id, student_id, relationship, contact_order,
                     effective_from, effective_to, created_at, updated_at)
                VALUES (?, ?, ?, 'FATHER', 1, CURRENT_DATE, NULL,
                        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                linkId, parentId, ownChildId);
    }

    @Test
    void listsOnlyTheChildrenTheGuardianIsLinkedTo() throws Exception {
        mockMvc.perform(get("/api/v1/parent/children")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].studentId").value(ownChildId.toString()));
    }

    @Test
    void readsTheGradesOfTheirOwnChild() throws Exception {
        mockMvc.perform(get("/api/v1/parent/children/" + ownChildId + "/grades")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subjects").isArray());
    }

    /** The other student exists and has marks; only the link is missing. */
    @Test
    void refusesTheGradesOfAStudentTheyAreNotLinkedTo() throws Exception {
        mockMvc.perform(get("/api/v1/parent/children/" + otherChildId + "/grades")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_LINKED_TO_STUDENT"));
    }

    /** A student who does not exist must look exactly like one who is simply not theirs. */
    @Test
    void answersTheSameWayForAStudentThatDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/v1/parent/children/" + UUID.randomUUID() + "/grades")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_LINKED_TO_STUDENT"));
    }

    /** Ending a link must take effect at once: access is the link, so it cannot outlive it. */
    @Test
    void losesAccessOnceTheLinkHasEnded() throws Exception {
        mockMvc.perform(get("/api/v1/parent/children/" + ownChildId + "/grades")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token()))
                .andExpect(status().isOk());

        jdbcTemplate.update(
                "UPDATE parent_student_links SET effective_to = CURRENT_DATE WHERE id = ?", linkId);

        mockMvc.perform(get("/api/v1/parent/children/" + ownChildId + "/grades")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/parent/children")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token()))
                .andExpect(jsonPath("$.length()").value(0));
    }

    /** A link dated to start tomorrow grants nothing today. */
    @Test
    void grantsNothingBeforeTheLinkStarts() throws Exception {
        jdbcTemplate.update(
                "UPDATE parent_student_links SET effective_from = CURRENT_DATE + 1 WHERE id = ?",
                linkId);

        mockMvc.perform(get("/api/v1/parent/children/" + ownChildId + "/grades")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token()))
                .andExpect(status().isForbidden());
    }

    /**
     * A guardian must not see more than the student does.
     *
     * <p>Both go through the same query, so unpublishing hides the marks from the guardian too
     * without anyone having to remember a second rule.
     */
    @Test
    void seesNoMarkThatHasNotBeenPublished() throws Exception {
        String body = mockMvc.perform(get("/api/v1/parent/children/" + ownChildId + "/grades")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(JsonPath.<List<String>>read(body, "$..assessments[*].displayLabel")).isNotEmpty();

        jdbcTemplate.update("UPDATE grade_books SET published_at = NULL, published_by = NULL");

        String hidden = mockMvc.perform(get("/api/v1/parent/children/" + ownChildId + "/grades")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(JsonPath.<List<String>>read(hidden, "$..assessments[*].displayLabel")).isEmpty();
    }

    @Test
    void readsTheTimetableOfTheirOwnChild() throws Exception {
        mockMvc.perform(get("/api/v1/parent/children/" + ownChildId + "/timetable")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days").isArray());
    }

    /** The class code is never taken from the caller: it comes from the child the link names. */
    @Test
    void refusesTheTimetableOfAStudentTheyAreNotLinkedTo() throws Exception {
        mockMvc.perform(get("/api/v1/parent/children/" + otherChildId + "/timetable")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_LINKED_TO_STUDENT"));
    }

    @Test
    void aStudentCannotReachTheParentNamespace() throws Exception {
        String studentToken = authService
                .login("0912345678", "Student@123", UserRole.STUDENT)
                .accessToken();

        mockMvc.perform(get("/api/v1/parent/children")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
                .andExpect(status().isForbidden());
    }
}
