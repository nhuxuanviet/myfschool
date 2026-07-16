package vn.edu.fpt.myschool.auth.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.fpt.myschool.auth.application.port.AccessTokenIssuer;
import vn.edu.fpt.myschool.auth.application.port.RefreshSessionStore;
import vn.edu.fpt.myschool.auth.application.port.SecretTokenService;
import vn.edu.fpt.myschool.auth.application.port.UserAccountStore;
import vn.edu.fpt.myschool.auth.application.port.UserProfileStore;
import vn.edu.fpt.myschool.auth.domain.PasswordPolicy;
import vn.edu.fpt.myschool.auth.domain.RefreshSession;
import vn.edu.fpt.myschool.auth.domain.UserAccount;
import vn.edu.fpt.myschool.auth.domain.UserProfile;
import vn.edu.fpt.myschool.auth.domain.UserRole;
import vn.edu.fpt.myschool.auth.domain.VietnamesePhoneNumber;

@Service
public class AuthService {

    private static final String REFRESH_REUSE_REASON = "REUSE_DETECTED";
    private static final String REFRESH_EXPIRED_REASON = "EXPIRED";
    private static final String LOGOUT_REASON = "LOGOUT";
    private static final String ROLE_SWITCH_REASON = "ROLE_SWITCHED";

    private final UserAccountStore userAccountStore;
    private final UserProfileStore userProfileStore;
    private final RefreshSessionStore refreshSessionStore;
    private final AccessTokenIssuer accessTokenIssuer;
    private final SecretTokenService secretTokenService;
    private final PasswordEncoder passwordEncoder;
    private final AuthProperties properties;
    private final Clock clock;
    private final String dummyPasswordHash;

    public AuthService(
            UserAccountStore userAccountStore,
            UserProfileStore userProfileStore,
            RefreshSessionStore refreshSessionStore,
            AccessTokenIssuer accessTokenIssuer,
            SecretTokenService secretTokenService,
            PasswordEncoder passwordEncoder,
            AuthProperties properties,
            Clock clock) {
        this.userAccountStore = userAccountStore;
        this.userProfileStore = userProfileStore;
        this.refreshSessionStore = refreshSessionStore;
        this.accessTokenIssuer = accessTokenIssuer;
        this.secretTokenService = secretTokenService;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
        this.clock = clock;
        this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    /**
     * Roles that may open a session in the mobile app.
     *
     * <p>Admin is absent on purpose: administration is web-only and holds a separate, stricter
     * session mechanism. Without this set, an admin account with a single role would be let into
     * the mobile app by the "one role, go straight in" rule below.
     */
    private static final Set<UserRole> MOBILE_ROLES = Collections.unmodifiableSet(
            EnumSet.of(UserRole.STUDENT, UserRole.TEACHER, UserRole.PARENT));

    @Transactional(noRollbackFor = AuthException.class)
    public AuthenticationResult login(String phoneNumber, String password, UserRole requestedRole) {
        UserAccount account = authenticate(phoneNumber, password);
        Set<UserRole> mobileRoles = mobileRolesOf(account);
        if (mobileRoles.isEmpty()) {
            throw AuthException.invalidCredentials();
        }
        UserRole activeRole = resolveActiveRole(mobileRoles, requestedRole);
        return issueSession(account, activeRole, UUID.randomUUID(), null);
    }

    /**
     * Opens a session for another role the same account already holds.
     *
     * <p>The previous session family is revoked rather than left running, so that one sign-in
     * yields exactly one live session and a token always answers for the role on screen.
     */
    @Transactional(noRollbackFor = AuthException.class)
    public AuthenticationResult switchRole(String refreshToken, UserRole targetRole) {
        Instant now = clock.instant();
        String tokenHash = secretTokenService.hash(refreshToken);
        UUID userId = refreshSessionStore.findUserIdByTokenHash(tokenHash)
                .orElseThrow(AuthException::invalidRefreshToken);
        UserAccount account = userAccountStore.findByIdForUpdate(userId)
                .filter(UserAccount::enabled)
                .orElseThrow(AuthException::invalidRefreshToken);
        RefreshSession currentSession = refreshSessionStore.findByTokenHashForUpdate(tokenHash)
                .filter(session -> session.userId().equals(userId))
                .orElseThrow(AuthException::invalidRefreshToken);
        if (currentSession.wasRevoked() || currentSession.isExpiredAt(now)) {
            throw AuthException.invalidRefreshToken();
        }
        if (!mobileRolesOf(account).contains(targetRole)) {
            throw AuthException.invalidCredentials();
        }
        refreshSessionStore.revokeFamily(currentSession.familyId(), now, ROLE_SWITCH_REASON);
        return issueSession(account, targetRole, UUID.randomUUID(), null);
    }

    public Set<UserRole> mobileRolesOf(UserAccount account) {
        return account.roles().stream()
                .filter(MOBILE_ROLES::contains)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(UserRole.class)));
    }

    private UserRole resolveActiveRole(Set<UserRole> mobileRoles, UserRole requestedRole) {
        if (requestedRole != null) {
            if (!mobileRoles.contains(requestedRole)) {
                throw AuthException.invalidCredentials();
            }
            return requestedRole;
        }
        if (mobileRoles.size() == 1) {
            return mobileRoles.iterator().next();
        }
        throw AuthException.roleSelectionRequired();
    }

    private UserAccount authenticate(String phoneNumber, String password) {
        Optional<VietnamesePhoneNumber> normalizedPhone =
                VietnamesePhoneNumber.tryNormalize(phoneNumber);
        Optional<UserAccount> account = normalizedPhone
                .flatMap(phone -> userAccountStore.findByPhoneNumberForUpdate(phone.value()));
        String passwordHash = account.map(UserAccount::passwordHash).orElse(dummyPasswordHash);
        boolean passwordLengthValid = PasswordPolicy.isBcryptCompatible(password);
        String passwordForCheck = passwordLengthValid ? password : "Invalid@Password1";
        boolean passwordMatches = passwordEncoder.matches(passwordForCheck, passwordHash);

        if (account.isEmpty()
                || !passwordLengthValid
                || !passwordMatches
                || !account.orElseThrow().enabled()) {
            throw AuthException.invalidCredentials();
        }
        return account.orElseThrow();
    }

    @Transactional(noRollbackFor = AuthException.class)
    public AuthenticationResult loginForRole(
            String phoneNumber,
            String password,
            UserRole expectedRole) {
        UserAccount account = authenticate(phoneNumber, password);
        if (!account.hasRole(expectedRole)) {
            throw AuthException.invalidCredentials();
        }
        return issueSession(account, expectedRole, UUID.randomUUID(), null);
    }

    /**
     * Rotates a mobile session, keeping the role it was opened for.
     *
     * <p>The role comes from the session itself rather than a default, so a teacher or parent
     * session refreshes into its own role instead of being told it is not a student.
     */
    @Transactional(noRollbackFor = AuthException.class)
    public AuthenticationResult refresh(String refreshToken) {
        UserRole sessionRole = refreshSessionStore
                .findByTokenHashForUpdate(secretTokenService.hash(refreshToken))
                .map(RefreshSession::activeRole)
                .orElseThrow(AuthException::invalidRefreshToken);
        if (!MOBILE_ROLES.contains(sessionRole)) {
            throw AuthException.invalidRefreshToken();
        }
        return refreshForRole(refreshToken, sessionRole);
    }

    @Transactional(noRollbackFor = AuthException.class)
    public AuthenticationResult refreshForRole(String refreshToken, UserRole expectedRole) {
        Instant now = clock.instant();
        String tokenHash = secretTokenService.hash(refreshToken);
        // Credential/session mutations always lock the user row before a session row.
        UUID userId = refreshSessionStore.findUserIdByTokenHash(tokenHash)
                .orElseThrow(AuthException::invalidRefreshToken);
        UserAccount account = userAccountStore.findByIdForUpdate(userId)
                .filter(UserAccount::enabled)
                .filter(candidate -> candidate.hasRole(expectedRole))
                .orElseThrow(AuthException::invalidRefreshToken);
        RefreshSession currentSession = refreshSessionStore
                .findByTokenHashForUpdate(tokenHash)
                .filter(session -> session.userId().equals(userId))
                .orElseThrow(AuthException::invalidRefreshToken);

        if (currentSession.wasConsumed()) {
            refreshSessionStore.revokeFamily(
                    currentSession.familyId(), now, REFRESH_REUSE_REASON);
            throw AuthException.invalidRefreshToken();
        }
        if (currentSession.wasRevoked()) {
            throw AuthException.invalidRefreshToken();
        }
        if (currentSession.isExpiredAt(now)) {
            refreshSessionStore.revokeFamily(
                    currentSession.familyId(), now, REFRESH_EXPIRED_REASON);
            throw AuthException.invalidRefreshToken();
        }

        refreshSessionStore.markUsed(currentSession.id(), now);
        return issueSession(account, expectedRole, currentSession.familyId(), currentSession.id());
    }

    @Transactional
    public void logout(String refreshToken) {
        String tokenHash = secretTokenService.hash(refreshToken);
        refreshSessionStore.findByTokenHashForUpdate(tokenHash)
                .ifPresent(session -> refreshSessionStore.revokeFamily(
                        session.familyId(), clock.instant(), LOGOUT_REASON));
    }

    private AuthenticationResult issueSession(
            UserAccount account,
            UserRole activeRole,
            UUID familyId,
            UUID parentSessionId) {
        UserProfile profile = userProfileStore.findProfile(account.id(), activeRole)
                .orElseThrow(() -> new IllegalStateException(
                        "Profile is missing for role " + activeRole));
        Instant now = clock.instant();
        String refreshToken = secretTokenService.generateToken();
        RefreshSession refreshSession = new RefreshSession(
                UUID.randomUUID(),
                account.id(),
                familyId,
                parentSessionId,
                activeRole,
                secretTokenService.hash(refreshToken),
                now.plus(properties.refreshTokenTtl()),
                null,
                null,
                now);
        refreshSessionStore.create(refreshSession);

        AccessTokenIssuer.IssuedAccessToken accessToken =
                accessTokenIssuer.issue(account, activeRole);
        return new AuthenticationResult(
                accessToken.value(),
                refreshToken,
                accessToken.expiresIn(),
                account,
                activeRole,
                profile);
    }
}
