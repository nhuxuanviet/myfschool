package vn.edu.fpt.myschool.admin.academics.application.port;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import vn.edu.fpt.myschool.admin.academics.domain.AdminAcademics;

public interface AdminAcademicsStore {

    enum StudentSort {
        FULL_NAME_ASC,
        FULL_NAME_DESC,
        STUDENT_CODE_ASC,
        UPDATED_AT_DESC
    }

    AdminAcademics.Catalog loadCatalog();

    AdminAcademics.StudentPage findStudents(
            String query,
            Integer gradeLevel,
            UUID classId,
            Boolean enabled,
            int page,
            int size,
            StudentSort sort);

    Optional<AdminAcademics.SchoolClass> findClass(UUID classId);

    Optional<AdminAcademics.Student> findStudent(UUID studentId);

    UUID createAcademicYear(String code, LocalDate startsOn, LocalDate endsOn, Instant now);

    boolean updateAcademicYear(
            UUID id, String code, LocalDate startsOn, LocalDate endsOn, long version, Instant now);

    UUID createAcademicTerm(
            UUID academicYearId,
            String code,
            String name,
            LocalDate startsOn,
            LocalDate endsOn,
            Instant now);

    boolean updateAcademicTerm(
            UUID id,
            UUID academicYearId,
            String code,
            String name,
            LocalDate startsOn,
            LocalDate endsOn,
            long version,
            Instant now);

    UUID createSubject(String code, String name, Instant now);

    boolean updateSubject(
            UUID id, String code, String name, boolean enabled, long version, Instant now);

    UUID createClass(
            UUID academicYearId,
            String code,
            String name,
            int gradeLevel,
            Instant now);

    boolean updateClass(
            UUID id,
            UUID academicYearId,
            String code,
            String name,
            int gradeLevel,
            boolean enabled,
            long version,
            Instant now);

    boolean deleteAcademicYear(UUID id, long version);

    boolean deleteAcademicTerm(UUID id, long version);

    boolean deleteSubject(UUID id, long version);

    boolean deleteClass(UUID id, long version);

    UUID createStudent(
            String phoneNumber,
            String passwordHash,
            String studentCode,
            String fullName,
            AdminAcademics.SchoolClass schoolClass,
            Instant now);

    boolean updateStudent(
            UUID studentId,
            String phoneNumber,
            String studentCode,
            String fullName,
            AdminAcademics.SchoolClass schoolClass,
            boolean enabled,
            long version,
            Instant now);

    void recordAudit(
            UUID actorUserId,
            String action,
            String entityType,
            UUID entityId,
            String changedFieldsJson,
            Instant occurredAt);
}
