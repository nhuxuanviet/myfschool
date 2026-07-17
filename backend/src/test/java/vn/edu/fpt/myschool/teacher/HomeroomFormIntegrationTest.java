package vn.edu.fpt.myschool.teacher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

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

/**
 * The R5 gate: the homeroom teacher decides their own class's leave requests, and nobody else's.
 *
 * <p>Two homeroom teachers of two classes, so "my class" and "another class" are both real.
 */
@Testcontainers
@ActiveProfiles({"test", "e2e"})
@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Transactional
class HomeroomFormIntegrationTest {

    private static final String OWN_PHONE = "0966300001";
    private static final String OTHER_PHONE = "0966300002";
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

    private UUID studentId;
    private UUID leaveFormId;
    private UUID adminFormId;

    private UUID createHomeroomTeacher(String phone, String code, UUID classId) {
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
                teacherId, userId, code, "GVCN " + code);
        jdbcTemplate.update(
                """
                INSERT INTO homeroom_assignments
                    (id, teacher_id, class_id, academic_year_id, created_at, updated_at)
                SELECT ?, ?, ?, academic_year_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                FROM school_classes WHERE id = ?
                ON CONFLICT DO NOTHING
                """,
                UUID.randomUUID(), teacherId, classId, classId);
        return teacherId;
    }

    private UUID insertForm(String type, String status) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO student_forms (
                    id, student_id, form_type, reason, starts_on, ends_on, status,
                    submitted_by, submitted_by_role, submitted_at, updated_at
                )
                -- Only a leave request carries dates; the schema requires the others to have none.
                SELECT ?, ?, ?, 'Lý do thử',
                       CASE WHEN ? = 'LEAVE_OF_ABSENCE' THEN CURRENT_DATE END,
                       CASE WHEN ? = 'LEAVE_OF_ABSENCE' THEN CURRENT_DATE END,
                       ?, student.user_id, 'STUDENT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                FROM students student WHERE student.id = ?
                """,
                id, studentId, type, type, type, status, studentId);
        return id;
    }

    private String token(String phone) {
        return authService.login(phone, PASSWORD, UserRole.TEACHER).accessToken();
    }

    @BeforeEach
    void setUp() {
        var row = jdbcTemplate.queryForMap(
                "SELECT id, class_id FROM students WHERE class_id IS NOT NULL LIMIT 1");
        studentId = (UUID) row.get("id");
        UUID ownClassId = (UUID) row.get("class_id");
        UUID otherClassId = jdbcTemplate.queryForObject(
                "SELECT id FROM school_classes WHERE id <> ? LIMIT 1", UUID.class, ownClassId);

        createHomeroomTeacher(OWN_PHONE, "GVCN01", ownClassId);
        createHomeroomTeacher(OTHER_PHONE, "GVCN02", otherClassId);
        leaveFormId = insertForm("LEAVE_OF_ABSENCE", "SUBMITTED");
        adminFormId = insertForm("TRANSCRIPT_REQUEST", "SUBMITTED");
    }

    @Test
    void listsTheLeaveRequestsOfTheirOwnHomeroomClass() throws Exception {
        mockMvc.perform(get("/api/v1/teacher/homeroom-forms")
                        .param("status", "SUBMITTED")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(OWN_PHONE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + leaveFormId + "')]").exists());
    }

    /** An administrative form is the office's business and must never enter this queue. */
    @Test
    void neverShowsAnAdministrativeFormToTheHomeroomTeacher() throws Exception {
        mockMvc.perform(get("/api/v1/teacher/homeroom-forms")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(OWN_PHONE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + adminFormId + "')]").doesNotExist());
    }

    @Test
    void refusesToDecideAnAdministrativeFormEvenForTheirOwnStudent() throws Exception {
        mockMvc.perform(post("/api/v1/teacher/homeroom-forms/" + adminFormId + "/approve")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(OWN_PHONE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void approvesAndNamesTheTeacherWhoDecided() throws Exception {
        mockMvc.perform(post("/api/v1/teacher/homeroom-forms/" + leaveFormId + "/approve")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(OWN_PHONE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"Đồng ý cho nghỉ\"}"))
                .andExpect(status().isNoContent());

        assertThat(jdbcTemplate.queryForObject(
                        "SELECT status FROM student_forms WHERE id = ?", String.class, leaveFormId))
                .isEqualTo("APPROVED");

        // The trail must name who approved it; that is the defect V28 existed to fix.
        var history = jdbcTemplate.queryForMap(
                """
                SELECT actor_user_id, actor_role FROM student_form_status_history
                WHERE form_id = ? ORDER BY sequence_number DESC LIMIT 1
                """,
                leaveFormId);
        assertThat(history.get("actor_user_id")).isNotNull();
        assertThat(history.get("actor_role")).isEqualTo("TEACHER");
    }

    /** The homeroom teacher of another class is a teacher, and still has no business here. */
    @Test
    void refusesTheHomeroomTeacherOfADifferentClass() throws Exception {
        mockMvc.perform(post("/api/v1/teacher/homeroom-forms/" + leaveFormId + "/approve")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(OTHER_PHONE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_HOMEROOM_TEACHER_OF_STUDENT"));

        mockMvc.perform(get("/api/v1/teacher/homeroom-forms")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(OTHER_PHONE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + leaveFormId + "')]").doesNotExist());
    }

    @Test
    void refusesToDecideTheSameFormTwice() throws Exception {
        mockMvc.perform(post("/api/v1/teacher/homeroom-forms/" + leaveFormId + "/approve")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(OWN_PHONE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/teacher/homeroom-forms/" + leaveFormId + "/reject")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(OWN_PHONE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("FORM_ALREADY_DECIDED"));
    }

    @Test
    void rejectingLeavesTheFormRejectedAndTrailed() throws Exception {
        mockMvc.perform(post("/api/v1/teacher/homeroom-forms/" + leaveFormId + "/reject")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(OWN_PHONE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"Trùng lịch kiểm tra\"}"))
                .andExpect(status().isNoContent());

        assertThat(jdbcTemplate.queryForObject(
                        "SELECT status FROM student_forms WHERE id = ?", String.class, leaveFormId))
                .isEqualTo("REJECTED");
    }

    @Test
    void aStudentCannotReachTheHomeroomQueue() throws Exception {
        String studentToken = authService
                .login("0912345678", "Student@123", UserRole.STUDENT)
                .accessToken();

        mockMvc.perform(get("/api/v1/teacher/homeroom-forms")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
                .andExpect(status().isForbidden());
    }
}
