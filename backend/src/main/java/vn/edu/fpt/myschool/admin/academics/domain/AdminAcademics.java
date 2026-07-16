package vn.edu.fpt.myschool.admin.academics.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class AdminAcademics {

    private AdminAcademics() {
    }

    public record Catalog(
            List<AcademicYear> academicYears,
            List<AcademicTerm> terms,
            List<Subject> subjects,
            List<SchoolClass> classes) {
        public Catalog {
            academicYears = List.copyOf(academicYears);
            terms = List.copyOf(terms);
            subjects = List.copyOf(subjects);
            classes = List.copyOf(classes);
        }
    }

    public record AcademicYear(
            UUID id, String code, LocalDate startsOn, LocalDate endsOn, long version) {
    }

    public record AcademicTerm(
            UUID id,
            UUID academicYearId,
            String code,
            String name,
            LocalDate startsOn,
            LocalDate endsOn,
            long version) {
    }

    public record Subject(UUID id, String code, String name, boolean enabled, long version) {
    }

    public record SchoolClass(
            UUID id,
            UUID academicYearId,
            String code,
            String name,
            int gradeLevel,
            boolean enabled,
            long version,
            long studentCount) {
    }

    public record Student(
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
    }

    public record StudentPage(
            List<Student> items,
            int page,
            int size,
            long totalElements,
            int totalPages) {
        public StudentPage {
            items = List.copyOf(Objects.requireNonNull(items, "items must not be null"));
        }
    }
}
