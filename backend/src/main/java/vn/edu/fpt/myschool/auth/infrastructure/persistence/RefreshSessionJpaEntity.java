package vn.edu.fpt.myschool.auth.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import vn.edu.fpt.myschool.auth.domain.UserRole;

@Entity
@Table(name = "refresh_sessions")
class RefreshSessionJpaEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    @Column(name = "parent_session_id")
    private UUID parentSessionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "active_role", nullable = false, length = 32)
    private UserRole activeRole;

    @Column(name = "token_hash", nullable = false, length = 64, unique = true)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revocation_reason", length = 64)
    private String revocationReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected RefreshSessionJpaEntity() {
    }

    RefreshSessionJpaEntity(
            UUID id,
            UUID userId,
            UUID familyId,
            UUID parentSessionId,
            UserRole activeRole,
            String tokenHash,
            Instant expiresAt,
            Instant usedAt,
            Instant revokedAt,
            Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.familyId = familyId;
        this.parentSessionId = parentSessionId;
        this.activeRole = activeRole;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.usedAt = usedAt;
        this.revokedAt = revokedAt;
        this.createdAt = createdAt;
    }

    UUID getId() {
        return id;
    }

    UUID getUserId() {
        return userId;
    }

    UUID getFamilyId() {
        return familyId;
    }

    UUID getParentSessionId() {
        return parentSessionId;
    }

    UserRole getActiveRole() {
        return activeRole;
    }

    String getTokenHash() {
        return tokenHash;
    }

    Instant getExpiresAt() {
        return expiresAt;
    }

    Instant getUsedAt() {
        return usedAt;
    }

    Instant getRevokedAt() {
        return revokedAt;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}
