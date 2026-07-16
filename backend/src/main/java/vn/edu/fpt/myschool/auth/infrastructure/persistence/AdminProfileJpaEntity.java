package vn.edu.fpt.myschool.auth.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "admin_profiles")
class AdminProfileJpaEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AdminProfileJpaEntity() {
    }

    AdminProfileJpaEntity(UUID id, UUID userId, String fullName, Instant now) {
        this.id = id;
        this.userId = userId;
        this.fullName = fullName;
        this.createdAt = now;
        this.updatedAt = now;
    }

    UUID getId() {
        return id;
    }

    UUID getUserId() {
        return userId;
    }

    String getFullName() {
        return fullName;
    }

    void updateFullName(String fullName, Instant updatedAt) {
        this.fullName = fullName;
        this.updatedAt = updatedAt;
    }
}
