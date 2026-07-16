package vn.edu.fpt.myschool.auth.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.fpt.myschool.auth.application.port.OtpDeliveryPort;
import vn.edu.fpt.myschool.auth.application.port.OtpDeliveryException;
import vn.edu.fpt.myschool.auth.application.port.OtpGenerator;
import vn.edu.fpt.myschool.auth.application.port.PasswordResetChallengeStore;
import vn.edu.fpt.myschool.auth.application.port.PasswordResetRateLimitStore;
import vn.edu.fpt.myschool.auth.application.port.RefreshSessionStore;
import vn.edu.fpt.myschool.auth.application.port.SecretTokenService;
import vn.edu.fpt.myschool.auth.application.port.UserAccountStore;
import vn.edu.fpt.myschool.auth.domain.PasswordPolicy;
import vn.edu.fpt.myschool.auth.domain.PasswordResetChallenge;
import vn.edu.fpt.myschool.auth.domain.UserAccount;
import vn.edu.fpt.myschool.auth.domain.VietnamesePhoneNumber;

@Service
public class PasswordResetService {

    private static final String PASSWORD_RESET_REASON = "PASSWORD_RESET";

    private final UserAccountStore userAccountStore;
    private final RefreshSessionStore refreshSessionStore;
    private final PasswordResetChallengeStore challengeStore;
    private final PasswordResetRateLimitStore rateLimitStore;
    private final SecretTokenService secretTokenService;
    private final OtpGenerator otpGenerator;
    private final OtpDeliveryPort otpDeliveryPort;
    private final PasswordEncoder passwordEncoder;
    private final AuthProperties properties;
    private final Clock clock;

    public PasswordResetService(
            UserAccountStore userAccountStore,
            RefreshSessionStore refreshSessionStore,
            PasswordResetChallengeStore challengeStore,
            PasswordResetRateLimitStore rateLimitStore,
            SecretTokenService secretTokenService,
            OtpGenerator otpGenerator,
            OtpDeliveryPort otpDeliveryPort,
            PasswordEncoder passwordEncoder,
            AuthProperties properties,
            Clock clock) {
        this.userAccountStore = userAccountStore;
        this.refreshSessionStore = refreshSessionStore;
        this.challengeStore = challengeStore;
        this.rateLimitStore = rateLimitStore;
        this.secretTokenService = secretTokenService;
        this.otpGenerator = otpGenerator;
        this.otpDeliveryPort = otpDeliveryPort;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public ResetChallengeResult request(String phoneNumber) {
        Instant now = clock.instant();
        UUID challengeId = UUID.randomUUID();
        Optional<VietnamesePhoneNumber> normalizedPhone =
                VietnamesePhoneNumber.tryNormalize(phoneNumber);
        String phoneMaterial = normalizedPhone
                .map(VietnamesePhoneNumber::value)
                .orElse("invalid-phone");
        String phoneHash = secretTokenService.hash("phone:" + phoneMaterial);
        if (!rateLimitStore.tryAcquire(
                phoneHash,
                now,
                properties.resetRequestRateLimitWindow(),
                properties.maxResetRequestsPerWindow())) {
            throw AuthException.resetRequestRateLimited();
        }
        Optional<UserAccount> account = normalizedPhone
                .flatMap(phone -> userAccountStore.findByPhoneNumber(phone.value()));
        String otp = otpGenerator.generate();

        challengeStore.create(
                challengeId,
                account.map(UserAccount::id).orElse(null),
                phoneHash,
                secretTokenService.hashOtp(challengeId, otp),
                now.plus(properties.resetChallengeTtl()),
                now);
        ResetChallengeResult result = new ResetChallengeResult(
                challengeId, properties.resetChallengeTtl().toSeconds());
        try {
            account.ifPresent(user ->
                    otpDeliveryPort.sendPasswordResetOtp(user.phoneNumber(), otp));
        } catch (OtpDeliveryException exception) {
            throw new PasswordResetDeliveryUnavailableException(result, exception);
        }
        return result;
    }

    @Transactional(noRollbackFor = AuthException.class)
    public ResetVerificationResult verify(UUID challengeId, String otp) {
        Instant now = clock.instant();
        PasswordResetChallenge challenge = challengeStore.findByIdForUpdate(challengeId)
                .orElseThrow(AuthException::invalidChallenge);
        if (challenge.wasUsed()
                || challenge.wasVerified()
                || challenge.isExpiredAt(now)
                || challenge.attempts() >= properties.maxOtpAttempts()) {
            throw AuthException.invalidChallenge();
        }
        if (!secretTokenService.matchesOtp(challenge.id(), otp, challenge.otpHash())) {
            challengeStore.incrementAttempts(challenge.id());
            throw AuthException.invalidOtp();
        }

        String resetToken = secretTokenService.generateToken();
        challengeStore.markVerified(
                challenge.id(),
                now,
                secretTokenService.hash(resetToken),
                now.plus(properties.resetTokenTtl()));
        return new ResetVerificationResult(resetToken);
    }

    @Transactional
    public void complete(String resetToken, String newPassword) {
        if (!PasswordPolicy.isStrong(newPassword)) {
            throw AuthException.weakPassword();
        }

        Instant now = clock.instant();
        String passwordHash = passwordEncoder.encode(newPassword);
        String resetTokenHash = secretTokenService.hash(resetToken);
        // The projection identifies the owner without attaching or locking the challenge first.
        PasswordResetChallengeStore.ResetTokenReference reference = challengeStore
                .findResetTokenReference(resetTokenHash)
                .orElseThrow(AuthException::invalidResetToken);
        if (reference.userId() != null
                && userAccountStore.findByIdForUpdate(reference.userId()).isEmpty()) {
            throw AuthException.invalidResetToken();
        }
        PasswordResetChallenge challenge = challengeStore
                .findByResetTokenHashForUpdate(resetTokenHash)
                .filter(locked -> locked.id().equals(reference.challengeId()))
                .filter(locked -> Objects.equals(locked.userId(), reference.userId()))
                .orElseThrow(AuthException::invalidResetToken);
        if (challenge.wasUsed()
                || !challenge.wasVerified()
                || challenge.resetTokenIsExpiredAt(now)) {
            throw AuthException.invalidResetToken();
        }

        challengeStore.markUsed(challenge.id(), now);
        if (challenge.userId() == null) {
            return;
        }

        userAccountStore.updatePassword(challenge.userId(), passwordHash, now);
        refreshSessionStore.revokeAllForUser(
                challenge.userId(), now, PASSWORD_RESET_REASON);
        challengeStore.markAllUsedForUser(challenge.userId(), now);
    }
}
