package vn.edu.fpt.myschool.admin.grades;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
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
import org.springframework.http.MediaType;
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

/** Closes the R3 loop: lock, request a correction, decide it, and see the mark actually change. */
@Testcontainers
@ActiveProfiles({"test", "e2e"})
@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Transactional
class GradeGovernanceIntegrationTest {

    private static final String TEACHER_PHONE = "0966200001";
    private static final String TEACHER_PASSWORD = "Teacher@123";

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

    private UUID bookId;
    private UUID assessmentId;
    private long bookVersion;

    private String adminToken() {
        return authService.loginForRole("0900000000", "Admin@123", UserRole.ADMIN).accessToken();
    }

    private String teacherToken() {
        return authService.login(TEACHER_PHONE, TEACHER_PASSWORD, UserRole.TEACHER).accessToken();
    }

    @BeforeEach
    void setUp() {
        var slot = jdbcTemplate.queryForMap(
                """
                SELECT book.id AS book_id, book.class_id, book.subject_id,
                       book.academic_term_id, book.version
                FROM grade_books book
                INNER JOIN grade_columns grade_column ON grade_column.grade_book_id = book.id
                INNER JOIN grade_assessments assessment
                    ON assessment.grade_column_id = grade_column.id
                WHERE assessment.status = 'RECORDED' AND assessment.score IS NOT NULL
                LIMIT 1
                """);
        bookId = (UUID) slot.get("book_id");
        bookVersion = ((Number) slot.get("version")).longValue();
        assessmentId = jdbcTemplate.queryForObject(
                """
                SELECT assessment.id FROM grade_assessments assessment
                INNER JOIN grade_columns grade_column ON grade_column.id = assessment.grade_column_id
                WHERE grade_column.grade_book_id = ? AND assessment.score IS NOT NULL
                LIMIT 1
                """,
                UUID.class, bookId);

        UUID userId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO users (id, phone_number, password_hash, enabled,
                                   credentials_updated_at, created_at, updated_at)
                VALUES (?, ?, ?, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                userId, TEACHER_PHONE, passwordEncoder.encode(TEACHER_PASSWORD));
        jdbcTemplate.update(
                "INSERT INTO user_roles (user_id, role, created_at) VALUES (?, 'TEACHER', CURRENT_TIMESTAMP)",
                userId);
        UUID teacherId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO teacher_profiles
                    (id, user_id, teacher_code, full_name, enabled, version, created_at, updated_at)
                VALUES (?, ?, 'GVQ001', 'Giáo viên phụ trách', TRUE, 0,
                        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                teacherId, userId);
        jdbcTemplate.update(
                """
                INSERT INTO teacher_subject_assignments
                    (id, teacher_id, class_id, subject_id, academic_term_id, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                ON CONFLICT DO NOTHING
                """,
                UUID.randomUUID(), teacherId, slot.get("class_id"), slot.get("subject_id"),
                slot.get("academic_term_id"));
    }

    private void lockBook() throws Exception {
        mockMvc.perform(post("/api/v1/admin/gradebooks/" + bookId + "/lock")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":%d}".formatted(bookVersion)))
                .andExpect(status().isNoContent());
    }

    private String raiseRequest() throws Exception {
        return JsonPath.read(
                mockMvc.perform(post("/api/v1/teacher/gradebooks/marks/" + assessmentId
                                        + "/change-requests")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"newScore":9.5,"reason":"Cộng nhầm điểm thành phần"}
                                        """))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$.id");
    }

    @Test
    void locksABookAndRecordsWhoLockedIt() throws Exception {
        lockBook();

        var book = jdbcTemplate.queryForMap(
                "SELECT locked_at, locked_by FROM grade_books WHERE id = ?", bookId);
        assertThat(book.get("locked_at")).isNotNull();
        assertThat(book.get("locked_by")).isNotNull();
    }

    /** Approving must change the mark, not just the paperwork. */
    @Test
    void approvingARequestActuallyChangesTheMark() throws Exception {
        lockBook();
        String requestId = raiseRequest();

        mockMvc.perform(post("/api/v1/admin/grade-change-requests/" + requestId + "/approve")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decisionNote\":\"Đã đối chiếu bài\"}"))
                .andExpect(status().isNoContent());

        assertThat(jdbcTemplate.queryForObject(
                        "SELECT score FROM grade_assessments WHERE id = ?",
                        BigDecimal.class, assessmentId))
                .isEqualByComparingTo("9.5");
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT status FROM grade_change_requests WHERE id = ?::uuid",
                        String.class, requestId))
                .isEqualTo("APPROVED");
    }

    /** Rejecting must leave the mark exactly as it was. */
    @Test
    void rejectingARequestLeavesTheMarkAlone() throws Exception {
        lockBook();
        BigDecimal before = jdbcTemplate.queryForObject(
                "SELECT score FROM grade_assessments WHERE id = ?", BigDecimal.class, assessmentId);
        String requestId = raiseRequest();

        mockMvc.perform(post("/api/v1/admin/grade-change-requests/" + requestId + "/reject")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decisionNote\":\"Không đủ căn cứ\"}"))
                .andExpect(status().isNoContent());

        assertThat(jdbcTemplate.queryForObject(
                        "SELECT score FROM grade_assessments WHERE id = ?",
                        BigDecimal.class, assessmentId))
                .isEqualByComparingTo(before);
    }

    @Test
    void refusesToDecideTheSameRequestTwice() throws Exception {
        lockBook();
        String requestId = raiseRequest();
        mockMvc.perform(post("/api/v1/admin/grade-change-requests/" + requestId + "/approve")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/admin/grade-change-requests/" + requestId + "/reject")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GRADE_CHANGE_REQUEST_DECIDED"));
    }

    /** While the book is open the teacher just edits: an approval queue there teaches rubber-stamping. */
    @Test
    void refusesAChangeRequestWhileTheBookIsStillOpen() throws Exception {
        mockMvc.perform(post("/api/v1/teacher/gradebooks/marks/" + assessmentId + "/change-requests")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newScore":9.5,"reason":"Sửa khi sổ còn mở"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GRADE_BOOK_NOT_LOCKED"));
    }

    @Test
    void theRequestRemembersWhatTheMarkHeldWhenItWasRaised() throws Exception {
        lockBook();
        BigDecimal before = jdbcTemplate.queryForObject(
                "SELECT score FROM grade_assessments WHERE id = ?", BigDecimal.class, assessmentId);
        String requestId = raiseRequest();

        assertThat(jdbcTemplate.queryForObject(
                        "SELECT old_score FROM grade_change_requests WHERE id = ?::uuid",
                        BigDecimal.class, requestId))
                .isEqualByComparingTo(before);
    }

    @Test
    void aTeacherCannotLockOrDecide() throws Exception {
        mockMvc.perform(post("/api/v1/admin/gradebooks/" + bookId + "/lock")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":0}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/grade-change-requests")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void refusesToLockABookThatIsAlreadyLocked() throws Exception {
        lockBook();

        mockMvc.perform(post("/api/v1/admin/gradebooks/" + bookId + "/lock")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":%d}".formatted(bookVersion + 1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GRADE_BOOK_NOT_LOCKABLE"));
    }
}
