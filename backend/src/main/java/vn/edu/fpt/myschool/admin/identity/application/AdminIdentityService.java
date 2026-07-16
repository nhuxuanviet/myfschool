package vn.edu.fpt.myschool.admin.identity.application;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.fpt.myschool.admin.identity.application.port.AdminIdentityStore;
import vn.edu.fpt.myschool.admin.identity.domain.AdminIdentity;
import vn.edu.fpt.myschool.auth.domain.PasswordPolicy;
import vn.edu.fpt.myschool.auth.domain.VietnamesePhoneNumber;
import vn.edu.fpt.myschool.shared.time.SchoolTimeZone;

@Service
public class AdminIdentityService {

    private static final String TEACHER_ENTITY = "TEACHER";
    private static final String PARENT_ENTITY = "PARENT";
    private static final String LINK_ENTITY = "GUARDIAN_LINK";

    private final AdminIdentityStore store;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public AdminIdentityService(
            AdminIdentityStore store, PasswordEncoder passwordEncoder, Clock clock) {
        this.store = store;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AdminIdentity.TeacherPage getTeachers(
            String query, Boolean enabled, Boolean hasAccount, int page, int size, String sort) {
        return store.findTeachers(normalize(query), enabled, hasAccount, page, Math.min(size, 100), sort);
    }

    @Transactional
    public UUID createTeacher(UUID actorUserId, String teacherCode, String fullName, String email) {
        String code = teacherCode.strip();
        if (store.teacherCodeExists(code, null)) {
            throw AdminIdentityException.teacherCodeTaken();
        }
        UUID id = store.createTeacher(code, fullName.strip(), normalize(email), clock.instant());
        audit(actorUserId, "CREATE", id, "{\"fields\":[\"teacherCode\",\"fullName\",\"email\"]}");
        return id;
    }

    @Transactional
    public void updateTeacher(
            UUID actorUserId,
            UUID teacherId,
            String teacherCode,
            String fullName,
            String email,
            boolean enabled,
            long expectedVersion) {
        AdminIdentity.Teacher existing = store.findTeacher(teacherId)
                .orElseThrow(AdminIdentityException::teacherNotFound);
        String code = teacherCode.strip();
        if (store.teacherCodeExists(code, teacherId)) {
            throw AdminIdentityException.teacherCodeTaken();
        }
        boolean updated = store.updateTeacher(
                existing.id(), code, fullName.strip(), normalize(email), enabled,
                expectedVersion, clock.instant());
        if (!updated) {
            throw AdminIdentityException.staleTeacher();
        }
        audit(actorUserId, "UPDATE", teacherId,
                "{\"fields\":[\"teacherCode\",\"fullName\",\"email\",\"enabled\"]}");
    }

    @Transactional(readOnly = true)
    public AdminIdentity.ParentPage getParents(
            String query, Boolean enabled, int page, int size, String sort) {
        return store.findParents(normalize(query), enabled, page, Math.min(size, 100), sort);
    }

    /**
     * Creates a guardian, with an account when a phone number is given.
     *
     * <p>A guardian without an account is legitimate: the school records who a child's guardians
     * are before deciding which of them get to sign in.
     */
    @Transactional
    public UUID createParent(
            UUID actorUserId,
            String fullName,
            String email,
            String phoneNumber,
            String initialPassword) {
        String normalizedPhone = null;
        String passwordHash = null;
        if (normalize(phoneNumber) != null) {
            normalizedPhone = VietnamesePhoneNumber.tryNormalize(phoneNumber)
                    .map(VietnamesePhoneNumber::value)
                    .orElseThrow(AdminIdentityException::invalidPhoneNumber);
            if (!PasswordPolicy.isStrong(initialPassword)) {
                throw AdminIdentityException.weakPassword();
            }
            if (store.phoneNumberExists(normalizedPhone)) {
                throw AdminIdentityException.phoneNumberTaken();
            }
            passwordHash = passwordEncoder.encode(initialPassword);
        }
        UUID id = store.createParent(
                fullName.strip(), normalize(email), normalizedPhone, passwordHash, clock.instant());
        audit(actorUserId, "CREATE", PARENT_ENTITY, id,
                "{\"fields\":[\"fullName\",\"email\",\"phoneNumber\"]}");
        return id;
    }

    @Transactional
    public void updateParent(
            UUID actorUserId,
            UUID parentId,
            String fullName,
            String email,
            boolean enabled,
            long expectedVersion) {
        store.findParent(parentId).orElseThrow(AdminIdentityException::parentNotFound);
        if (!store.updateParent(parentId, fullName.strip(), normalize(email), enabled,
                expectedVersion, clock.instant())) {
            throw AdminIdentityException.staleParent();
        }
        audit(actorUserId, "UPDATE", PARENT_ENTITY, parentId,
                "{\"fields\":[\"fullName\",\"email\",\"enabled\"]}");
    }

    @Transactional(readOnly = true)
    public List<AdminIdentity.GuardianLink> getLinks(UUID parentId, UUID studentId, boolean inForceOnly) {
        return store.findLinks(parentId, studentId, inForceOnly);
    }

    @Transactional
    public UUID linkGuardian(
            UUID actorUserId,
            UUID parentId,
            UUID studentId,
            AdminIdentity.Relationship relationship,
            int contactOrder) {
        store.findParent(parentId).orElseThrow(AdminIdentityException::parentNotFound);
        if (!store.studentExists(studentId)) {
            throw AdminIdentityException.studentNotFound();
        }
        if (store.linkInForceExists(parentId, studentId)) {
            throw AdminIdentityException.linkAlreadyInForce();
        }
        LocalDate today = LocalDate.ofInstant(clock.instant(), SchoolTimeZone.ZONE);
        UUID id = store.createLink(parentId, studentId, relationship, contactOrder, today, clock.instant());
        audit(actorUserId, "CREATE", LINK_ENTITY, id,
                "{\"fields\":[\"parentId\",\"studentId\",\"relationship\",\"contactOrder\"]}");
        return id;
    }

    /**
     * Ends a link from tomorrow.
     *
     * <p>The schema requires effective_to to be after effective_from, and a link created today
     * cannot be ended today without violating that. Ending tomorrow also matches what the date
     * means: the guardian did hold the relationship for the whole of today.
     */
    @Transactional
    public void unlinkGuardian(UUID actorUserId, UUID linkId) {
        LocalDate endsOn = LocalDate.ofInstant(clock.instant(), SchoolTimeZone.ZONE).plusDays(1);
        if (!store.endLink(linkId, endsOn, clock.instant())) {
            throw AdminIdentityException.linkNotInForce();
        }
        audit(actorUserId, "UPDATE", LINK_ENTITY, linkId, "{\"fields\":[\"effectiveTo\"]}");
    }

    private void audit(
            UUID actorUserId, String action, String entityType, UUID entityId, String changedFieldsJson) {
        store.recordAudit(actorUserId, action, entityType, entityId, changedFieldsJson, clock.instant());
    }

    private void audit(UUID actorUserId, String action, UUID entityId, String changedFieldsJson) {
        audit(actorUserId, action, TEACHER_ENTITY, entityId, changedFieldsJson);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String stripped = value.strip();
        return stripped.isEmpty() ? null : stripped;
    }
}
