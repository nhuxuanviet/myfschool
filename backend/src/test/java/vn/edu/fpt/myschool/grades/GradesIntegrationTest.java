package vn.edu.fpt.myschool.grades;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Clock;
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

import vn.edu.fpt.myschool.shared.time.SchoolTimeZone;

@Testcontainers
@ActiveProfiles({"test", "e2e"})
@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Transactional
class GradesIntegrationTest {

    private static final UUID CURRENT_TERM_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000302");
    private static final UUID HISTORICAL_TERM_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000304");
    private static final UUID CURRENT_MATHEMATICS_ENROLLMENT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000001001");

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
    void requiresAuthenticationAndAllowsOnlyStudentGetRequests() throws Exception {
        mockMvc.perform(get("/api/v1/grades"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(post("/api/v1/grades")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + loginAccessToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void returnsCircularTwentyTwoSemesterGradesWithNumericAndRemarkResults() throws Exception {
        String response = mockMvc.perform(get("/api/v1/grades")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + loginAccessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timeZone").value("Asia/Ho_Chi_Minh"))
                .andExpect(jsonPath("$.selectedTerm.id").value(CURRENT_TERM_ID.toString()))
                .andExpect(jsonPath("$.selectedTerm.code").value("HK1"))
                .andExpect(jsonPath("$.availableTerms.length()").value(2))
                .andExpect(jsonPath("$.availableTerms[0].id").value(CURRENT_TERM_ID.toString()))
                .andExpect(jsonPath("$.availableTerms[1].id").value(HISTORICAL_TERM_ID.toString()))
                .andExpect(jsonPath("$.subjects.length()").value(3))
                .andExpect(jsonPath("$.subjects[0].code").value("TOAN"))
                .andExpect(jsonPath("$.subjects[0].assessmentMode").value("NUMERIC"))
                .andExpect(jsonPath("$.subjects[0].annualLessonCount").value(105))
                .andExpect(jsonPath("$.subjects[0].requiredRegularAssessments").value(4))
                .andExpect(jsonPath("$.subjects[0].termAverage").value(8.7))
                .andExpect(jsonPath("$.subjects[0].termResult").value(nullValue()))
                .andExpect(jsonPath("$.subjects[0].assessments[0].displayLabel").value("Miệng"))
                .andExpect(jsonPath("$.subjects[0].assessments[1].displayLabel").value("15 phút"))
                .andExpect(jsonPath("$.subjects[0].assessments[2].displayLabel").value("45 phút"))
                .andExpect(jsonPath("$.subjects[1].termAverage").value(nullValue()))
                .andExpect(jsonPath("$.subjects[1].assessments[2].status").value("MAKE_UP_REQUIRED"))
                .andExpect(jsonPath("$.subjects[2].assessmentMode").value("REMARK"))
                .andExpect(jsonPath("$.subjects[2].annualLessonCount").value(nullValue()))
                .andExpect(jsonPath("$.subjects[2].termAverage").value(nullValue()))
                .andExpect(jsonPath("$.subjects[2].termResult").value("ACHIEVED"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Number returnedAverage = JsonPath.read(response, "$.subjects[0].termAverage");
        assertThat(new BigDecimal(returnedAverage.toString()))
                .isEqualByComparingTo(new BigDecimal("8.7"));
    }

    @Test
    void scopesTermSelectionToTheJwtStudentAndSupportsSemesterSwitching() throws Exception {
        mockMvc.perform(get("/api/v1/grades")
                        .param("studentId", "00000000-0000-0000-0000-000000000999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + loginAccessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.selectedTerm.id").value(CURRENT_TERM_ID.toString()))
                .andExpect(jsonPath("$.subjects[0].code").value("TOAN"));

        mockMvc.perform(get("/api/v1/grades")
                        .param("termId", HISTORICAL_TERM_ID.toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + loginAccessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.selectedTerm.id").value(HISTORICAL_TERM_ID.toString()))
                .andExpect(jsonPath("$.selectedTerm.code").value("HK2"))
                .andExpect(jsonPath("$.subjects.length()").value(2))
                .andExpect(jsonPath("$.subjects[0].assessments[0].kind").value("REGULAR"))
                .andExpect(jsonPath("$.subjects[0].assessments[4].kind").value("MIDTERM"))
                .andExpect(jsonPath("$.subjects[0].assessments[5].kind").value("FINAL"));
    }

    @Test
    void returnsANonLeakyNotFoundProblemForATermOutsideTheStudentScope() throws Exception {
        mockMvc.perform(get("/api/v1/grades")
                        .param("termId", UUID.randomUUID().toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + loginAccessToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GRADE_TERM_NOT_FOUND"))
                .andExpect(jsonPath("$.detail").value("The requested semester grades are unavailable"));
    }

    @Test
    void databaseRejectsADuplicateMidtermForTheSameSubjectTerm() {
        Instant now = clock.instant();
        assertThatThrownBy(() -> insertAssessment(
                UUID.randomUUID(),
                "NUMERIC",
                "MIDTERM",
                "WRITTEN",
                "Kiểm tra giữa kỳ bổ sung",
                "RECORDED",
                new BigDecimal("8.0"),
                null,
                90,
                now))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseRejectsAnAssessmentModeThatDoesNotMatchTheEnrollment() {
        Instant now = clock.instant();
        assertThatThrownBy(() -> insertAssessment(
                UUID.randomUUID(),
                "REMARK",
                "REGULAR",
                "PRACTICAL",
                "Đánh giá sai chế độ",
                "RECORDED",
                null,
                "ACHIEVED",
                91,
                now))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseRejectsAScoreOutsideTheTenPointScale() {
        Instant now = clock.instant();
        assertThatThrownBy(() -> insertAssessment(
                UUID.randomUUID(),
                "NUMERIC",
                "REGULAR",
                "WRITTEN",
                "Điểm vượt thang",
                "RECORDED",
                new BigDecimal("10.1"),
                null,
                92,
                now))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private void insertAssessment(
            UUID id,
            String assessmentMode,
            String kind,
            String form,
            String displayLabel,
            String status,
            BigDecimal score,
            String outcome,
            int displayOrder,
            Instant now) {
        jdbcTemplate.update("""
                INSERT INTO grade_assessments (
                    id, student_term_subject_id, assessment_mode, assessment_kind, assessment_form,
                    display_label, duration_minutes, status, score, outcome, assessed_on,
                    display_order, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                id,
                CURRENT_MATHEMATICS_ENROLLMENT_ID,
                assessmentMode,
                kind,
                form,
                displayLabel,
                45,
                status,
                score,
                outcome,
                Date.valueOf(LocalDate.ofInstant(now, SchoolTimeZone.ZONE)),
                displayOrder,
                Timestamp.from(now),
                Timestamp.from(now));
    }

    private String loginAccessToken() throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phoneNumber":"0912345678","password":"Student@123"}
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(response, "$.accessToken");
    }
}
