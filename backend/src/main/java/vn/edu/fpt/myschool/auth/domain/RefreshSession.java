package vn.edu.fpt.myschool.auth.domain;

import java.time.Instant;
import java.util.UUID;

public record RefreshSession(
        UUID id,
        UUID userId,
        UUID familyId,
        UUID parentSessionId,
        String tokenHash,
        Instant expiresAt,
        Instant usedAt,
        Instant revokedAt,
        Instant createdAt) {

    public boolean isExpiredAt(Instant instant) {
        return !expiresAt.isAfter(instant);
    }

    public boolean wasConsumed() {
        return usedAt != null;
    }

    public boolean wasRevoked() {
        return revokedAt != null;
    }
}
