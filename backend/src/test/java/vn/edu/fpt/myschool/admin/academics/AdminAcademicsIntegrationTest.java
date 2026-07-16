package vn.edu.fpt.myschool.admin.academics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
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
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@ActiveProfiles({"test", "e2e"})
@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
class AdminAcademicsIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRESQL =
            new PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createsAndUpdatesAStudentWithPaginationAndAudit() throws Exception {
        String adminToken = login("/api/v1/admin/auth/login", "0900000000", "Admin@123");
        MvcResult catalog = mockMvc.perform(get("/api/v1/admin/academics")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.academicYears").isArray())
                .andExpect(jsonPath("$.classes").isArray())
                .andReturn();
        String catalogJson = catalog.getResponse().getContentAsString();
        String academicYearId = JsonPath.read(catalogJson, "$.academicYears[0].id");

        String classCode = "10Z" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        MvcResult createdClass = mockMvc.perform(post("/api/v1/admin/academics/classes")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "academicYearId":"%s",
                                  "code":"%s",
                                  "name":"Lớp thử nghiệm",
                                  "gradeLevel":10,
                                  "enabled":true,
                                  "version":0
                                }
                                """.formatted(academicYearId, classCode)))
                .andExpect(status().isCreated())
                .andReturn();
        String classId = JsonPath.read(
                createdClass.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/v1/admin/students")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());

        String studentCode = "E2E" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        MvcResult createdStudent = mockMvc.perform(post("/api/v1/admin/students")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phoneNumber":"0988888888",
                                  "initialPassword":"Student@123",
                                  "studentCode":"%s",
                                  "fullName":"Nguyễn Học Sinh Mới",
                                  "classId":"%s"
                                }
                                """.formatted(studentCode, classId)))
                .andExpect(status().isCreated())
                .andReturn();
        String studentId = JsonPath.read(
                createdStudent.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/v1/admin/students")
                        .param("query", studentCode)
                        .param("page", "0")
                        .param("size", "10")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].studentCode").value(studentCode))
                .andExpect(jsonPath("$.items[0].classCode").value(classCode))
                .andExpect(jsonPath("$.items[0].version").value(0));

        String updatedName = "Nguyễn Học Sinh Đã Sửa";
        mockMvc.perform(put("/api/v1/admin/students/{studentId}", studentId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phoneNumber":"0988888888",
                                  "studentCode":"%s",
                                  "fullName":"%s",
                                  "classId":"%s",
                                  "enabled":true,
                                  "version":0
                                }
                                """.formatted(studentCode, updatedName, classId)))
                .andExpect(status().isNoContent());

        mockMvc.perform(put("/api/v1/admin/students/{studentId}", studentId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phoneNumber":"0988888888",
                                  "studentCode":"%s",
                                  "fullName":"Ghi đè cũ",
                                  "classId":"%s",
                                  "enabled":true,
                                  "version":0
                                }
                                """.formatted(studentCode, classId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));

        String studentToken = login("/api/v1/auth/login", "0988888888", "Student@123");
        assertThat(studentToken).isNotBlank();
        mockMvc.perform(get("/api/v1/home")
                        .header(HttpHeaders.AUTHORIZATION, bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.student.fullName").value(updatedName))
                .andExpect(jsonPath("$.student.className").value(classCode));

        Integer auditCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM admin_audit_events WHERE entity_id IN (?, ?)",
                Integer.class,
                UUID.fromString(classId),
                UUID.fromString(studentId));
        assertThat(auditCount).isEqualTo(3);
    }

    @Test
    void validatesUniquenessAndDeniesStudentAccess() throws Exception {
        String adminToken = login("/api/v1/admin/auth/login", "0900000000", "Admin@123");
        mockMvc.perform(post("/api/v1/admin/academics/subjects")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"TOAN\",\"name\":\"Trùng mã\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));

        String studentToken = login("/api/v1/auth/login", "0912345678", "Student@123");
        mockMvc.perform(get("/api/v1/admin/students")
                        .header(HttpHeaders.AUTHORIZATION, bearer(studentToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updatesAndDeletesUnreferencedCatalogRecordsButProtectsReferencedOnes() throws Exception {
        String adminToken = login("/api/v1/admin/auth/login", "0900000000", "Admin@123");
        String code = "ELECTIVE_" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        MvcResult created = mockMvc.perform(post("/api/v1/admin/academics/subjects")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"%s","name":"Môn tự chọn"}
                                """.formatted(code)))
                .andExpect(status().isCreated())
                .andReturn();
        String subjectId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(put("/api/v1/admin/academics/subjects/{subjectId}", subjectId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"%s","name":"Môn tự chọn cập nhật","enabled":true,"version":0}
                                """.formatted(code)))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/v1/admin/academics/subjects/{subjectId}", subjectId)
                        .param("version", "1")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isNoContent());

        MvcResult catalog = mockMvc.perform(get("/api/v1/admin/academics")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andReturn();
        String referencedClassId = JsonPath.read(
                catalog.getResponse().getContentAsString(), "$.classes[0].id");
        int referencedClassVersion = JsonPath.read(
                catalog.getResponse().getContentAsString(), "$.classes[0].version");
        mockMvc.perform(delete("/api/v1/admin/academics/classes/{classId}", referencedClassId)
                        .param("version", String.valueOf(referencedClassVersion))
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    private String login(String path, String phoneNumber, String password) throws Exception {
        String response = mockMvc.perform(post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phoneNumber":"%s","password":"%s"}
                                """.formatted(phoneNumber, password)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(response, "$.accessToken");
    }

    private static String bearer(String accessToken) {
        return "Bearer " + accessToken;
    }
}
