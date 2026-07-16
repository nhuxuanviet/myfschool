package vn.edu.fpt.myschool.admin.identity.application.port;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
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

    AdminIdentity.ParentPage findParents(String query, Boolean enabled, int page, int size, String sort);

    Optional<AdminIdentity.Parent> findParent(UUID parentId);

    boolean phoneNumberExists(String phoneNumber);

    /**
     * Creates a guardian profile, and an account for them when a phone number is supplied.
     *
     * <p>Both rows are written together with the PARENT role: an account without its role row
     * cannot be loaded at all.
     */
    UUID createParent(
            String fullName,
            String email,
            String phoneNumber,
            String passwordHash,
            Instant now);

    boolean updateParent(
            UUID parentId,
            String fullName,
            String email,
            boolean enabled,
            long expectedVersion,
            Instant now);

    List<AdminIdentity.GuardianLink> findLinks(UUID parentId, UUID studentId, boolean inForceOnly);

    boolean studentExists(UUID studentId);

    boolean linkInForceExists(UUID parentId, UUID studentId);

    UUID createLink(
            UUID parentId,
            UUID studentId,
            AdminIdentity.Relationship relationship,
            int contactOrder,
            LocalDate effectiveFrom,
            Instant now);

    /** Ends a link by dating it, never by deleting the row. */
    boolean endLink(UUID linkId, LocalDate effectiveTo, Instant now);

    void recordAudit(
            UUID actorUserId,
            String action,
            String entityType,
            UUID entityId,
            String changedFieldsJson,
            Instant occurredAt);
}
