package vn.edu.fpt.myschool.admin.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@ActiveProfiles({"test", "e2e"})
@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Transactional
class AdminDashboardIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRESQL =
            new PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine"));

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsBoundedOperationalMetricsToAdministrators() throws Exception {
        String accessToken = login("0900000000", "Admin@123");

        MvcResult result = mockMvc.perform(get("/api/v1/admin/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metrics.totalStudents").isNumber())
                .andExpect(jsonPath("$.metrics.activeClasses").isNumber())
                .andExpect(jsonPath("$.metrics.pendingForms").isNumber())
                .andExpect(jsonPath("$.metrics.upcomingEvents").isNumber())
                .andExpect(jsonPath("$.metrics.pendingClubApplications").isNumber())
                .andExpect(jsonPath("$.metrics.recentlyUpdatedGrades").isNumber())
                .andExpect(jsonPath("$.recentActivities").isArray())
                .andExpect(jsonPath("$.generatedAt").isNotEmpty())
                .andReturn();

        int activityCount = JsonPath.read(
                result.getResponse().getContentAsString(),
                "$.recentActivities.length()");
        assertThat(activityCount).isBetween(1, 6);
    }

    @Test
    void deniesAnonymousAndStudentAccess() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard"))
                .andExpect(status().isUnauthorized());

        String studentToken = login("0912345678", "Student@123");
        mockMvc.perform(get("/api/v1/admin/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
                .andExpect(status().isForbidden());
    }

    private String login(String phoneNumber, String password) throws Exception {
        String path = phoneNumber.equals("0900000000")
                ? "/api/v1/admin/auth/login"
                : "/api/v1/auth/login";
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
}
