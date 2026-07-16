package vn.edu.fpt.myschool.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
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
class AdminAuthIntegrationTest {

    private static final String ADMIN_PHONE = "0900000000";
    private static final String ADMIN_PASSWORD = "Admin@123";
    private static final String STUDENT_PHONE = "0912345678";
    private static final String STUDENT_PASSWORD = "Student@123";
    private static final String REFRESH_COOKIE = "MYSCHOOL_ADMIN_REFRESH";
    private static final String CSRF_COOKIE = "MYSCHOOL_ADMIN_CSRF";
    private static final String CSRF_HEADER = "X-CSRF-Token";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRESQL =
            new PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void logsInWithoutExposingTheRefreshTokenAndIssuesAnAdminJwt() throws Exception {
        MvcResult result = adminLogin(ADMIN_PHONE, ADMIN_PASSWORD, 200);

        String body = result.getResponse().getContentAsString();
        String accessToken = JsonPath.read(body, "$.accessToken");
        Cookie refreshCookie = requireCookie(result, REFRESH_COOKIE);
        Cookie csrfCookie = requireCookie(result, CSRF_COOKIE);
        Jwt jwt = jwtDecoder.decode(accessToken);

        assertThat(body).doesNotContain("refreshToken").doesNotContain(refreshCookie.getValue());
        assertThat(JsonPath.<String>read(body, "$.csrfToken")).isEqualTo(csrfCookie.getValue());
        assertThat(jwt.getClaimAsString("role")).isEqualTo("ADMIN");
        assertThat(jwt.hasClaim("adminId")).isTrue();
        assertThat(jwt.hasClaim("studentId")).isFalse();
        assertThat(result.getResponse().getHeaders(HttpHeaders.SET_COOKIE))
                .anySatisfy(header -> assertThat(header)
                        .contains(REFRESH_COOKIE + "=")
                        .contains("HttpOnly")
                        .contains("SameSite=Strict"))
                .anySatisfy(header -> assertThat(header)
                        .contains(CSRF_COOKIE + "=")
                        .doesNotContain("HttpOnly"));
    }

    @Test
    void keepsStudentAndAdministratorLoginBoundariesSeparate() throws Exception {
        adminLogin(STUDENT_PHONE, STUDENT_PASSWORD, 401);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(ADMIN_PHONE, ADMIN_PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void protectsAdminResourcesByRole() throws Exception {
        String adminToken = JsonPath.read(
                adminLogin(ADMIN_PHONE, ADMIN_PASSWORD, 200)
                        .getResponse().getContentAsString(),
                "$.accessToken");
        String studentToken = JsonPath.read(
                mockMvc.perform(post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(credentials(STUDENT_PHONE, STUDENT_PASSWORD)))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse().getContentAsString(),
                "$.accessToken");

        mockMvc.perform(get("/api/v1/admin/auth/me"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/admin/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void requiresCsrfAndRotatesTheRefreshSession() throws Exception {
        MvcResult login = adminLogin(ADMIN_PHONE, ADMIN_PASSWORD, 200);
        Cookie refreshCookie = requireCookie(login, REFRESH_COOKIE);
        Cookie csrfCookie = requireCookie(login, CSRF_COOKIE);

        mockMvc.perform(post("/api/v1/admin/auth/refresh")
                        .cookie(refreshCookie, csrfCookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("INVALID_CSRF_TOKEN"));

        MvcResult refreshed = mockMvc.perform(post("/api/v1/admin/auth/refresh")
                        .cookie(refreshCookie, csrfCookie)
                        .header(CSRF_HEADER, csrfCookie.getValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

        assertThat(requireCookie(refreshed, REFRESH_COOKIE).getValue())
                .isNotEqualTo(refreshCookie.getValue());
        assertThat(requireCookie(refreshed, CSRF_COOKIE).getValue())
                .isNotEqualTo(csrfCookie.getValue());
    }

    @Test
    void recordsSecurityEventsWithoutPersistingThePhoneNumber() throws Exception {
        adminLogin(ADMIN_PHONE, ADMIN_PASSWORD, 200);
        adminLogin("0900000001", "Wrong@123", 401);
        adminLogin("0900000001", "WrongAgain@123", 401);

        Integer eventCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM security_audit_events", Integer.class);
        Integer rawPhoneCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM security_audit_events "
                        + "WHERE identifier_hash IN (?, ?)",
                Integer.class,
                ADMIN_PHONE,
                "0900000001");

        assertThat(eventCount).isGreaterThanOrEqualTo(3);
        assertThat(rawPhoneCount).isZero();
    }

    private MvcResult adminLogin(String phone, String password, int expectedStatus)
            throws Exception {
        return mockMvc.perform(post("/api/v1/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(phone, password)))
                .andExpect(status().is(expectedStatus))
                .andExpect(content().contentTypeCompatibleWith(
                        expectedStatus == 200
                                ? MediaType.APPLICATION_JSON
                                : MediaType.APPLICATION_PROBLEM_JSON))
                .andReturn();
    }

    private static Cookie requireCookie(MvcResult result, String name) {
        Cookie cookie = result.getResponse().getCookie(name);
        assertThat(cookie).as("response cookie %s", name).isNotNull();
        return cookie;
    }

    private static String credentials(String phone, String password) {
        return "{\"phoneNumber\":\"%s\",\"password\":\"%s\"}"
                .formatted(phone, password);
    }
}
