package vn.edu.fpt.myschool.auth.infrastructure.persistence;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "teacher_profiles")
class TeacherProfileJpaEntity {

    @Id
    private UUID id;

    /** Null until the school issues an account for this teacher. */
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "teacher_code", nullable = false, length = 32)
    private String teacherCode;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    protected TeacherProfileJpaEntity() {
    }

    UUID getId() {
        return id;
    }

    String getTeacherCode() {
        return teacherCode;
    }

    String getFullName() {
        return fullName;
    }

    boolean isEnabled() {
        return enabled;
    }
}
