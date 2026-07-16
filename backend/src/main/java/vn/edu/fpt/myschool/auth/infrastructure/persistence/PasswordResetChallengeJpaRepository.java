package vn.edu.fpt.myschool.auth.infrastructure.persistence;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface PasswordResetChallengeJpaRepository
        extends JpaRepository<PasswordResetChallengeJpaEntity, UUID> {

    interface ResetTokenReferenceProjection {

        UUID getChallengeId();

        UUID getUserId();
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select challenge from PasswordResetChallengeJpaEntity challenge
            where challenge.id = :challengeId
            """)
    Optional<PasswordResetChallengeJpaEntity> findByIdForUpdate(
            @Param("challengeId") UUID challengeId);

    @Query("""
            select challenge.id as challengeId, challenge.userId as userId
            from PasswordResetChallengeJpaEntity challenge
            where challenge.resetTokenHash = :resetTokenHash
            """)
    Optional<ResetTokenReferenceProjection> findResetTokenReference(
            @Param("resetTokenHash") String resetTokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select challenge from PasswordResetChallengeJpaEntity challenge
            where challenge.resetTokenHash = :resetTokenHash
            """)
    Optional<PasswordResetChallengeJpaEntity> findByResetTokenHashForUpdate(
            @Param("resetTokenHash") String resetTokenHash);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update PasswordResetChallengeJpaEntity challenge
            set challenge.attempts = challenge.attempts + 1
            where challenge.id = :challengeId
            """)
    int incrementAttempts(@Param("challengeId") UUID challengeId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update PasswordResetChallengeJpaEntity challenge
            set challenge.verifiedAt = :verifiedAt,
                challenge.resetTokenHash = :resetTokenHash,
                challenge.resetTokenExpiresAt = :resetTokenExpiresAt
            where challenge.id = :challengeId and challenge.verifiedAt is null
            """)
    int markVerified(
            @Param("challengeId") UUID challengeId,
            @Param("verifiedAt") Instant verifiedAt,
            @Param("resetTokenHash") String resetTokenHash,
            @Param("resetTokenExpiresAt") Instant resetTokenExpiresAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update PasswordResetChallengeJpaEntity challenge
            set challenge.usedAt = :usedAt
            where challenge.id = :challengeId and challenge.usedAt is null
            """)
    int markUsed(@Param("challengeId") UUID challengeId, @Param("usedAt") Instant usedAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update PasswordResetChallengeJpaEntity challenge
            set challenge.usedAt = :usedAt
            where challenge.userId = :userId and challenge.usedAt is null
            """)
    int markAllUsedForUser(@Param("userId") UUID userId, @Param("usedAt") Instant usedAt);
}
