package vn.edu.fpt.myschool.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import vn.edu.fpt.myschool.auth.application.AuthException;
import vn.edu.fpt.myschool.auth.application.AuthService;
import vn.edu.fpt.myschool.auth.application.AuthenticationResult;
import vn.edu.fpt.myschool.auth.application.PasswordResetService;
import vn.edu.fpt.myschool.auth.application.PasswordResetDeliveryUnavailableException;
import vn.edu.fpt.myschool.auth.application.port.OtpDeliveryException;
import vn.edu.fpt.myschool.auth.application.port.OtpDeliveryPort;
import vn.edu.fpt.myschool.auth.application.port.PasswordResetRateLimitStore;
import vn.edu.fpt.myschool.auth.application.port.RefreshSessionStore;
import vn.edu.fpt.myschool.auth.application.port.SecretTokenService;
import vn.edu.fpt.myschool.auth.application.port.UserAccountStore;
import vn.edu.fpt.myschool.auth.domain.RefreshSession;

@Testcontainers
@ActiveProfiles({"test", "e2e"})
@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@TestPropertySource(properties = "app.security.auth.max-reset-requests-per-window=2")
class AuthConcurrencyIntegrationTest {

    private static final String PHONE_NUMBER = "0912345678";
    private static final String UNKNOWN_PHONE_NUMBER = "0988888888";
    private static final String ORIGINAL_PASSWORD = "Student@123";
    private static final String RESET_PASSWORD = "ResetRace@456";
    private static final String LOGIN_THREAD = "concurrent-old-password-login";
    private static final String REFRESH_THREAD = "concurrent-refresh";
    private static final String RESET_THREAD = "concurrent-password-reset";
    private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(10);

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRESQL =
            new PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine"));

    @Autowired
    private AuthService authService;

    @Autowired
    private PasswordResetService passwordResetService;

    @Autowired
    private PasswordResetRateLimitStore rateLimitStore;

    @Autowired
    private SecretTokenService secretTokenService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockMvc mockMvc;

    @MockitoSpyBean
    private RefreshSessionStore refreshSessionStore;

    @MockitoSpyBean
    private UserAccountStore userAccountStore;

    @MockitoSpyBean
    private OtpDeliveryPort otpDeliveryPort;

    @BeforeEach
    void restoreSeedAccountAndClearAuthState() {
        jdbcTemplate.update("DELETE FROM password_reset_challenges");
        jdbcTemplate.update("DELETE FROM refresh_sessions");
        jdbcTemplate.update("DELETE FROM password_reset_rate_limits");
        int updated = jdbcTemplate.update(
                """
                UPDATE users
                SET password_hash = ?, credentials_updated_at = NOW(), updated_at = NOW()
                WHERE phone_number = ?
                """,
                passwordEncoder.encode(ORIGINAL_PASSWORD),
                PHONE_NUMBER);
        assertThat(updated).isEqualTo(1);
    }

    @Test
    void resetSerializesWithOldPasswordLoginAndLeavesNoActiveSession() throws Exception {
        String resetToken = createVerifiedResetToken();
        CountDownLatch loginReachedSessionCreation = new CountDownLatch(1);
        CountDownLatch allowLoginToFinish = new CountDownLatch(1);
        CountDownLatch resetAttemptedUserLock = new CountDownLatch(1);
        pauseSessionCreation(
                LOGIN_THREAD, loginReachedSessionCreation, allowLoginToFinish);
        signalUserLockAttempt(RESET_THREAD, resetAttemptedUserLock);

        CompletableFuture<AuthenticationResult> loginFuture =
                runAsync(LOGIN_THREAD, () -> authService.login(PHONE_NUMBER, ORIGINAL_PASSWORD));
        CompletableFuture<Void> resetFuture = null;
        try {
            await(loginReachedSessionCreation);
            resetFuture = runAsync(RESET_THREAD, () -> {
                passwordResetService.complete(resetToken, RESET_PASSWORD);
                return null;
            });
            await(resetAttemptedUserLock);

            assertThat(resetFuture).isNotDone();
        } finally {
            allowLoginToFinish.countDown();
        }

        AuthenticationResult loginResult = get(loginFuture);
        get(resetFuture);

        assertThat(activeRefreshSessionCount()).isZero();
        assertInvalidRefreshToken(loginResult.refreshToken());
        assertThatThrownBy(() -> authService.login(PHONE_NUMBER, ORIGINAL_PASSWORD))
                .isInstanceOf(AuthException.class)
                .extracting("code")
                .isEqualTo("INVALID_CREDENTIALS");
    }

    @Test
    void resetSerializesWithRefreshAndRevokesTheRotatedFamily() throws Exception {
        AuthenticationResult initialLogin =
                authService.login(PHONE_NUMBER, ORIGINAL_PASSWORD);
        String resetToken = createVerifiedResetToken();
        CountDownLatch refreshReachedConsumption = new CountDownLatch(1);
        CountDownLatch allowRefreshToFinish = new CountDownLatch(1);
        CountDownLatch resetAttemptedUserLock = new CountDownLatch(1);
        pauseSessionConsumption(
                REFRESH_THREAD, refreshReachedConsumption, allowRefreshToFinish);
        signalUserLockAttempt(RESET_THREAD, resetAttemptedUserLock);

        CompletableFuture<AuthenticationResult> refreshFuture = runAsync(
                REFRESH_THREAD,
                () -> authService.refresh(initialLogin.refreshToken()));
        CompletableFuture<Void> resetFuture = null;
        try {
            await(refreshReachedConsumption);
            resetFuture = runAsync(RESET_THREAD, () -> {
                passwordResetService.complete(resetToken, RESET_PASSWORD);
                return null;
            });
            await(resetAttemptedUserLock);

            assertThat(resetFuture).isNotDone();
        } finally {
            allowRefreshToFinish.countDown();
        }

        AuthenticationResult rotated = get(refreshFuture);
        get(resetFuture);

        assertThat(activeRefreshSessionCount()).isZero();
        assertInvalidRefreshToken(rotated.refreshToken());
    }

    @Test
    void resetRequestRateLimitIsIdenticalForKnownAndUnknownPhones() throws Exception {
        String knownProblem = exhaustResetRequestLimit(PHONE_NUMBER);
        String unknownProblem = exhaustResetRequestLimit(UNKNOWN_PHONE_NUMBER);

        assertThat(JsonPath.<String>read(knownProblem, "$.code"))
                .isEqualTo("PASSWORD_RESET_RATE_LIMITED")
                .isEqualTo(JsonPath.read(unknownProblem, "$.code"));
        assertThat(JsonPath.<String>read(knownProblem, "$.detail"))
                .isEqualTo(JsonPath.read(unknownProblem, "$.detail"));
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM password_reset_rate_limits WHERE request_count = 2",
                        Integer.class))
                .isEqualTo(2);
    }

    @Test
    void resetRequestRateLimitIsAtomicAcrossConcurrentCallers() throws Exception {
        int callers = 12;
        int maxRequests = 3;
        CountDownLatch callersReady = new CountDownLatch(callers);
        CountDownLatch start = new CountDownLatch(1);
        String phoneHash = secretTokenService.hash("phone:" + PHONE_NUMBER);
        Instant requestedAt = Instant.now();
        List<CompletableFuture<Boolean>> attempts = new ArrayList<>();

        for (int index = 0; index < callers; index++) {
            attempts.add(runAsync("rate-limit-caller-" + index, () -> {
                callersReady.countDown();
                await(start);
                return rateLimitStore.tryAcquire(
                        phoneHash,
                        requestedAt,
                        Duration.ofMinutes(15),
                        maxRequests);
            }));
        }
        await(callersReady);
        start.countDown();

        long granted = 0;
        for (CompletableFuture<Boolean> attempt : attempts) {
            if (get(attempt)) {
                granted++;
            }
        }

        assertThat(granted).isEqualTo(maxRequests);
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT request_count FROM password_reset_rate_limits WHERE phone_hash = ?",
                        Integer.class,
                        phoneHash))
                .isEqualTo(maxRequests);
    }

    @Test
    void resetRequestDeliversOtpOnlyForAnExistingAccount() {
        passwordResetService.request(PHONE_NUMBER);
        passwordResetService.request(UNKNOWN_PHONE_NUMBER);

        verify(otpDeliveryPort, times(1))
                .sendPasswordResetOtp(PHONE_NUMBER, "123456");
        verify(otpDeliveryPort, never())
                .sendPasswordResetOtp(UNKNOWN_PHONE_NUMBER, "123456");
    }

    @Test
    void resetRequestRollsBackPermitAndChallengeWhenDeliveryFails() {
        doThrow(new OtpDeliveryException(
                        "SMS gateway unavailable", new IllegalStateException("offline")))
                .when(otpDeliveryPort)
                .sendPasswordResetOtp(PHONE_NUMBER, "123456");

        assertThatThrownBy(() -> passwordResetService.request(PHONE_NUMBER))
                .isInstanceOf(PasswordResetDeliveryUnavailableException.class);
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM password_reset_challenges",
                        Integer.class))
                .isZero();
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM password_reset_rate_limits",
                        Integer.class))
                .isZero();
    }

    @Test
    void resetRequestKeepsTheGenericAcceptedResponseWhenDeliveryFails() throws Exception {
        doThrow(new OtpDeliveryException(
                        "SMS gateway unavailable", new IllegalStateException("offline")))
                .when(otpDeliveryPort)
                .sendPasswordResetOtp(PHONE_NUMBER, "123456");

        mockMvc.perform(post("/api/v1/auth/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"%s\"}".formatted(PHONE_NUMBER)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.challengeId").isNotEmpty())
                .andExpect(jsonPath("$.expiresIn").value(600));
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM password_reset_challenges",
                        Integer.class))
                .isZero();
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM password_reset_rate_limits",
                        Integer.class))
                .isZero();
    }

    private String createVerifiedResetToken() {
        var challenge = passwordResetService.request(PHONE_NUMBER);
        return passwordResetService.verify(challenge.challengeId(), "123456").resetToken();
    }

    private String exhaustResetRequestLimit(String phoneNumber) throws Exception {
        for (int attempt = 0; attempt < 2; attempt++) {
            mockMvc.perform(post("/api/v1/auth/password-reset/request")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"phoneNumber\":\"%s\"}".formatted(phoneNumber)))
                    .andExpect(status().isAccepted());
        }
        MvcResult result = mockMvc.perform(post("/api/v1/auth/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"%s\"}".formatted(phoneNumber)))
                .andExpect(status().isTooManyRequests())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("PASSWORD_RESET_RATE_LIMITED"))
                .andReturn();
        return result.getResponse().getContentAsString();
    }

    private void pauseSessionCreation(
            String threadName,
            CountDownLatch reached,
            CountDownLatch release) {
        doAnswer(invocation -> {
            if (threadName.equals(Thread.currentThread().getName())) {
                reached.countDown();
                await(release);
            }
            return invocation.callRealMethod();
        }).when(refreshSessionStore).create(any(RefreshSession.class));
    }

    private void pauseSessionConsumption(
            String threadName,
            CountDownLatch reached,
            CountDownLatch release) {
        doAnswer(invocation -> {
            if (threadName.equals(Thread.currentThread().getName())) {
                reached.countDown();
                await(release);
            }
            return invocation.callRealMethod();
        }).when(refreshSessionStore).markUsed(any(), any());
    }

    private void signalUserLockAttempt(String threadName, CountDownLatch attempted) {
        doAnswer(invocation -> {
            if (threadName.equals(Thread.currentThread().getName())) {
                attempted.countDown();
            }
            return invocation.callRealMethod();
        }).when(userAccountStore).findByIdForUpdate(any());
    }

    private void assertInvalidRefreshToken(String refreshToken) {
        assertThatThrownBy(() -> authService.refresh(refreshToken))
                .isInstanceOf(AuthException.class)
                .extracting("code")
                .isEqualTo("INVALID_REFRESH_TOKEN");
    }

    private int activeRefreshSessionCount() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM refresh_sessions WHERE revoked_at IS NULL",
                Integer.class);
    }

    private static void await(CountDownLatch latch) throws InterruptedException {
        if (!latch.await(WAIT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
            throw new AssertionError("Timed out waiting for concurrent operation");
        }
    }

    private static <T> T get(CompletableFuture<T> future) throws Exception {
        return future.get(WAIT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
    }

    private static <T> CompletableFuture<T> runAsync(
            String threadName,
            Callable<T> operation) {
        CompletableFuture<T> future = new CompletableFuture<>();
        Thread.ofPlatform().name(threadName).start(() -> {
            try {
                future.complete(operation.call());
            } catch (Throwable failure) {
                future.completeExceptionally(failure);
            }
        });
        return future;
    }
}
