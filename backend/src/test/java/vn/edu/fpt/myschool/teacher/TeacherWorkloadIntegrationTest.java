package vn.edu.fpt.myschool.teacher;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
 * Proves the R2 acceptance gate: a teacher sees only what an assignment makes theirs.
 *
 * <p>Two teachers are set up against the same seeded classes so that "mine" and "someone else's"
 * are both real, rather than one teacher and an empty world.
 */
@Testcontainers
@ActiveProfiles({"test", "e2e"})
@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Transactional
class TeacherWorkloadIntegrationTest {

    private static final String OWN_PHONE = "0966000001";
    private static final String OTHER_PHONE = "0966000002";
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

    private UUID ownTeacherId;
    private UUID otherTeacherId;
    private UUID ownClassId;
    private UUID otherClassId;
    private UUID termId;

    private UUID createTeacherAccount(String phone, String code) {
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

    private void assign(UUID teacherId, UUID classId, UUID subjectId) {
        jdbcTemplate.update(
                """
                INSERT INTO teacher_subject_assignments
                    (id, teacher_id, class_id, subject_id, academic_term_id, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                UUID.randomUUID(), teacherId, classId, subjectId, termId);
    }

    private String tokenFor(String phone) {
        return authService.login(phone, PASSWORD, UserRole.TEACHER).accessToken();
    }

    @BeforeEach
    void setUp() {
        var classes = jdbcTemplate.queryForList(
                "SELECT id FROM school_classes ORDER BY code LIMIT 2", UUID.class);
        var subjects = jdbcTemplate.queryForList(
                "SELECT id FROM subjects ORDER BY code LIMIT 2", UUID.class);
        termId = jdbcTemplate.queryForObject("SELECT id FROM academic_terms LIMIT 1", UUID.class);
        ownClassId = classes.get(0);
        otherClassId = classes.get(1);

        ownTeacherId = createTeacherAccount(OWN_PHONE, "GVT001");
        otherTeacherId = createTeacherAccount(OTHER_PHONE, "GVT002");
        assign(ownTeacherId, ownClassId, subjects.get(0));
        assign(otherTeacherId, otherClassId, subjects.get(0));
    }

    @Test
    void listsOnlyTheClassesTheTeacherIsAssignedTo() throws Exception {
        mockMvc.perform(get("/api/v1/teacher/classes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(OWN_PHONE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].classId").value(ownClassId.toString()));
    }

    @Test
    void refusesTheStudentListOfAClassTheTeacherDoesNotTeach() throws Exception {
        // The class exists and has students; only the relationship is missing.
        mockMvc.perform(get("/api/v1/teacher/classes/" + otherClassId + "/students")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(OWN_PHONE)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_ASSIGNED_TO_CLASS"));
    }

    @Test
    void allowsTheStudentListOfAnAssignedClass() throws Exception {
        mockMvc.perform(get("/api/v1/teacher/classes/" + ownClassId + "/students")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(OWN_PHONE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    /**
     * Removing the assignment must take effect immediately: authorisation is the relationship,
     * so it cannot outlive it.
     */
    @Test
    void stopsAllowingTheClassOnceTheAssignmentIsRemoved() throws Exception {
        String token = tokenFor(OWN_PHONE);
        mockMvc.perform(get("/api/v1/teacher/classes/" + ownClassId + "/students")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());

        jdbcTemplate.update(
                "DELETE FROM teacher_subject_assignments WHERE teacher_id = ?", ownTeacherId);

        mockMvc.perform(get("/api/v1/teacher/classes/" + ownClassId + "/students")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void showsHomeroomOnlyToTheHomeroomTeacher() throws Exception {
        jdbcTemplate.update(
                """
                INSERT INTO homeroom_assignments
                    (id, teacher_id, class_id, academic_year_id, created_at, updated_at)
                SELECT ?, ?, ?, academic_year_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                FROM school_classes WHERE id = ?
                """,
                UUID.randomUUID(), ownTeacherId, ownClassId, ownClassId);

        mockMvc.perform(get("/api/v1/teacher/homerooms")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(OWN_PHONE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].classId").value(ownClassId.toString()));

        // A teacher who is nobody's homeroom teacher gets an empty list, not someone else's.
        mockMvc.perform(get("/api/v1/teacher/homerooms")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(OTHER_PHONE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    /** Homeroom duty alone is enough to see the class roll, without teaching a subject in it. */
    @Test
    void letsAHomeroomTeacherSeeTheirClassWithoutTeachingIt() throws Exception {
        jdbcTemplate.update(
                """
                INSERT INTO homeroom_assignments
                    (id, teacher_id, class_id, academic_year_id, created_at, updated_at)
                SELECT ?, ?, ?, academic_year_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                FROM school_classes WHERE id = ?
                """,
                UUID.randomUUID(), otherTeacherId, ownClassId, ownClassId);

        mockMvc.perform(get("/api/v1/teacher/classes/" + ownClassId + "/students")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(OTHER_PHONE)))
                .andExpect(status().isOk());
    }

    @Test
    void aStudentCannotReachTheTeacherNamespace() throws Exception {
        String studentToken = authService
                .login("0912345678", "Student@123", UserRole.STUDENT)
                .accessToken();

        mockMvc.perform(get("/api/v1/teacher/classes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/teacher/schedule")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
                .andExpect(status().isForbidden());
    }

    /** A teacher account whose profile was disabled has no teaching identity left to act as. */
    @Test
    void refusesATeacherWhoseProfileWasDisabled() throws Exception {
        String token = tokenFor(OWN_PHONE);
        jdbcTemplate.update(
                "UPDATE teacher_profiles SET enabled = FALSE WHERE id = ?", ownTeacherId);

        mockMvc.perform(get("/api/v1/teacher/classes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("TEACHER_PROFILE_MISSING"));
    }

    @Test
    void returnsAScheduleAnchoredOnMonday() throws Exception {
        mockMvc.perform(get("/api/v1/teacher/schedule")
                        .param("weekStart", "2026-03-11")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(OWN_PHONE)))
                .andExpect(status().isOk())
                // 2026-03-11 is a Wednesday; the week it belongs to starts on the Monday.
                .andExpect(jsonPath("$.weekStart").value("2026-03-09"))
                .andExpect(jsonPath("$.lessons").isArray());
    }
}
