package vn.edu.fpt.myschool.admin.identity.application.port;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import vn.edu.fpt.myschool.admin.identity.domain.AdminIdentity;

public interface AdminIdentityStore {

    AdminIdentity.TeacherPage findTeachers(
            String query, Boolean enabled, Boolean hasAccount, int page, int size, String sort);

    Optional<AdminIdentity.Teacher> findTeacher(UUID teacherId);

    boolean teacherCodeExists(String teacherCode, UUID excludingTeacherId);

    UUID createTeacher(
            String teacherCode, String fullName, String email, Instant now);

    /**
     * @param expectedVersion the version the caller last read; the update is refused when the row
     *     has moved on, so two administrators editing the same teacher cannot overwrite each other.
     */
    boolean updateTeacher(
            UUID teacherId,
            String teacherCode,
            String fullName,
            String email,
            boolean enabled,
            long expectedVersion,
            Instant now);

    void recordAudit(
            UUID actorUserId,
            String action,
            String entityType,
            UUID entityId,
            String changedFieldsJson,
            Instant occurredAt);
}
