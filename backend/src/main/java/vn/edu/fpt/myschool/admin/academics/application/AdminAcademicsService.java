package vn.edu.fpt.myschool.admin.academics.application;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.fpt.myschool.admin.academics.application.port.AdminAcademicsStore;
import vn.edu.fpt.myschool.admin.academics.domain.AdminAcademics;
import vn.edu.fpt.myschool.auth.domain.PasswordPolicy;
import vn.edu.fpt.myschool.auth.domain.VietnamesePhoneNumber;
import vn.edu.fpt.myschool.shared.error.ApiErrorCode;
import vn.edu.fpt.myschool.shared.error.ApiException;

@Service
@PreAuthorize("hasRole('ADMIN')")
public class AdminAcademicsService {

    private static final int MAX_PAGE_SIZE = 100;

    private final AdminAcademicsStore store;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public AdminAcademicsService(
            AdminAcademicsStore store,
            PasswordEncoder passwordEncoder,
            Clock clock) {
        this.store = store;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AdminAcademics.Catalog getCatalog() {
        return store.loadCatalog();
    }

    @Transactional(readOnly = true)
    public AdminAcademics.StudentPage getStudents(
            String query,
            Integer gradeLevel,
            UUID classId,
            Boolean enabled,
            int page,
            int size,
            String sort) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw badRequest("Page must be non-negative and size must be between 1 and 100");
        }
        if (gradeLevel != null && (gradeLevel < 6 || gradeLevel > 12)) {
            throw badRequest("Grade level must be between 6 and 12");
        }
        return store.findStudents(
                normalizeNullable(query),
                gradeLevel,
                classId,
                enabled,
                page,
                size,
                parseSort(sort));
    }

    @Transactional
    public UUID createAcademicYear(
            UUID actorUserId,
            String code,
            LocalDate startsOn,
            LocalDate endsOn) {
        requireDateRange(startsOn, endsOn, "Academic year");
        return executeMutation(() -> {
            UUID id = store.createAcademicYear(normalizeCode(code), startsOn, endsOn, clock.instant());
            audit(actorUserId, "CREATE", "ACADEMIC_YEAR", id, "{\"fields\":[\"code\",\"startsOn\",\"endsOn\"]}");
            return id;
        });
    }

    @Transactional
    public void updateAcademicYear(
            UUID actorUserId,
            UUID id,
            String code,
            LocalDate startsOn,
            LocalDate endsOn,
            long version) {
        requireDateRange(startsOn, endsOn, "Academic year");
        executeMutation(() -> {
            if (!store.updateAcademicYear(
                    id, normalizeCode(code), startsOn, endsOn, version, clock.instant())) {
                throw conflict("Academic year was changed by another administrator");
            }
            audit(actorUserId, "UPDATE", "ACADEMIC_YEAR", id, "{\"fields\":[\"code\",\"startsOn\",\"endsOn\"]}");
            return id;
        });
    }

    @Transactional
    public UUID createAcademicTerm(
            UUID actorUserId,
            UUID academicYearId,
            String code,
            String name,
            LocalDate startsOn,
            LocalDate endsOn) {
        requireDateRange(startsOn, endsOn, "Academic term");
        return executeMutation(() -> {
            UUID id = store.createAcademicTerm(
                    academicYearId,
                    normalizeCode(code),
                    requireText(name, "name"),
                    startsOn,
                    endsOn,
                    clock.instant());
            audit(actorUserId, "CREATE", "ACADEMIC_TERM", id, "{\"fields\":[\"academicYearId\",\"code\",\"name\",\"startsOn\",\"endsOn\"]}");
            return id;
        });
    }

    @Transactional
    public void updateAcademicTerm(
            UUID actorUserId,
            UUID id,
            UUID academicYearId,
            String code,
            String name,
            LocalDate startsOn,
            LocalDate endsOn,
            long version) {
        requireDateRange(startsOn, endsOn, "Academic term");
        executeMutation(() -> {
            if (!store.updateAcademicTerm(
                    id, academicYearId, normalizeCode(code), requireText(name, "name"),
                    startsOn, endsOn, version, clock.instant())) {
                throw conflict("Academic term was changed by another administrator");
            }
            audit(actorUserId, "UPDATE", "ACADEMIC_TERM", id, "{\"fields\":[\"academicYearId\",\"code\",\"name\",\"startsOn\",\"endsOn\"]}");
            return id;
        });
    }

    @Transactional
    public UUID createSubject(UUID actorUserId, String code, String name) {
        return executeMutation(() -> {
            UUID id = store.createSubject(normalizeCode(code), requireText(name, "name"), clock.instant());
            audit(actorUserId, "CREATE", "SUBJECT", id, "{\"fields\":[\"code\",\"name\"]}");
            return id;
        });
    }

    @Transactional
    public void updateSubject(
            UUID actorUserId,
            UUID id,
            String code,
            String name,
            boolean enabled,
            long version) {
        executeMutation(() -> {
            if (!store.updateSubject(
                    id, normalizeCode(code), requireText(name, "name"), enabled,
                    version, clock.instant())) {
                throw conflict("Subject was changed by another administrator");
            }
            audit(actorUserId, "UPDATE", "SUBJECT", id, "{\"fields\":[\"code\",\"name\",\"enabled\"]}");
            return id;
        });
    }

    @Transactional
    public UUID createClass(
            UUID actorUserId,
            UUID academicYearId,
            String code,
            String name,
            int gradeLevel) {
        validateGradeLevel(gradeLevel);
        return executeMutation(() -> {
            UUID id = store.createClass(
                    academicYearId,
                    normalizeCode(code),
                    requireText(name, "name"),
                    gradeLevel,
                    clock.instant());
            audit(actorUserId, "CREATE", "SCHOOL_CLASS", id, "{\"fields\":[\"academicYearId\",\"code\",\"name\",\"gradeLevel\"]}");
            return id;
        });
    }

    @Transactional
    public void updateClass(
            UUID actorUserId,
            UUID classId,
            UUID academicYearId,
            String code,
            String name,
            int gradeLevel,
            boolean enabled,
            long version) {
        validateGradeLevel(gradeLevel);
        executeMutation(() -> {
            boolean updated = store.updateClass(
                    classId,
                    academicYearId,
                    normalizeCode(code),
                    requireText(name, "name"),
                    gradeLevel,
                    enabled,
                    version,
                    clock.instant());
            if (!updated) {
                throw conflict("Class was changed by another administrator");
            }
            audit(actorUserId, "UPDATE", "SCHOOL_CLASS", classId, "{\"fields\":[\"code\",\"name\",\"gradeLevel\",\"enabled\"]}");
            return classId;
        });
    }

    @Transactional
    public void deleteAcademicYear(UUID actorUserId, UUID id, long version) {
        delete(actorUserId, id, version, "ACADEMIC_YEAR", store::deleteAcademicYear);
    }

    @Transactional
    public void deleteAcademicTerm(UUID actorUserId, UUID id, long version) {
        delete(actorUserId, id, version, "ACADEMIC_TERM", store::deleteAcademicTerm);
    }

    @Transactional
    public void deleteSubject(UUID actorUserId, UUID id, long version) {
        delete(actorUserId, id, version, "SUBJECT", store::deleteSubject);
    }

    @Transactional
    public void deleteClass(UUID actorUserId, UUID id, long version) {
        delete(actorUserId, id, version, "SCHOOL_CLASS", store::deleteClass);
    }

    @Transactional
    public UUID createStudent(
            UUID actorUserId,
            String phoneNumber,
            String password,
            String studentCode,
            String fullName,
            UUID classId) {
        String normalizedPhone = normalizePhone(phoneNumber);
        if (!PasswordPolicy.isStrong(password)) {
            throw badRequest("Initial password does not meet the password policy");
        }
        AdminAcademics.SchoolClass schoolClass = requireEnabledClass(classId);
        return executeMutation(() -> {
            UUID id = store.createStudent(
                    normalizedPhone,
                    passwordEncoder.encode(password),
                    normalizeCode(studentCode),
                    requireText(fullName, "fullName"),
                    schoolClass,
                    clock.instant());
            audit(actorUserId, "CREATE", "STUDENT", id, "{\"fields\":[\"phoneNumber\",\"studentCode\",\"fullName\",\"classId\"]}");
            return id;
        });
    }

    @Transactional
    public void updateStudent(
            UUID actorUserId,
            UUID studentId,
            String phoneNumber,
            String studentCode,
            String fullName,
            UUID classId,
            boolean enabled,
            long version) {
        String normalizedPhone = normalizePhone(phoneNumber);
        AdminAcademics.SchoolClass schoolClass = requireEnabledClass(classId);
        executeMutation(() -> {
            boolean updated = store.updateStudent(
                    studentId,
                    normalizedPhone,
                    normalizeCode(studentCode),
                    requireText(fullName, "fullName"),
                    schoolClass,
                    enabled,
                    version,
                    clock.instant());
            if (!updated) {
                throw conflict("Student was changed by another administrator");
            }
            audit(actorUserId, "UPDATE", "STUDENT", studentId, "{\"fields\":[\"phoneNumber\",\"studentCode\",\"fullName\",\"classId\",\"enabled\"]}");
            return studentId;
        });
    }

    private AdminAcademics.SchoolClass requireEnabledClass(UUID classId) {
        return store.findClass(classId)
                .filter(AdminAcademics.SchoolClass::enabled)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        ApiErrorCode.RESOURCE_NOT_FOUND.name(),
                        "School class was not found or is disabled"));
    }

    private void audit(UUID actorUserId, String action, String entityType, UUID entityId, String changedFields) {
        store.recordAudit(actorUserId, action, entityType, entityId, changedFields, clock.instant());
    }

    private void delete(
            UUID actorUserId,
            UUID id,
            long version,
            String entityType,
            DeleteOperation operation) {
        executeMutation(() -> {
            if (!operation.delete(id, version)) {
                throw conflict("Record was changed or no longer exists");
            }
            audit(actorUserId, "DELETE", entityType, id, "{\"fields\":[]}");
            return id;
        });
    }

    private static AdminAcademicsStore.StudentSort parseSort(String value) {
        if (value == null || value.isBlank() || value.equals("fullName,asc")) {
            return AdminAcademicsStore.StudentSort.FULL_NAME_ASC;
        }
        return switch (value) {
            case "fullName,desc" -> AdminAcademicsStore.StudentSort.FULL_NAME_DESC;
            case "studentCode,asc" -> AdminAcademicsStore.StudentSort.STUDENT_CODE_ASC;
            case "updatedAt,desc" -> AdminAcademicsStore.StudentSort.UPDATED_AT_DESC;
            default -> throw badRequest("Unsupported student sort");
        };
    }

    private static String normalizePhone(String phoneNumber) {
        try {
            return VietnamesePhoneNumber.normalize(phoneNumber).value();
        } catch (IllegalArgumentException exception) {
            throw badRequest("Invalid Vietnamese mobile phone number");
        }
    }

    private static String normalizeCode(String value) {
        return requireText(value, "code").toUpperCase(Locale.ROOT);
    }

    private static String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw badRequest(name + " must not be blank");
        }
        return value.strip();
    }

    private static void validateGradeLevel(int gradeLevel) {
        if (gradeLevel < 6 || gradeLevel > 12) {
            throw badRequest("Grade level must be between 6 and 12");
        }
    }

    private static void requireDateRange(LocalDate startsOn, LocalDate endsOn, String label) {
        if (startsOn == null || endsOn == null || !endsOn.isAfter(startsOn)) {
            throw badRequest(label + " end date must be after its start date");
        }
    }

    private static <T> T executeMutation(Mutation<T> mutation) {
        try {
            return mutation.execute();
        } catch (DataIntegrityViolationException exception) {
            throw conflict("The value already exists or conflicts with referenced academic data");
        }
    }

    private static ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_FAILED.name(), message);
    }

    private static ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, ApiErrorCode.CONFLICT.name(), message);
    }

    @FunctionalInterface
    private interface Mutation<T> {
        T execute();
    }

    @FunctionalInterface
    private interface DeleteOperation {
        boolean delete(UUID id, long version);
    }
}
