package vn.edu.fpt.myschool.forms;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
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
class StudentFormsIntegrationTest {

    private static final UUID SEEDED_STUDENT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000201");
    private static final UUID PENDING_LEAVE_FORM_ID =
            UUID.fromString("00000000-0000-0000-0000-000000001701");
    private static final UUID APPROVED_CONFIRMATION_FORM_ID =
            UUID.fromString("00000000-0000-0000-0000-000000001702");
    private static final UUID PEER_PENDING_CARD_FORM_ID =
            UUID.fromString("00000000-0000-0000-0000-000000001704");

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRESQL =
            new PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Clock clock;

    @Test
    void requiresAuthenticationForAllStudentFormRoutes() throws Exception {
        mockMvc.perform(get("/api/v1/forms"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        mockMvc.perform(post("/api/v1/forms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validConfirmationRequest()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/v1/forms/{formId}", PENDING_LEAVE_FORM_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listsOnlyOwnedFormsAndSupportsStatusFiltering() throws Exception {
        String token = loginAccessToken("0912345678");
        mockMvc.perform(get("/api/v1/forms")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.forms.length()").value(3))
                .andExpect(jsonPath("$.forms[0].id").value(PENDING_LEAVE_FORM_ID.toString()))
                .andExpect(jsonPath("$.forms[*].id").value(
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem(
                                PEER_PENDING_CARD_FORM_ID.toString()))));

        mockMvc.perform(get("/api/v1/forms")
                        .param("status", "APPROVED")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.forms.length()").value(1))
                .andExpect(jsonPath("$.forms[0].id").value(APPROVED_CONFIRMATION_FORM_ID.toString()))
                .andExpect(jsonPath("$.forms[0].canCancel").value(false));
    }

    @Test
    void returnsANonLeakyNotFoundForAnotherStudentsForm() throws Exception {
        mockMvc.perform(get("/api/v1/forms/{formId}", PEER_PENDING_CARD_FORM_ID)
                        .param("studentId", "00000000-0000-0000-0000-000000000211")
                        .header(HttpHeaders.AUTHORIZATION, bearer(loginAccessToken("0912345678"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FORM_NOT_FOUND"))
                .andExpect(jsonPath("$.detail").value("The requested student form is unavailable"));
    }

    @Test
    void returnsFormDetailsWithAnOrderedStatusTimeline() throws Exception {
        mockMvc.perform(get("/api/v1/forms/{formId}", APPROVED_CONFIRMATION_FORM_ID)
                        .header(HttpHeaders.AUTHORIZATION, bearer(loginAccessToken("0912345678"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(APPROVED_CONFIRMATION_FORM_ID.toString()))
                .andExpect(jsonPath("$.type").value("STUDENT_CONFIRMATION"))
                .andExpect(jsonPath("$.reason").isNotEmpty())
                .andExpect(jsonPath("$.startsOn").doesNotExist())
                .andExpect(jsonPath("$.endsOn").doesNotExist())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.canCancel").value(false))
                .andExpect(jsonPath("$.timeline.length()").value(3))
                .andExpect(jsonPath("$.timeline[0].status").value("SUBMITTED"))
                .andExpect(jsonPath("$.timeline[1].status").value("IN_REVIEW"))
                .andExpect(jsonPath("$.timeline[2].status").value("APPROVED"));
    }

    @Test
    void createsAFormForTheJwtStudentAndCancelsItWithATimelineEntry() throws Exception {
        String token = loginAccessToken("0912345678");
        String response = mockMvc.perform(post("/api/v1/forms")
                        .param("studentId", "00000000-0000-0000-0000-000000000211")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validConfirmationRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("STUDENT_CONFIRMATION"))
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.canCancel").value(true))
                .andExpect(jsonPath("$.timeline.length()").value(1))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String formId = JsonPath.read(response, "$.id");

        mockMvc.perform(delete("/api/v1/forms/{formId}", formId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.canCancel").value(false))
                .andExpect(jsonPath("$.timeline.length()").value(2))
                .andExpect(jsonPath("$.timeline[1].status").value("CANCELLED"));

        UUID ownerId = jdbcTemplate.queryForObject(
                "SELECT student_id FROM student_forms WHERE id = ?",
                UUID.class,
                UUID.fromString(formId));
        org.assertj.core.api.Assertions.assertThat(ownerId).isEqualTo(SEEDED_STUDENT_ID);
    }

    @Test
    void rejectsInvalidLeaveDatesAndDatesOnOtherFormTypes() throws Exception {
        String token = loginAccessToken("0912345678");
        mockMvc.perform(post("/api/v1/forms")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type":"LEAVE_OF_ABSENCE",
                                  "reason":"Xin nghỉ học",
                                  "startsOn":"2026-07-20",
                                  "endsOn":"2026-07-19"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("FORM_DATE_RANGE_INVALID"));

        mockMvc.perform(post("/api/v1/forms")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type":"TRANSCRIPT_REQUEST",
                                  "reason":"Xin bảng điểm",
                                  "startsOn":"2026-07-20",
                                  "endsOn":"2026-07-20"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("FORM_DATE_RANGE_INVALID"));
    }

    @Test
    void rejectsCancellationAfterAFormHasReachedATerminalStatus() throws Exception {
        mockMvc.perform(delete("/api/v1/forms/{formId}", APPROVED_CONFIRMATION_FORM_ID)
                        .header(HttpHeaders.AUTHORIZATION, bearer(loginAccessToken("0912345678"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("FORM_CANNOT_BE_CANCELLED"));
    }

    @Test
    void databaseRejectsDatesForANonLeaveForm() {
        Instant now = clock.instant();
        LocalDate date = LocalDate.of(2026, 7, 20);
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO student_forms (
                    id, student_id, form_type, reason, starts_on, ends_on, status,
                    submitted_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                UUID.randomUUID(),
                SEEDED_STUDENT_ID,
                "TRANSCRIPT_REQUEST",
                "Invalid dates",
                date,
                date,
                "SUBMITTED",
                Timestamp.from(now),
                Timestamp.from(now.plus(Duration.ofSeconds(1)))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private String loginAccessToken(String phoneNumber) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phoneNumber":"%s","password":"Student@123"}
                                """.formatted(phoneNumber)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(response, "$.accessToken");
    }

    private static String validConfirmationRequest() {
        return """
                {
                  "type":"STUDENT_CONFIRMATION",
                  "reason":"Xin giấy xác nhận học sinh để bổ sung hồ sơ.",
                  "startsOn":null,
                  "endsOn":null
                }
                """;
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
