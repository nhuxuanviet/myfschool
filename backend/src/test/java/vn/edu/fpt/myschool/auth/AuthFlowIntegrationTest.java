package vn.edu.fpt.myschool.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
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
class AuthFlowIntegrationTest {

    private static final String PHONE_NUMBER = "0912345678";
    private static final String PASSWORD = "Student@123";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRESQL =
            new PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtDecoder jwtDecoder;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void logsInWithNormalizedPhoneAndReturnsTheExactStudentContract() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phoneNumber":"+84 912 345 678","password":"Student@123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.expiresIn").value(600))
                .andExpect(jsonPath("$.student.id").value(
                        "00000000-0000-0000-0000-000000000201"))
                .andExpect(jsonPath("$.student.studentCode").value("SE1913001"))
                .andExpect(jsonPath("$.student.fullName").value("Nhữ Xuân Việt"))
                .andExpect(jsonPath("$.student.gradeLevel").value(10))
                .andExpect(jsonPath("$.student.className").value("10A1"))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        String accessToken = JsonPath.read(response, "$.accessToken");
        String refreshToken = JsonPath.read(response, "$.refreshToken");
        entityManager.flush();
        String persistedHash = jdbcTemplate.queryForObject(
                "SELECT token_hash FROM refresh_sessions", String.class);

        assertThat(jwtDecoder.decode(accessToken).getClaimAsString("role"))
                .isEqualTo("STUDENT");
        assertThat(persistedHash).hasSize(64).isNotEqualTo(refreshToken);
    }

    @Test
    void rejectsKnownAndUnknownAccountsWithTheSameCredentialsError() throws Exception {
        String knownResponse = performFailedLogin(PHONE_NUMBER, "Wrong@123");
        String unknownResponse = performFailedLogin("0988888888", "Wrong@123");
        String oversizedUnicodeResponse = performFailedLogin(PHONE_NUMBER, "界".repeat(30));

        assertThat(JsonPath.<String>read(knownResponse, "$.code"))
                .isEqualTo("INVALID_CREDENTIALS")
                .isEqualTo(JsonPath.read(unknownResponse, "$.code"));
        assertThat(JsonPath.<String>read(knownResponse, "$.detail"))
                .isEqualTo(JsonPath.read(unknownResponse, "$.detail"));
        assertThat(JsonPath.<String>read(oversizedUnicodeResponse, "$.code"))
                .isEqualTo("INVALID_CREDENTIALS");
    }

    @Test
    void rotatesRefreshTokensAndRevokesTheFamilyWhenAnOldTokenIsReplayed()
            throws Exception {
        String oldRefreshToken = loginAndRead("$.refreshToken");
        String rotatedResponse = refresh(oldRefreshToken, 200);
        String newRefreshToken = JsonPath.read(rotatedResponse, "$.refreshToken");

        assertThat(newRefreshToken).isNotEqualTo(oldRefreshToken);
        refresh(oldRefreshToken, 401);
        String revokedResponse = refresh(newRefreshToken, 401);
        assertThat(JsonPath.<String>read(revokedResponse, "$.code"))
                .isEqualTo("INVALID_REFRESH_TOKEN");
    }

    @Test
    void logoutIsIdempotentAndRevokesTheRefreshFamily() throws Exception {
        String refreshToken = loginAndRead("$.refreshToken");
        String body = json(Map.of("refreshToken", refreshToken));

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());
        refresh(refreshToken, 401);
    }

    @Test
    void completesPasswordResetOnceAndRevokesExistingSessions() throws Exception {
        String existingRefreshToken = loginAndRead("$.refreshToken");
        String challengeId = requestReset(PHONE_NUMBER);
        String resetToken = verifyReset(challengeId, "123456", 200);
        entityManager.flush();
        String persistedResetHash = jdbcTemplate.queryForObject(
                "SELECT reset_token_hash FROM password_reset_challenges WHERE id = ?::uuid",
                String.class,
                challengeId);
        assertThat(persistedResetHash).hasSize(64).isNotEqualTo(resetToken);

        mockMvc.perform(post("/api/v1/auth/password-reset/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "resetToken", resetToken,
                                "newPassword", "NewStudent@456"))))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/v1/auth/password-reset/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "resetToken", resetToken,
                                "newPassword", "Another@789"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("RESET_TOKEN_INVALID"));

        performFailedLogin(PHONE_NUMBER, PASSWORD);
        performSuccessfulLogin(PHONE_NUMBER, "NewStudent@456");
        refresh(existingRefreshToken, 401);
    }

    @Test
    void resetRequestDoesNotRevealWhetherAnAccountExists() throws Exception {
        String knownChallenge = requestReset(PHONE_NUMBER);
        String unknownChallenge = requestReset("0988888888");

        assertThat(knownChallenge).isNotBlank().isNotEqualTo(unknownChallenge);
        String decoyResetToken = verifyReset(unknownChallenge, "123456", 200);
        mockMvc.perform(post("/api/v1/auth/password-reset/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "resetToken", decoyResetToken,
                                "newPassword", "DecoyUser@123"))))
                .andExpect(status().isNoContent());
    }

    @Test
    void rejectsInvalidOtpAndPreventsAChallengeFromBeingVerifiedTwice() throws Exception {
        String challengeId = requestReset(PHONE_NUMBER);

        verifyReset(challengeId, "000000", 400);
        verifyReset(challengeId, "123456", 200);
        String repeatedResponse = verifyReset(challengeId, "123456", 400);
        assertThat(JsonPath.<String>read(repeatedResponse, "$.code"))
                .isEqualTo("RESET_CHALLENGE_INVALID");
    }

    @Test
    void rejectsMissingOtpAtTheApiBoundary() throws Exception {
        mockMvc.perform(post("/api/v1/auth/password-reset/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"challengeId":"00000000-0000-0000-0000-000000000001"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("otp"));
    }

    @Test
    void resourceServerValidatesBearerTokensAndDeniesUnlistedEndpoints() throws Exception {
        String accessToken = loginAndRead("$.accessToken");

        mockMvc.perform(get("/api/v1/private-test"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        mockMvc.perform(get("/api/v1/private-test")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        mockMvc.perform(get("/api/v1/private-test")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    private String performFailedLogin(String phoneNumber, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("phoneNumber", phoneNumber, "password", password))))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private String performSuccessfulLogin(String phoneNumber, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("phoneNumber", phoneNumber, "password", password))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private String loginAndRead(String jsonPath) throws Exception {
        return JsonPath.read(performSuccessfulLogin(PHONE_NUMBER, PASSWORD), jsonPath);
    }

    private String refresh(String refreshToken, int expectedStatus) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("refreshToken", refreshToken))))
                .andExpect(status().is(expectedStatus))
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private String requestReset(String phoneNumber) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("phoneNumber", phoneNumber))))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.challengeId").isNotEmpty())
                .andExpect(jsonPath("$.expiresIn").value(600))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(response, "$.challengeId");
    }

    private String verifyReset(String challengeId, String otp, int expectedStatus)
            throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/password-reset/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("challengeId", challengeId, "otp", otp))))
                .andExpect(status().is(expectedStatus))
                .andReturn()
                .getResponse()
                .getContentAsString();
        if (expectedStatus == 200) {
            return JsonPath.read(response, "$.resetToken");
        }
        return response;
    }

    private static String json(Map<String, String> values) {
        return values.entrySet().stream()
                .map(entry -> "\"%s\":\"%s\"".formatted(entry.getKey(), entry.getValue()))
                .collect(java.util.stream.Collectors.joining(",", "{", "}"));
    }
}
