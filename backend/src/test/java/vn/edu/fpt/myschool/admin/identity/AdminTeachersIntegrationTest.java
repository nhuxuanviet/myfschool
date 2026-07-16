package vn.edu.fpt.myschool.admin.identity;

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import vn.edu.fpt.myschool.auth.application.AuthService;
import vn.edu.fpt.myschool.auth.domain.UserRole;

@Testcontainers
@ActiveProfiles({"test", "e2e"})
@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Transactional
class AdminTeachersIntegrationTest {

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

    private String adminToken;

    @BeforeEach
    void signInAsAdmin() {
        adminToken = authService
                .loginForRole("0900000000", "Admin@123", UserRole.ADMIN)
                .accessToken();
    }

    private String createTeacher(String code, String fullName) throws Exception {
        return JsonPath.read(
                mockMvc.perform(post("/api/v1/admin/teachers")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"teacherCode":"%s","fullName":"%s","email":"gv@fschool.edu.vn"}
                                        """.formatted(code, fullName)))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$.id");
    }

    @Test
    void createsATeacherWithoutAnAccountAndRecordsWhoDidIt() throws Exception {
        String id = createTeacher("GV100", "Nguyễn Thị Lan");

        // A newly entered teacher has no account: the school adds the roster first and issues
        // credentials later, and the list must say so rather than hide them.
        mockMvc.perform(get("/api/v1/admin/teachers")
                        .param("query", "GV100")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].teacherCode").value("GV100"))
                .andExpect(jsonPath("$.items[0].hasAccount").value(false))
                .andExpect(jsonPath("$.items[0].phoneNumber").doesNotExist())
                .andExpect(jsonPath("$.totalElements").value(1));

        Integer audits = jdbcTemplate.queryForObject(
                """
                SELECT count(*) FROM admin_audit_events
                WHERE entity_type = 'TEACHER' AND entity_id = ? AND action = 'CREATE'
                  AND actor_user_id IS NOT NULL
                """,
                Integer.class,
                UUID.fromString(id));
        assertThat(audits).isEqualTo(1);
    }

    @Test
    void refusesATeacherCodeThatIsAlreadyInUse() throws Exception {
        createTeacher("GV200", "Trần Văn A");

        mockMvc.perform(post("/api/v1/admin/teachers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"teacherCode":"gv200","fullName":"Lê Thị B"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TEACHER_CODE_TAKEN"));
    }

    @Test
    void refusesAnUpdateBuiltOnAStaleRead() throws Exception {
        String id = createTeacher("GV300", "Phạm Văn Hùng");

        mockMvc.perform(put("/api/v1/admin/teachers/" + id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"teacherCode":"GV300","fullName":"Phạm Văn Hùng","enabled":true,"version":0}
                                """))
                .andExpect(status().isOk());

        // Version 0 has already been consumed above, so this second writer is working from a
        // read that no longer reflects the row and must be refused rather than overwrite.
        mockMvc.perform(put("/api/v1/admin/teachers/" + id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"teacherCode":"GV300","fullName":"Ghi đè","enabled":true,"version":0}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TEACHER_VERSION_CONFLICT"));
    }

    @Test
    void reportsAMissingTeacher() throws Exception {
        mockMvc.perform(put("/api/v1/admin/teachers/" + UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"teacherCode":"GV999","fullName":"Không tồn tại","enabled":true,"version":0}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TEACHER_NOT_FOUND"));
    }

    @Test
    void filtersTeachersByWhetherTheyCanSignIn() throws Exception {
        createTeacher("GV400", "Chưa có tài khoản");

        mockMvc.perform(get("/api/v1/admin/teachers")
                        .param("hasAccount", "false")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/v1/admin/teachers")
                        .param("hasAccount", "true")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void aStudentCannotReadOrChangeTheTeacherRoster() throws Exception {
        String studentToken = authService
                .login("0912345678", "Student@123", UserRole.STUDENT)
                .accessToken();

        mockMvc.perform(get("/api/v1/admin/teachers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/admin/teachers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"teacherCode":"GV666","fullName":"Kẻ mạo danh"}
                                """))
                .andExpect(status().isForbidden());
    }
}
