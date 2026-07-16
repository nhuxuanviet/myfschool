package vn.edu.fpt.myschool.auth.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "password_reset_challenges")
class PasswordResetChallengeJpaEntity {

    @Id
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "phone_hash", nullable = false, length = 64)
    private String phoneHash;

    @Column(name = "otp_hash", nullable = false, length = 64)
    private String otpHash;

    @Column(name = "attempts", nullable = false)
    private short attempts;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "reset_token_hash", length = 64, unique = true)
    private String resetTokenHash;

    @Column(name = "reset_token_expires_at")
    private Instant resetTokenExpiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PasswordResetChallengeJpaEntity() {
    }

    PasswordResetChallengeJpaEntity(
            UUID id,
            UUID userId,
            String phoneHash,
            String otpHash,
            Instant expiresAt,
            Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.phoneHash = phoneHash;
        this.otpHash = otpHash;
        this.attempts = 0;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    UUID getId() {
        return id;
    }

    UUID getUserId() {
        return userId;
    }

    String getOtpHash() {
        return otpHash;
    }

    int getAttempts() {
        return attempts;
    }

    Instant getExpiresAt() {
        return expiresAt;
    }

    Instant getVerifiedAt() {
        return verifiedAt;
    }

    Instant getResetTokenExpiresAt() {
        return resetTokenExpiresAt;
    }

    Instant getUsedAt() {
        return usedAt;
    }
}
