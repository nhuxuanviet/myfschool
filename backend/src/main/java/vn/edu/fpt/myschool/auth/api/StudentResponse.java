package vn.edu.fpt.myschool.auth.api;

import java.util.UUID;

import vn.edu.fpt.myschool.auth.domain.StudentProfile;

public record StudentResponse(
        UUID id,
        String studentCode,
        String fullName,
        int gradeLevel,
        String className) {

    static StudentResponse from(StudentProfile profile) {
        return new StudentResponse(
                profile.id(),
                profile.studentCode(),
                profile.fullName(),
                profile.gradeLevel(),
                profile.className());
    }
}
