package vn.edu.fpt.myschool.auth.application.port;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import vn.edu.fpt.myschool.auth.domain.RefreshSession;

public interface RefreshSessionStore {

    void create(RefreshSession session);

    Optional<UUID> findUserIdByTokenHash(String tokenHash);

    Optional<RefreshSession> findByTokenHashForUpdate(String tokenHash);

    void markUsed(UUID sessionId, Instant usedAt);

    void revokeFamily(UUID familyId, Instant revokedAt, String reason);

    void revokeAllForUser(UUID userId, Instant revokedAt, String reason);
}
