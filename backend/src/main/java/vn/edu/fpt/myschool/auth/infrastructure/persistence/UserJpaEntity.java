package vn.edu.fpt.myschool.auth.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
class UserJpaEntity {

    @Id
    private UUID id;

    @Column(name = "phone_number", nullable = false, length = 10, unique = true)
    private String phoneNumber;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "credentials_updated_at", nullable = false)
    private Instant credentialsUpdatedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserJpaEntity() {
    }

    UserJpaEntity(
            UUID id,
            String phoneNumber,
            String passwordHash,
            boolean enabled,
            Instant now) {
        this.id = id;
        this.phoneNumber = phoneNumber;
        this.passwordHash = passwordHash;
        this.enabled = enabled;
        this.credentialsUpdatedAt = now;
        this.createdAt = now;
        this.updatedAt = now;
    }

    UUID getId() {
        return id;
    }

    String getPhoneNumber() {
        return phoneNumber;
    }

    String getPasswordHash() {
        return passwordHash;
    }

    boolean isEnabled() {
        return enabled;
    }

    void updatePassword(String passwordHash, Instant updatedAt) {
        this.passwordHash = passwordHash;
        this.credentialsUpdatedAt = updatedAt;
        this.updatedAt = updatedAt;
    }
}
