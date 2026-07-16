package vn.edu.fpt.myschool.clubs;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.hasItem;

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
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
class ClubsIntegrationTest {
    private static final UUID OPEN = UUID.fromString("00000000-0000-0000-0000-000000001901");
    private static final UUID ACTIVE = UUID.fromString("00000000-0000-0000-0000-000000001902");
    private static final UUID PENDING = UUID.fromString("00000000-0000-0000-0000-000000001903");
    private static final UUID FULL = UUID.fromString("00000000-0000-0000-0000-000000001904");
    private static final UUID GRADE_ELEVEN = UUID.fromString("00000000-0000-0000-0000-000000001905");

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRESQL =
            new PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine"));

    @Autowired
    private MockMvc mockMvc;

    @Test
    void requiresAuthenticationForClubRoutes() throws Exception {
        mockMvc.perform(get("/api/v1/clubs")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/clubs/{id}/applications", OPEN))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/v1/clubs/{id}/applications", OPEN))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listsOnlyVisibleClubsWithStudentMembershipStateAndCategoryFilter() throws Exception {
        String token = login("0912345678");
        mockMvc.perform(get("/api/v1/clubs").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clubs.length()").value(4))
                .andExpect(jsonPath("$.clubs[*].id").value(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.hasItem(GRADE_ELEVEN.toString()))))
                .andExpect(jsonPath("$.clubs[?(@.id == '%s')].membershipStatus".formatted(ACTIVE))
                        .value(hasItem("ACTIVE")))
                .andExpect(jsonPath("$.clubs[?(@.id == '%s')].membershipStatus".formatted(PENDING))
                        .value(hasItem("PENDING")));

        mockMvc.perform(get("/api/v1/clubs")
                        .param("category", "ACADEMIC")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clubs.length()").value(1))
                .andExpect(jsonPath("$.clubs[0].id").value(OPEN.toString()));
    }

    @Test
    void hidesGradeScopedClubDetailsWithoutDisclosingExistence() throws Exception {
        mockMvc.perform(get("/api/v1/clubs/{id}", GRADE_ELEVEN)
                        .param("studentId", "00000000-0000-0000-0000-000000000211")
                        .header(HttpHeaders.AUTHORIZATION, bearer(login("0912345678"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CLUB_NOT_FOUND"));
    }

    @Test
    void createsWithdrawsAndReactivatesAnApplication() throws Exception {
        String token = login("0912345678");
        mockMvc.perform(post("/api/v1/clubs/{id}/applications", OPEN)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.membershipStatus").value("PENDING"))
                .andExpect(jsonPath("$.canWithdraw").value(true));
        mockMvc.perform(delete("/api/v1/clubs/{id}/applications", OPEN)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.membershipStatus").value("WITHDRAWN"))
                .andExpect(jsonPath("$.canApply").value(true));
        mockMvc.perform(post("/api/v1/clubs/{id}/applications", OPEN)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.membershipStatus").value("PENDING"));
    }

    @Test
    void rejectsDuplicateAndCapacityBlockedApplications() throws Exception {
        String token = login("0912345678");
        mockMvc.perform(post("/api/v1/clubs/{id}/applications", PENDING)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CLUB_ALREADY_APPLIED"));
        mockMvc.perform(post("/api/v1/clubs/{id}/applications", FULL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CLUB_CAPACITY_REACHED"));
    }

    private String login(String phone) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phoneNumber":"%s","password":"Student@123"}
                                """.formatted(phone)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
