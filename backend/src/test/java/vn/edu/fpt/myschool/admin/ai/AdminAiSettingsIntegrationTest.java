package vn.edu.fpt.myschool.admin.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.jdbc.core.JdbcTemplate;
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
class AdminAiSettingsIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRESQL =
            new PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void readsAndUpdatesSafeRuntimeSettingsWithOptimisticLockingAndAudit() throws Exception {
        String token = login("/api/v1/admin/auth/login", "0900000000", "Admin@123");
        MvcResult current = mockMvc.perform(get("/api/v1/admin/ai/settings")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("LOCAL"))
                .andExpect(jsonPath("$.status").value("LOCAL_FALLBACK"))
                .andExpect(jsonPath("$.apiKeyConfigured").isBoolean())
                .andExpect(jsonPath("$.apiKey").doesNotExist())
                .andExpect(jsonPath("$.model").isString())
                .andExpect(jsonPath("$.version").isNumber())
                .andReturn();
        int version = JsonPath.read(current.getResponse().getContentAsString(), "$.version");

        mockMvc.perform(put("/api/v1/admin/ai/settings")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "model":"gpt-5.6-luna",
                                  "temperature":0.7,
                                  "maxCompletionTokens":900,
                                  "memoryMaxMessages":14,
                                  "version":%d
                                }
                                """.formatted(version)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.temperature").value(0.7))
                .andExpect(jsonPath("$.maxCompletionTokens").value(900))
                .andExpect(jsonPath("$.memoryMaxMessages").value(14))
                .andExpect(jsonPath("$.version").value(version + 1));

        Integer auditCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM admin_audit_events WHERE entity_type = 'ASSISTANT_SETTINGS'",
                Integer.class);
        assertThat(auditCount).isEqualTo(1);

        mockMvc.perform(put("/api/v1/admin/ai/settings")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "model":"gpt-5.6-luna",
                                  "temperature":0.7,
                                  "maxCompletionTokens":900,
                                  "memoryMaxMessages":14,
                                  "version":%d
                                }
                                """.formatted(version)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void rejectsInvalidSettingsAndDeniesStudentAccess() throws Exception {
        String adminToken = login("/api/v1/admin/auth/login", "0900000000", "Admin@123");
        mockMvc.perform(put("/api/v1/admin/ai/settings")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "model":"invalid model",
                                  "temperature":3,
                                  "maxCompletionTokens":99,
                                  "memoryMaxMessages":31,
                                  "version":0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        String studentToken = login("/api/v1/auth/login", "0912345678", "Student@123");
        mockMvc.perform(get("/api/v1/admin/ai/settings")
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
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(response, "$.accessToken");
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
