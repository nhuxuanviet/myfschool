package vn.edu.fpt.myschool.auth.infrastructure.persistence;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "parent_profiles")
class ParentProfileJpaEntity {

    @Id
    private UUID id;

    /** Null until the school issues an account for this guardian. */
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    protected ParentProfileJpaEntity() {
    }

    UUID getId() {
        return id;
    }

    String getFullName() {
        return fullName;
    }

    boolean isEnabled() {
        return enabled;
    }
}
