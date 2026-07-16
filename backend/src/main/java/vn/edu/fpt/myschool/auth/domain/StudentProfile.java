package vn.edu.fpt.myschool.auth.domain;

import java.util.Objects;
import java.util.UUID;

public record StudentProfile(
        UUID id,
        String studentCode,
        String fullName,
        int gradeLevel,
        String className) implements UserProfile {

    public StudentProfile {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(studentCode, "studentCode must not be null");
        Objects.requireNonNull(fullName, "fullName must not be null");
        Objects.requireNonNull(className, "className must not be null");
    }
}
