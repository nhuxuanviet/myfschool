package vn.edu.fpt.myschool.auth.infrastructure.persistence;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import vn.edu.fpt.myschool.auth.application.port.RefreshSessionStore;
import vn.edu.fpt.myschool.auth.domain.RefreshSession;

@Repository
class JpaRefreshSessionStore implements RefreshSessionStore {

    private final RefreshSessionJpaRepository repository;

    JpaRefreshSessionStore(RefreshSessionJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void create(RefreshSession session) {
        repository.save(new RefreshSessionJpaEntity(
                session.id(),
                session.userId(),
                session.familyId(),
                session.parentSessionId(),
                session.activeRole(),
                session.tokenHash(),
                session.expiresAt(),
                session.usedAt(),
                session.revokedAt(),
                session.createdAt()));
    }

    @Override
    public Optional<UUID> findUserIdByTokenHash(String tokenHash) {
        return repository.findUserIdByTokenHash(tokenHash);
    }

    @Override
    public Optional<RefreshSession> findByTokenHashForUpdate(String tokenHash) {
        return repository.findByTokenHashForUpdate(tokenHash).map(this::toDomain);
    }

    @Override
    public void markUsed(UUID sessionId, Instant usedAt) {
        if (repository.markUsed(sessionId, usedAt) != 1) {
            throw new IllegalStateException("Refresh session was concurrently consumed");
        }
    }

    @Override
    public void revokeFamily(UUID familyId, Instant revokedAt, String reason) {
        repository.revokeFamily(familyId, revokedAt, reason);
    }

    @Override
    public void revokeAllForUser(UUID userId, Instant revokedAt, String reason) {
        repository.revokeAllForUser(userId, revokedAt, reason);
    }

    private RefreshSession toDomain(RefreshSessionJpaEntity session) {
        return new RefreshSession(
                session.getId(),
                session.getUserId(),
                session.getFamilyId(),
                session.getParentSessionId(),
                session.getActiveRole(),
                session.getTokenHash(),
                session.getExpiresAt(),
                session.getUsedAt(),
                session.getRevokedAt(),
                session.getCreatedAt());
    }
}
