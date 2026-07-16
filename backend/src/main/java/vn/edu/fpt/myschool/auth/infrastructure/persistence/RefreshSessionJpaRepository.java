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

interface RefreshSessionJpaRepository extends JpaRepository<RefreshSessionJpaEntity, UUID> {

    @Query("select session.userId from RefreshSessionJpaEntity session where session.tokenHash = :tokenHash")
    Optional<UUID> findUserIdByTokenHash(@Param("tokenHash") String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select session from RefreshSessionJpaEntity session where session.tokenHash = :tokenHash")
    Optional<RefreshSessionJpaEntity> findByTokenHashForUpdate(
            @Param("tokenHash") String tokenHash);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update RefreshSessionJpaEntity session
            set session.usedAt = :usedAt
            where session.id = :sessionId and session.usedAt is null
            """)
    int markUsed(@Param("sessionId") UUID sessionId, @Param("usedAt") Instant usedAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update RefreshSessionJpaEntity session
            set session.revokedAt = :revokedAt,
                session.revocationReason = :reason
            where session.familyId = :familyId and session.revokedAt is null
            """)
    int revokeFamily(
            @Param("familyId") UUID familyId,
            @Param("revokedAt") Instant revokedAt,
            @Param("reason") String reason);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update RefreshSessionJpaEntity session
            set session.revokedAt = :revokedAt,
                session.revocationReason = :reason
            where session.userId = :userId and session.revokedAt is null
            """)
    int revokeAllForUser(
            @Param("userId") UUID userId,
            @Param("revokedAt") Instant revokedAt,
            @Param("reason") String reason);
}
