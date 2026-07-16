package vn.edu.fpt.myschool.auth.infrastructure.persistence;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import vn.edu.fpt.myschool.auth.domain.UserRole;

@Entity
@Table(name = "user_roles")
@IdClass(UserRoleJpaEntity.UserRoleId.class)
class UserRoleJpaEntity {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 32)
    private UserRole role;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected UserRoleJpaEntity() {
    }

    UserRoleJpaEntity(UUID userId, UserRole role, Instant createdAt) {
        this.userId = userId;
        this.role = role;
        this.createdAt = createdAt;
    }

    UUID getUserId() {
        return userId;
    }

    UserRole getRole() {
        return role;
    }

    /** Composite identifier for (user_id, role). Public per the JPA id-class contract. */
    public static final class UserRoleId implements Serializable {

        private UUID userId;
        private UserRole role;

        public UserRoleId() {
        }

        public UserRoleId(UUID userId, UserRole role) {
            this.userId = userId;
            this.role = role;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            return other instanceof UserRoleId candidate
                    && Objects.equals(userId, candidate.userId)
                    && role == candidate.role;
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, role);
        }
    }
}
