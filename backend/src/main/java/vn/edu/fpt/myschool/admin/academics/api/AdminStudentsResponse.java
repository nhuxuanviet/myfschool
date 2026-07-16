package vn.edu.fpt.myschool.admin.academics.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import vn.edu.fpt.myschool.admin.academics.domain.AdminAcademics;

public record AdminStudentsResponse(
        List<StudentResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    static AdminStudentsResponse from(AdminAcademics.StudentPage value) {
        return new AdminStudentsResponse(
                value.items().stream().map(StudentResponse::from).toList(),
                value.page(),
                value.size(),
                value.totalElements(),
                value.totalPages());
    }

    public record StudentResponse(
            UUID id,
            String studentCode,
            String fullName,
            String phoneNumber,
            int gradeLevel,
            UUID classId,
            String classCode,
            boolean enabled,
            long version,
            Instant updatedAt) {
        static StudentResponse from(AdminAcademics.Student value) {
            return new StudentResponse(
                    value.id(), value.studentCode(), value.fullName(), value.phoneNumber(),
                    value.gradeLevel(), value.classId(), value.classCode(), value.enabled(),
                    value.version(), value.updatedAt());
        }
    }
}
