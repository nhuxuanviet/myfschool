package vn.edu.fpt.myschool.auth.domain;

import java.util.Objects;
import java.util.UUID;

public record TeacherProfile(UUID id, String teacherCode, String fullName)
        implements UserProfile {

    public TeacherProfile {
        Objects.requireNonNull(id, "id must not be null");
        if (teacherCode == null || teacherCode.isBlank()) {
            throw new IllegalArgumentException("teacherCode must not be blank");
        }
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("fullName must not be blank");
        }
    }
}
