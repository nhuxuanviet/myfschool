package vn.edu.fpt.myschool.admin.operations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
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
class AdminOperationsIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRESQL =
            new PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void managesTimetableAndPublishesStudentVisibleContentWithAudit() throws Exception {
        String adminToken = login("/api/v1/admin/auth/login", "0900000000", "Admin@123");
        Map<String, Object> references = jdbcTemplate.queryForMap("""
                SELECT term.id AS term_id, school_class.id AS class_id, subject.id AS subject_id
                FROM academic_terms term
                CROSS JOIN LATERAL (
                    SELECT id FROM school_classes
                    WHERE academic_year_id = term.academic_year_id ORDER BY code LIMIT 1
                ) school_class
                CROSS JOIN LATERAL (
                    SELECT id FROM subjects WHERE enabled = TRUE ORDER BY code LIMIT 1
                ) subject
                ORDER BY term.starts_on DESC
                LIMIT 1
                """);
        UUID termId = (UUID) references.get("term_id");
        UUID classId = (UUID) references.get("class_id");
        UUID subjectId = (UUID) references.get("subject_id");

        MvcResult lessonResult = mockMvc.perform(post("/api/v1/admin/operations/timetable/lessons")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "academicTermId":"%s","schoolClassId":"%s",
                                  "dayOfWeek":7,"session":"AFTERNOON","periodNumber":5,
                                  "subjectId":"%s","teacherName":"Cô Mai","room":"P.A4"
                                }
                                """.formatted(termId, classId, subjectId)))
                .andExpect(status().isCreated())
                .andReturn();
        String lessonId = JsonPath.read(lessonResult.getResponse().getContentAsString(), "$.id");
        mockMvc.perform(get("/api/v1/admin/operations/timetable")
                        .param("academicTermId", termId.toString())
                        .param("schoolClassId", classId.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lessons[?(@.id == '%s')]".formatted(lessonId)).exists());

        String announcementTitle = "Thông báo E2E " + UUID.randomUUID().toString().substring(0, 6);
        MvcResult announcementResult = mockMvc.perform(post("/api/v1/admin/operations/announcements")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"%s","body":"Nội dung kiểm thử quản trị",
                                  "audience":"ALL","audienceGradeLevel":null,
                                  "publishedAt":"2026-07-14T00:00:00Z",
                                  "visibleFrom":"2026-01-01T00:00:00Z",
                                  "visibleUntil":"2027-01-01T00:00:00Z"
                                }
                                """.formatted(announcementTitle)))
                .andExpect(status().isCreated())
                .andReturn();
        String announcementId = JsonPath.read(
                announcementResult.getResponse().getContentAsString(), "$.id");

        String studentToken = login("/api/v1/auth/login", "0912345678", "Student@123");
        mockMvc.perform(get("/api/v1/home")
                        .header(HttpHeaders.AUTHORIZATION, bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.announcements[?(@.title == '%s')]".formatted(announcementTitle)).exists());

        Integer auditCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM admin_audit_events
                WHERE entity_id IN (?, ?)
                """, Integer.class, UUID.fromString(lessonId), UUID.fromString(announcementId));
        assertThat(auditCount).isEqualTo(2);
    }

    @Test
    void recordsGradesProcessesFormsAndExportsAudit() throws Exception {
        String adminToken = login("/api/v1/admin/auth/login", "0900000000", "Admin@123");
        Map<String, Object> enrollment = jdbcTemplate.queryForMap("""
                SELECT enrollment.id, enrollment.student_id, enrollment.academic_term_id,
                       COALESCE(MAX(assessment.display_order), 0) + 1 AS next_order
                FROM student_term_subjects enrollment
                LEFT JOIN grade_assessments assessment
                    ON assessment.student_term_subject_id = enrollment.id
                GROUP BY enrollment.id
                ORDER BY enrollment.updated_at DESC
                LIMIT 1
                """);
        UUID enrollmentId = (UUID) enrollment.get("id");
        int displayOrder = ((Number) enrollment.get("next_order")).intValue();
        String label = "E2E " + UUID.randomUUID().toString().substring(0, 6);
        mockMvc.perform(post("/api/v1/admin/operations/grades/assessments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "studentTermSubjectId":"%s","assessmentKind":"REGULAR",
                                  "assessmentForm":"WRITTEN","displayLabel":"%s",
                                  "durationMinutes":15,"status":"RECORDED","score":9.0,
                                  "outcome":null,"assessedOn":"2026-07-15","displayOrder":%d
                                }
                                """.formatted(enrollmentId, label, displayOrder)))
                .andExpect(status().isCreated());

        Map<String, Object> form = jdbcTemplate.queryForMap("""
                SELECT id, version FROM student_forms
                WHERE status IN ('SUBMITTED', 'IN_REVIEW')
                ORDER BY submitted_at LIMIT 1
                """);
        mockMvc.perform(patch("/api/v1/admin/operations/forms/{id}/status", form.get("id"))
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"APPROVED","note":"Đã kiểm tra","version":%s}
                                """.formatted(form.get("version"))))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/admin/operations/audit/export")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(result.getResponse().getContentType())
                        .startsWith("text/csv"))
                .andExpect(result -> assertThat(result.getResponse().getContentAsString())
                        .contains("Quản trị viên"));
    }

    @Test
    void deniesStudentAccessToEveryOperationsResource() throws Exception {
        String studentToken = login("/api/v1/auth/login", "0912345678", "Student@123");
        mockMvc.perform(get("/api/v1/admin/operations/forms")
                        .header(HttpHeaders.AUTHORIZATION, bearer(studentToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/operations/audit")
                        .header(HttpHeaders.AUTHORIZATION, bearer(studentToken)))
                .andExpect(status().isForbidden());
    }

    private String login(String path, String phoneNumber, String password) throws Exception {
        String response = mockMvc.perform(post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phoneNumber":"%s","password":"%s"}
                                """.formatted(phoneNumber, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.accessToken");
    }

    private static String bearer(String accessToken) {
        return "Bearer " + accessToken;
    }
}
