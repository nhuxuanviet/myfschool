package vn.edu.fpt.myschool.auth.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "students")
class StudentJpaEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "student_code", nullable = false, length = 32, unique = true)
    private String studentCode;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(name = "grade_level", nullable = false)
    private short gradeLevel;

    @Column(name = "class_name", nullable = false, length = 32)
    private String className;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected StudentJpaEntity() {
    }

    StudentJpaEntity(
            UUID id,
            UUID userId,
            String studentCode,
            String fullName,
            int gradeLevel,
            String className,
            Instant now) {
        this.id = id;
        this.userId = userId;
        this.studentCode = studentCode;
        this.fullName = fullName;
        this.gradeLevel = (short) gradeLevel;
        this.className = className;
        this.createdAt = now;
        this.updatedAt = now;
    }

    UUID getId() {
        return id;
    }

    String getStudentCode() {
        return studentCode;
    }

    String getFullName() {
        return fullName;
    }

    int getGradeLevel() {
        return gradeLevel;
    }

    String getClassName() {
        return className;
    }

    void updateSeedProfile(
            String studentCode,
            String fullName,
            int gradeLevel,
            String className,
            Instant now) {
        this.studentCode = studentCode;
        this.fullName = fullName;
        this.gradeLevel = (short) gradeLevel;
        this.className = className;
        this.updatedAt = now;
    }
}
