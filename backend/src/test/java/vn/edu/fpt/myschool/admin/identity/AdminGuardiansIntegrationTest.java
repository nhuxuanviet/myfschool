package vn.edu.fpt.myschool.admin.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class AdminGuardiansIntegrationTest {

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

    private UUID seededStudentId() {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM students WHERE student_code = 'SE1913001'", UUID.class);
    }

    private String createParent(String body) throws Exception {
        return JsonPath.read(
                mockMvc.perform(post("/api/v1/admin/parents")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$.id");
    }

    private String linkTo(String parentId, UUID studentId, String relationship, int order)
            throws Exception {
        return JsonPath.read(
                mockMvc.perform(post("/api/v1/admin/parents/" + parentId + "/links")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"studentId":"%s","relationship":"%s","contactOrder":%d}
                                        """.formatted(studentId, relationship, order)))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$.id");
    }

    @Test
    void createsAGuardianWithASignInAccountAndTheParentRole() throws Exception {
        String id = createParent("""
                {"fullName":"Nguyễn Văn Cha","phoneNumber":"0988888881","initialPassword":"Parent@123"}
                """);

        Integer roles = jdbcTemplate.queryForObject(
                """
                SELECT count(*) FROM user_roles ur
                INNER JOIN parent_profiles p ON p.user_id = ur.user_id
                WHERE p.id = ? AND ur.role = 'PARENT'
                """,
                Integer.class,
                UUID.fromString(id));
        assertThat(roles).isEqualTo(1);

        // A guardian with an account can sign in, and lands in the parent role.
        assertThat(authService.login("0988888881", "Parent@123", null).activeRole())
                .isEqualTo(UserRole.PARENT);
    }

    @Test
    void recordsAGuardianWhoHasNoAccountYet() throws Exception {
        createParent("""
                {"fullName":"Trần Thị Mẹ"}
                """);

        mockMvc.perform(get("/api/v1/admin/parents")
                        .param("query", "Trần Thị Mẹ")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].hasAccount").value(false))
                .andExpect(jsonPath("$.items[0].linkedStudents").value(0));
    }

    @Test
    void refusesAnAccountOnAPhoneNumberThatIsAlreadyTaken() throws Exception {
        mockMvc.perform(post("/api/v1/admin/parents")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Trùng số","phoneNumber":"0912345678","initialPassword":"Parent@123"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PHONE_NUMBER_TAKEN"));
    }

    @Test
    void refusesAWeakInitialPassword() throws Exception {
        mockMvc.perform(post("/api/v1/admin/parents")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Mật khẩu yếu","phoneNumber":"0988888882","initialPassword":"123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WEAK_PASSWORD"));
    }

    @Test
    void linksAGuardianToAStudentAndCountsTheLink() throws Exception {
        String parentId = createParent("""
                {"fullName":"Nguyễn Văn Liên Kết"}
                """);
        linkTo(parentId, seededStudentId(), "FATHER", 1);

        mockMvc.perform(get("/api/v1/admin/parents/links")
                        .param("parentId", parentId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].relationship").value("FATHER"))
                .andExpect(jsonPath("$[0].studentCode").value("SE1913001"))
                .andExpect(jsonPath("$[0].inForce").value(true));

        mockMvc.perform(get("/api/v1/admin/parents")
                        .param("query", "Nguyễn Văn Liên Kết")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(jsonPath("$.items[0].linkedStudents").value(1));
    }

    @Test
    void refusesASecondLinkBetweenTheSameGuardianAndStudent() throws Exception {
        String parentId = createParent("""
                {"fullName":"Nguyễn Văn Trùng"}
                """);
        UUID studentId = seededStudentId();
        linkTo(parentId, studentId, "FATHER", 1);

        mockMvc.perform(post("/api/v1/admin/parents/" + parentId + "/links")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId":"%s","relationship":"GUARDIAN","contactOrder":2}
                                """.formatted(studentId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GUARDIAN_LINK_EXISTS"));
    }

    @Test
    void refusesALinkToAStudentThatDoesNotExist() throws Exception {
        String parentId = createParent("""
                {"fullName":"Nguyễn Văn Sai"}
                """);

        mockMvc.perform(post("/api/v1/admin/parents/" + parentId + "/links")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId":"%s","relationship":"FATHER","contactOrder":1}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STUDENT_NOT_FOUND"));
    }

    @Test
    void endingALinkKeepsItAsARecordRatherThanDeletingIt() throws Exception {
        String parentId = createParent("""
                {"fullName":"Nguyễn Văn Kết Thúc"}
                """);
        String linkId = linkTo(parentId, seededStudentId(), "GUARDIAN", 1);

        mockMvc.perform(delete("/api/v1/admin/parents/links/" + linkId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // Who could see a child's data, and until when, must remain answerable after the fact.
        Integer surviving = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM parent_student_links WHERE id = ? AND effective_to IS NOT NULL",
                Integer.class,
                UUID.fromString(linkId));
        assertThat(surviving).isEqualTo(1);

        mockMvc.perform(get("/api/v1/admin/parents/links")
                        .param("parentId", parentId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(jsonPath("$").isEmpty());

        mockMvc.perform(get("/api/v1/admin/parents/links")
                        .param("parentId", parentId)
                        .param("inForceOnly", "false")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(jsonPath("$[0].inForce").value(false));
    }

    @Test
    void refusesToEndALinkTwice() throws Exception {
        String parentId = createParent("""
                {"fullName":"Nguyễn Văn Hai Lần"}
                """);
        String linkId = linkTo(parentId, seededStudentId(), "FATHER", 1);

        mockMvc.perform(delete("/api/v1/admin/parents/links/" + linkId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/v1/admin/parents/links/" + linkId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GUARDIAN_LINK_NOT_IN_FORCE"));
    }

    @Test
    void aParentCannotReachTheGuardianDirectory() throws Exception {
        String parentId = createParent("""
                {"fullName":"Phụ huynh tò mò","phoneNumber":"0988888883","initialPassword":"Parent@123"}
                """);
        assertThat(parentId).isNotBlank();
        String parentToken = authService.login("0988888883", "Parent@123", null).accessToken();

        mockMvc.perform(get("/api/v1/admin/parents")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + parentToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/admin/parents/links")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + parentToken))
                .andExpect(status().isForbidden());
    }
}
