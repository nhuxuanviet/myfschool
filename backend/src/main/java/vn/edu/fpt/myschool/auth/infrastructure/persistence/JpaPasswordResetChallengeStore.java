package vn.edu.fpt.myschool.auth.infrastructure.persistence;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import vn.edu.fpt.myschool.auth.application.port.PasswordResetChallengeStore;
import vn.edu.fpt.myschool.auth.domain.PasswordResetChallenge;

@Repository
class JpaPasswordResetChallengeStore implements PasswordResetChallengeStore {

    private final PasswordResetChallengeJpaRepository repository;

    JpaPasswordResetChallengeStore(PasswordResetChallengeJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void create(
            UUID challengeId,
            UUID userId,
            String phoneHash,
            String otpHash,
            Instant expiresAt,
            Instant createdAt) {
        repository.save(new PasswordResetChallengeJpaEntity(
                challengeId, userId, phoneHash, otpHash, expiresAt, createdAt));
    }

    @Override
    public Optional<PasswordResetChallenge> findByIdForUpdate(UUID challengeId) {
        return repository.findByIdForUpdate(challengeId).map(this::toDomain);
    }

    @Override
    public Optional<ResetTokenReference> findResetTokenReference(String resetTokenHash) {
        return repository.findResetTokenReference(resetTokenHash)
                .map(reference -> new ResetTokenReference(
                        reference.getChallengeId(), reference.getUserId()));
    }

    @Override
    public Optional<PasswordResetChallenge> findByResetTokenHashForUpdate(
            String resetTokenHash) {
        return repository.findByResetTokenHashForUpdate(resetTokenHash).map(this::toDomain);
    }

    @Override
    public void incrementAttempts(UUID challengeId) {
        repository.incrementAttempts(challengeId);
    }

    @Override
    public void markVerified(
            UUID challengeId,
            Instant verifiedAt,
            String resetTokenHash,
            Instant resetTokenExpiresAt) {
        if (repository.markVerified(
                challengeId, verifiedAt, resetTokenHash, resetTokenExpiresAt) != 1) {
            throw new IllegalStateException("Password reset challenge was concurrently verified");
        }
    }

    @Override
    public void markUsed(UUID challengeId, Instant usedAt) {
        if (repository.markUsed(challengeId, usedAt) != 1) {
            throw new IllegalStateException("Password reset token was concurrently consumed");
        }
    }

    @Override
    public void markAllUsedForUser(UUID userId, Instant usedAt) {
        repository.markAllUsedForUser(userId, usedAt);
    }

    private PasswordResetChallenge toDomain(PasswordResetChallengeJpaEntity challenge) {
        return new PasswordResetChallenge(
                challenge.getId(),
                challenge.getUserId(),
                challenge.getOtpHash(),
                challenge.getAttempts(),
                challenge.getExpiresAt(),
                challenge.getVerifiedAt(),
                challenge.getResetTokenExpiresAt(),
                challenge.getUsedAt());
    }
}
