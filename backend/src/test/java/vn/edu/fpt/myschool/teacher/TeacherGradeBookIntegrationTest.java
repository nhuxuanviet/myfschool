package vn.edu.fpt.myschool.teacher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

/** The R3 grade-book flow: open, add a column, enter marks, publish — and what is refused. */
@Testcontainers
@ActiveProfiles({"test", "e2e"})
@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Transactional
class TeacherGradeBookIntegrationTest {

    private static final String MATHS_PHONE = "0966100001";
    private static final String LITERATURE_PHONE = "0966100002";
    private static final String PASSWORD = "Teacher@123";

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

    private UUID classId;
    private UUID mathsSubjectId;
    private UUID literatureSubjectId;
    private UUID termId;

    private UUID createTeacher(String phone, String code) {
        UUID userId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO users (id, phone_number, password_hash, enabled,
                                   credentials_updated_at, created_at, updated_at)
                VALUES (?, ?, ?, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                userId, phone, passwordEncoder.encode(PASSWORD));
        jdbcTemplate.update(
                "INSERT INTO user_roles (user_id, role, created_at) VALUES (?, 'TEACHER', CURRENT_TIMESTAMP)",
                userId);
        UUID teacherId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO teacher_profiles
                    (id, user_id, teacher_code, full_name, enabled, version, created_at, updated_at)
                VALUES (?, ?, ?, ?, TRUE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                teacherId, userId, code, "Giáo viên " + code);
        return teacherId;
    }

    private void assign(UUID teacherId, UUID subjectId) {
        jdbcTemplate.update(
                """
                INSERT INTO teacher_subject_assignments
                    (id, teacher_id, class_id, subject_id, academic_term_id, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                ON CONFLICT DO NOTHING
                """,
                UUID.randomUUID(), teacherId, classId, subjectId, termId);
    }

    private String token(String phone) {
        return authService.login(phone, PASSWORD, UserRole.TEACHER).accessToken();
    }

    private String openBook(String phone, UUID subjectId) throws Exception {
        return mockMvc.perform(post("/api/v1/teacher/gradebooks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(phone))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"classId":"%s","subjectId":"%s","academicTermId":"%s"}
                                """.formatted(classId, subjectId, termId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    @BeforeEach
    void setUp() {
        // A class-subject-term that actually has enrolled students, so a column has cells to fill.
        var slot = jdbcTemplate.queryForMap(
                """
                SELECT student.class_id, enrollment.subject_id, enrollment.academic_term_id
                FROM student_term_subjects enrollment
                INNER JOIN students student ON student.id = enrollment.student_id
                WHERE student.class_id IS NOT NULL AND enrollment.assessment_mode = 'NUMERIC'
                LIMIT 1
                """);
        classId = (UUID) slot.get("class_id");
        mathsSubjectId = (UUID) slot.get("subject_id");
        termId = (UUID) slot.get("academic_term_id");
        literatureSubjectId = jdbcTemplate.queryForObject(
                "SELECT id FROM subjects WHERE id <> ? LIMIT 1", UUID.class, mathsSubjectId);

        assign(createTeacher(MATHS_PHONE, "GVG001"), mathsSubjectId);
        assign(createTeacher(LITERATURE_PHONE, "GVG002"), literatureSubjectId);
    }

    /** Creating a column must open a cell for every enrolled student at once, not as marks arrive. */
    @Test
    void addingAColumnOpensAnEmptyCellForEveryEnrolledStudent() throws Exception {
        String bookId = JsonPath.read(openBook(MATHS_PHONE, mathsSubjectId), "$.id");

        mockMvc.perform(post("/api/v1/teacher/gradebooks/" + bookId + "/columns")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(MATHS_PHONE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"assessmentKind":"REGULAR","assessmentForm":"ORAL","displayLabel":"Miệng 9"}
                                """))
                .andExpect(status().isCreated());

        Integer enrolled = jdbcTemplate.queryForObject(
                """
                SELECT count(*) FROM student_term_subjects enrollment
                INNER JOIN students student ON student.id = enrollment.student_id
                WHERE student.class_id = ? AND enrollment.subject_id = ?
                  AND enrollment.academic_term_id = ?
                """,
                Integer.class, classId, mathsSubjectId, termId);
        Integer pending = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM grade_assessments WHERE status = 'PENDING'", Integer.class);
        assertThat(pending).isEqualTo(enrolled).isGreaterThan(0);
    }

    /** Teaching maths to a class must not open that class's literature book. */
    @Test
    void refusesABookForASubjectTheTeacherDoesNotTeach() throws Exception {
        mockMvc.perform(post("/api/v1/teacher/gradebooks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(MATHS_PHONE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"classId":"%s","subjectId":"%s","academicTermId":"%s"}
                                """.formatted(classId, literatureSubjectId, termId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_ASSIGNED_TO_SUBJECT"));
    }

    @Test
    void recordsAMarkIntoAPendingCell() throws Exception {
        String bookId = JsonPath.read(openBook(MATHS_PHONE, mathsSubjectId), "$.id");
        mockMvc.perform(post("/api/v1/teacher/gradebooks/" + bookId + "/columns")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(MATHS_PHONE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"assessmentKind":"REGULAR","assessmentForm":"ORAL","displayLabel":"Miệng 9"}
                                """))
                .andExpect(status().isCreated());
        UUID assessmentId = jdbcTemplate.queryForObject(
                "SELECT id FROM grade_assessments WHERE status = 'PENDING' LIMIT 1", UUID.class);

        mockMvc.perform(put("/api/v1/teacher/gradebooks/marks/" + assessmentId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(MATHS_PHONE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":8.5}"))
                .andExpect(status().isNoContent());

        assertThat(jdbcTemplate.queryForObject(
                        "SELECT status FROM grade_assessments WHERE id = ?", String.class, assessmentId))
                .isEqualTo("RECORDED");
    }

    /** A numeric subject takes a score; sending both, or neither, is meaningless. */
    @Test
    void refusesAMarkThatIsNeitherAScoreNorAnOutcome() throws Exception {
        String bookId = JsonPath.read(openBook(MATHS_PHONE, mathsSubjectId), "$.id");
        mockMvc.perform(post("/api/v1/teacher/gradebooks/" + bookId + "/columns")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(MATHS_PHONE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"assessmentKind":"REGULAR","assessmentForm":"ORAL","displayLabel":"Miệng 9"}
                                """))
                .andExpect(status().isCreated());
        UUID assessmentId = jdbcTemplate.queryForObject(
                "SELECT id FROM grade_assessments WHERE status = 'PENDING' LIMIT 1", UUID.class);

        mockMvc.perform(put("/api/v1/teacher/gradebooks/marks/" + assessmentId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(MATHS_PHONE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MARK_DOES_NOT_MATCH_SUBJECT_MODE"));
    }

    @Test
    void publishingIsRecordedAgainstTheTeacherWhoDidIt() throws Exception {
        String body = openBook(MATHS_PHONE, mathsSubjectId);
        String bookId = JsonPath.read(body, "$.id");
        int version = JsonPath.read(body, "$.version");

        mockMvc.perform(post("/api/v1/teacher/gradebooks/" + bookId + "/publish")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(MATHS_PHONE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":%d}".formatted(version)))
                .andExpect(status().isNoContent());

        var book = jdbcTemplate.queryForMap(
                "SELECT published_at, published_by FROM grade_books WHERE id = ?::uuid", bookId);
        assertThat(book.get("published_at")).isNotNull();
        assertThat(book.get("published_by")).isNotNull();
    }

    /**
     * A locked book stops direct editing. 409 and not 403: the teacher is the right person, the
     * book is simply in a state where changes go through the administration.
     */
    @Test
    void refusesEditsOnceTheBookIsLocked() throws Exception {
        String bookId = JsonPath.read(openBook(MATHS_PHONE, mathsSubjectId), "$.id");
        jdbcTemplate.update(
                """
                UPDATE grade_books
                SET locked_at = CURRENT_TIMESTAMP,
                    locked_by = (SELECT id FROM users LIMIT 1)
                WHERE id = ?::uuid
                """,
                bookId);

        mockMvc.perform(post("/api/v1/teacher/gradebooks/" + bookId + "/columns")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(MATHS_PHONE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"assessmentKind":"REGULAR","assessmentForm":"ORAL","displayLabel":"Sau khoá"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GRADE_BOOK_LOCKED"));
    }

    /** Publishing does not freeze a book: only locking does (spec §5.5). */
    @Test
    void stillAllowsEditingAPublishedButUnlockedBook() throws Exception {
        String body = openBook(MATHS_PHONE, mathsSubjectId);
        String bookId = JsonPath.read(body, "$.id");
        int version = JsonPath.read(body, "$.version");
        mockMvc.perform(post("/api/v1/teacher/gradebooks/" + bookId + "/publish")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(MATHS_PHONE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":%d}".formatted(version)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/teacher/gradebooks/" + bookId + "/columns")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(MATHS_PHONE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"assessmentKind":"REGULAR","assessmentForm":"ORAL","displayLabel":"Sau công bố"}
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void refusesTheSheetOfAnotherTeachersBook() throws Exception {
        String bookId = JsonPath.read(openBook(MATHS_PHONE, mathsSubjectId), "$.id");

        mockMvc.perform(get("/api/v1/teacher/gradebooks/" + bookId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(LITERATURE_PHONE)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_ASSIGNED_TO_SUBJECT"));
    }

    @Test
    void refusesAPublishBuiltOnAStaleRead() throws Exception {
        String body = openBook(MATHS_PHONE, mathsSubjectId);
        String bookId = JsonPath.read(body, "$.id");
        int version = JsonPath.read(body, "$.version");
        mockMvc.perform(post("/api/v1/teacher/gradebooks/" + bookId + "/publish")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(MATHS_PHONE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":%d}".formatted(version)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/teacher/gradebooks/" + bookId + "/unpublish")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(MATHS_PHONE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":%d}".formatted(version)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GRADE_BOOK_VERSION_CONFLICT"));
    }

    @Test
    void aStudentCannotReachTheGradeBook() throws Exception {
        String studentToken = authService
                .login("0912345678", "Student@123", UserRole.STUDENT)
                .accessToken();

        mockMvc.perform(post("/api/v1/teacher/gradebooks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"classId":"%s","subjectId":"%s","academicTermId":"%s"}
                                """.formatted(classId, mathsSubjectId, termId)))
                .andExpect(status().isForbidden());
    }
}
