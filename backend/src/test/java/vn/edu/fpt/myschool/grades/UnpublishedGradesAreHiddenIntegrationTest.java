package vn.edu.fpt.myschool.grades;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
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
 * The R3 gate: an unpublished mark must not reach the student.
 *
 * <p>Asserted against the real endpoint the app calls, because the point is what a student can
 * actually retrieve, not what a query happens to say in isolation.
 */
@Testcontainers
@ActiveProfiles({"test", "e2e"})
@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Transactional
class UnpublishedGradesAreHiddenIntegrationTest {

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

    private String studentToken() {
        return authService.login("0912345678", "Student@123", UserRole.STUDENT).accessToken();
    }

    private UUID currentTermId() {
        return jdbcTemplate.queryForObject(
                """
                SELECT enrollment.academic_term_id
                FROM student_term_subjects enrollment
                INNER JOIN students student ON student.id = enrollment.student_id
                INNER JOIN academic_terms term ON term.id = enrollment.academic_term_id
                WHERE student.student_code = 'SE1913001'
                ORDER BY term.starts_on DESC
                LIMIT 1
                """,
                UUID.class);
    }

    private List<String> markLabelsSeenByStudent(UUID termId) throws Exception {
        String body = mockMvc.perform(get("/api/v1/grades")
                        .param("termId", termId.toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(body, "$..assessments[*].displayLabel");
    }

    @Test
    void showsMarksWhileTheirBookIsPublished() throws Exception {
        assertThat(markLabelsSeenByStudent(currentTermId())).isNotEmpty();
    }

    /** Withdrawing publication must take effect immediately, not at the next login. */
    @Test
    void hidesEveryMarkOnceTheBookIsNoLongerPublished() throws Exception {
        UUID termId = currentTermId();
        assertThat(markLabelsSeenByStudent(termId)).isNotEmpty();

        jdbcTemplate.update(
                """
                UPDATE grade_books SET published_at = NULL, published_by = NULL
                WHERE academic_term_id = ?
                """,
                termId);

        assertThat(markLabelsSeenByStudent(termId)).isEmpty();
    }

    /**
     * The subject must survive its book being unpublished.
     *
     * <p>Hiding the subject as well would tell the student a subject stopped existing, and would
     * quietly drop it from the term average once totals are computed.
     */
    @Test
    void keepsTheSubjectItselfVisibleWithoutItsMarks() throws Exception {
        UUID termId = currentTermId();
        jdbcTemplate.update(
                "UPDATE grade_books SET published_at = NULL, published_by = NULL WHERE academic_term_id = ?",
                termId);

        String body = mockMvc.perform(get("/api/v1/grades")
                        .param("termId", termId.toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(JsonPath.<List<String>>read(body, "$.subjects[*].code")).isNotEmpty();
    }

    /** PENDING is the teacher's own "not entered yet": there is no value to show. */
    @Test
    void neverShowsAMarkThatHasNotBeenEnteredYet() throws Exception {
        UUID termId = currentTermId();
        UUID enrollmentId = jdbcTemplate.queryForObject(
                """
                SELECT enrollment.id
                FROM student_term_subjects enrollment
                INNER JOIN students student ON student.id = enrollment.student_id
                WHERE student.student_code = 'SE1913001'
                  AND enrollment.academic_term_id = ?
                  AND enrollment.assessment_mode = 'NUMERIC'
                LIMIT 1
                """,
                UUID.class, termId);
        UUID columnId = jdbcTemplate.queryForObject(
                """
                SELECT grade_column.id
                FROM grade_columns grade_column
                INNER JOIN grade_assessments assessment
                    ON assessment.grade_column_id = grade_column.id
                WHERE assessment.student_term_subject_id = ?
                LIMIT 1
                """,
                UUID.class, enrollmentId);

        jdbcTemplate.update(
                """
                INSERT INTO grade_assessments (
                    id, student_term_subject_id, grade_column_id, assessment_mode,
                    assessment_kind, assessment_form, display_label, status,
                    score, outcome, display_order, created_at, updated_at
                ) VALUES (?, ?, ?, 'NUMERIC', 'REGULAR', 'ORAL', 'Ô chưa nhập', 'PENDING',
                          NULL, NULL, 97, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                UUID.randomUUID(), enrollmentId, columnId);

        assertThat(markLabelsSeenByStudent(termId)).doesNotContain("Ô chưa nhập");
    }
}
