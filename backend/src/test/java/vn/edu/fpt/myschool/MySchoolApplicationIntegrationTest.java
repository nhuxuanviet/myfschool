package vn.edu.fpt.myschool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@ActiveProfiles({"test", "e2e"})
@SpringBootTest(properties = "springdoc.api-docs.enabled=true")
@AutoConfigureMockMvc
class MySchoolApplicationIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRESQL =
            new PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine"));

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void startsWithPostgreSqlAndAppliesFlywayMigrations() {
        assertThat(flyway.info().current()).isNotNull();
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("26");
        assertThat(flyway.info().pending()).isEmpty();

        Integer appliedBaseline = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM flyway_schema_history
                WHERE version = '1' AND success = TRUE
                """,
                Integer.class);
        String databaseVersion = jdbcTemplate.queryForObject("SELECT version()", String.class);

        assertThat(appliedBaseline).isEqualTo(1);
        assertThat(databaseVersion).startsWith("PostgreSQL");
    }

    @Test
    void exposesHealthyActuatorAndVersionedSystemEndpoints() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
        mockMvc.perform(get("/api/v1/system/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void allowsConfiguredFlutterWebOriginToReadActuatorHealth() throws Exception {
        mockMvc.perform(options("/actuator/health")
                        .header(HttpHeaders.ORIGIN, "http://127.0.0.1:4173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        "http://127.0.0.1:4173"));
    }

    @Test
    void publishesTheVersionedApiContract() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("MySchool API"))
                .andExpect(jsonPath("$.info.version").value("v1"))
                .andExpect(jsonPath("$.paths['/api/v1/system/health']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/login']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/auth/login']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/dashboard']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/academics']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/students']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/operations/timetable']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/operations/grades']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/operations/audit']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/ai/settings']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/home']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/timetable']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/grades']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/events']").exists());
    }

    @Test
    void formatsFrameworkErrorsAsProblemDetails() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(
                        org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }
}
