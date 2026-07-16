package vn.edu.fpt.myschool.auth.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.fpt.myschool.auth.application.port.AdminLoginAttemptStore;
import vn.edu.fpt.myschool.auth.application.port.SecretTokenService;
import vn.edu.fpt.myschool.auth.application.port.SecurityAuditStore;
import vn.edu.fpt.myschool.auth.application.port.UserAccountStore;
import vn.edu.fpt.myschool.auth.domain.UserAccount;
import vn.edu.fpt.myschool.auth.domain.UserRole;
import vn.edu.fpt.myschool.auth.domain.VietnamesePhoneNumber;

@Service
public class AdminAuthService {

    private static final String LOGIN_SUCCEEDED = "ADMIN_LOGIN_SUCCEEDED";
    private static final String LOGIN_FAILED = "ADMIN_LOGIN_FAILED";
    private static final String LOGIN_RATE_LIMITED = "ADMIN_LOGIN_RATE_LIMITED";
    private static final String REFRESH_SUCCEEDED = "ADMIN_REFRESH_SUCCEEDED";
    private static final String REFRESH_FAILED = "ADMIN_REFRESH_FAILED";
    private static final String LOGOUT = "ADMIN_LOGOUT";

    private final AuthService authService;
    private final UserAccountStore userAccountStore;
    private final AdminLoginAttemptStore loginAttemptStore;
    private final SecurityAuditStore securityAuditStore;
    private final SecretTokenService secretTokenService;
    private final AdminSessionProperties properties;
    private final Clock clock;

    public AdminAuthService(
            AuthService authService,
            UserAccountStore userAccountStore,
            AdminLoginAttemptStore loginAttemptStore,
            SecurityAuditStore securityAuditStore,
            SecretTokenService secretTokenService,
            AdminSessionProperties properties,
            Clock clock) {
        this.authService = authService;
        this.userAccountStore = userAccountStore;
        this.loginAttemptStore = loginAttemptStore;
        this.securityAuditStore = securityAuditStore;
        this.secretTokenService = secretTokenService;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional(noRollbackFor = AuthException.class)
    public AuthenticationResult login(String phoneNumber, String password) {
        Instant now = clock.instant();
        String identifierHash = identifierHash(phoneNumber);
        if (loginAttemptStore.isBlocked(identifierHash, now)) {
            securityAuditStore.record(LOGIN_RATE_LIMITED, null, identifierHash, now);
            throw AuthException.adminLoginRateLimited();
        }

        try {
            AuthenticationResult result =
                    authService.loginForRole(phoneNumber, password, UserRole.ADMIN);
            loginAttemptStore.clear(identifierHash);
            securityAuditStore.record(
                    LOGIN_SUCCEEDED, result.account().id(), identifierHash, now);
            return result;
        } catch (AuthException exception) {
            loginAttemptStore.recordFailure(
                    identifierHash,
                    now,
                    properties.loginAttemptWindow(),
                    properties.maxLoginAttempts(),
                    properties.loginBlockDuration());
            securityAuditStore.record(LOGIN_FAILED, null, identifierHash, now);
            throw exception;
        }
    }

    @Transactional(noRollbackFor = AuthException.class)
    public AuthenticationResult refresh(String refreshToken) {
        Instant now = clock.instant();
        try {
            AuthenticationResult result =
                    authService.refreshForRole(refreshToken, UserRole.ADMIN);
            securityAuditStore.record(REFRESH_SUCCEEDED, result.account().id(), null, now);
            return result;
        } catch (AuthException exception) {
            securityAuditStore.record(REFRESH_FAILED, null, null, now);
            throw exception;
        }
    }

    @Transactional
    public void logout(String refreshToken) {
        authService.logout(refreshToken);
        securityAuditStore.record(LOGOUT, null, null, clock.instant());
    }

    @Transactional(readOnly = true)
    public UserAccount getAccount(UUID userId) {
        return userAccountStore.findById(userId)
                .filter(UserAccount::enabled)
                .filter(account -> account.role() == UserRole.ADMIN)
                .orElseThrow(AdminSessionException::missingRefreshToken);
    }

    private String identifierHash(String phoneNumber) {
        String normalized = VietnamesePhoneNumber.tryNormalize(phoneNumber)
                .map(VietnamesePhoneNumber::value)
                .orElseGet(() -> "invalid:" + phoneNumber.strip().toLowerCase(Locale.ROOT));
        return secretTokenService.hash(normalized);
    }
}
