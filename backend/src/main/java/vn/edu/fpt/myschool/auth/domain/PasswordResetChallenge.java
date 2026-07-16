package vn.edu.fpt.myschool.auth.domain;

import java.time.Instant;
import java.util.UUID;

public record PasswordResetChallenge(
        UUID id,
        UUID userId,
        String otpHash,
        int attempts,
        Instant expiresAt,
        Instant verifiedAt,
        Instant resetTokenExpiresAt,
        Instant usedAt) {

    public boolean isExpiredAt(Instant instant) {
        return !expiresAt.isAfter(instant);
    }

    public boolean resetTokenIsExpiredAt(Instant instant) {
        return resetTokenExpiresAt == null || !resetTokenExpiresAt.isAfter(instant);
    }

    public boolean wasVerified() {
        return verifiedAt != null;
    }

    public boolean wasUsed() {
        return usedAt != null;
    }
}
