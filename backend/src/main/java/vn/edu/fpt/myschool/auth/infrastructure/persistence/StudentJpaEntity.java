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

    @Column(name = "class_id")
    private UUID classId;

    /**
     * The class code, read through the class relationship.
     *
     * <p>Derived rather than stored: students.class_name was dropped in V25 because a class had
     * two representations that could disagree. This keeps the existing read contract intact while
     * class_id remains the only thing written.
     */
    @org.hibernate.annotations.Formula("(SELECT school_class.code FROM school_classes school_class WHERE school_class.id = class_id)")
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
            UUID classId,
            Instant now) {
        this.id = id;
        this.userId = userId;
        this.studentCode = studentCode;
        this.fullName = fullName;
        this.gradeLevel = (short) gradeLevel;
        this.classId = classId;
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
            UUID classId,
            Instant now) {
        this.studentCode = studentCode;
        this.fullName = fullName;
        this.gradeLevel = (short) gradeLevel;
        this.classId = classId;
        this.updatedAt = now;
    }
}
