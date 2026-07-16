package vn.edu.fpt.myschool.admin.identity.application;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.fpt.myschool.admin.identity.application.port.AdminIdentityStore;
import vn.edu.fpt.myschool.admin.identity.domain.AdminIdentity;

@Service
public class AdminIdentityService {

    private static final String TEACHER_ENTITY = "TEACHER";

    private final AdminIdentityStore store;
    private final Clock clock;

    public AdminIdentityService(AdminIdentityStore store, Clock clock) {
        this.store = store;
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

    private void audit(UUID actorUserId, String action, UUID entityId, String changedFieldsJson) {
        Instant now = clock.instant();
        store.recordAudit(actorUserId, action, TEACHER_ENTITY, entityId, changedFieldsJson, now);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String stripped = value.strip();
        return stripped.isEmpty() ? null : stripped;
    }
}
