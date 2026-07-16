package vn.edu.fpt.myschool.auth.application.port;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import vn.edu.fpt.myschool.auth.domain.PasswordResetChallenge;

public interface PasswordResetChallengeStore {

    record ResetTokenReference(UUID challengeId, UUID userId) {
    }

    void create(
            UUID challengeId,
            UUID userId,
            String phoneHash,
            String otpHash,
            Instant expiresAt,
            Instant createdAt);

    Optional<PasswordResetChallenge> findByIdForUpdate(UUID challengeId);

    Optional<ResetTokenReference> findResetTokenReference(String resetTokenHash);

    Optional<PasswordResetChallenge> findByResetTokenHashForUpdate(String resetTokenHash);

    void incrementAttempts(UUID challengeId);

    void markVerified(
            UUID challengeId,
            Instant verifiedAt,
            String resetTokenHash,
            Instant resetTokenExpiresAt);

    void markUsed(UUID challengeId, Instant usedAt);

    void markAllUsedForUser(UUID userId, Instant usedAt);
}
