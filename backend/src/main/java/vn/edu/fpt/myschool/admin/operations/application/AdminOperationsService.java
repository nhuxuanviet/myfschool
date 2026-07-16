package vn.edu.fpt.myschool.admin.operations.application;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.fpt.myschool.admin.operations.application.port.AdminOperationsStore;
import vn.edu.fpt.myschool.admin.operations.domain.AdminOperations;
import vn.edu.fpt.myschool.shared.error.ApiErrorCode;
import vn.edu.fpt.myschool.shared.error.ApiException;

@Service
@PreAuthorize("hasRole('ADMIN')")
public class AdminOperationsService {

    private static final Set<String> SESSIONS = Set.of("MORNING", "AFTERNOON");
    private static final Set<String> OVERRIDE_TYPES = Set.of("CANCELLED", "REPLACED", "ADDED");
    private static final Set<String> ASSESSMENT_MODES = Set.of("NUMERIC", "REMARK");
    private static final Set<String> ASSESSMENT_KINDS = Set.of("REGULAR", "MIDTERM", "FINAL");
    private static final Set<String> ASSESSMENT_FORMS = Set.of(
            "ORAL", "WRITTEN", "PRESENTATION", "PRACTICAL", "EXPERIMENT", "PRODUCT", "PROJECT");
    private static final Set<String> ASSESSMENT_STATUSES = Set.of(
            "RECORDED", "MAKE_UP_REQUIRED", "EXCUSED", "ABSENT_FINALIZED");
    private static final Set<String> FORM_STATUSES = Set.of(
            "SUBMITTED", "IN_REVIEW", "APPROVED", "REJECTED", "CANCELLED");
    private static final Set<String> AUDIENCES = Set.of("ALL", "GRADE");
    private static final Set<String> EVENT_CATEGORIES = Set.of(
            "ACADEMIC", "CULTURAL", "SPORTS", "CLUB", "CAREER");
    private static final Set<String> CLUB_CATEGORIES = Set.of(
            "ACADEMIC", "SPORTS", "ARTS", "SKILLS", "COMMUNITY", "MEDIA");
    private static final Set<String> EVENT_REGISTRATION_STATUSES = Set.of("REGISTERED", "CANCELLED");
    private static final Set<String> CLUB_MEMBERSHIP_STATUSES = Set.of(
            "PENDING", "ACTIVE", "REJECTED", "WITHDRAWN");

    private final AdminOperationsStore store;
    private final Clock clock;

    public AdminOperationsService(AdminOperationsStore store, Clock clock) {
        this.store = store;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AdminOperations.Timetable getTimetable(UUID termId, UUID classId) {
        return store.loadTimetable(termId, classId);
    }

    @Transactional
    public UUID createLesson(
            UUID actorId,
            UUID termId,
            UUID classId,
            int dayOfWeek,
            String session,
            int periodNumber,
            UUID subjectId,
            UUID teacherId,
            String room) {
        validateSlot(dayOfWeek, session, periodNumber);
        return mutate(() -> {
            UUID id = store.createLesson(termId, classId, dayOfWeek, normalize(session),
                    periodNumber, subjectId, teacherId, textOrNull(room), now());
            audit(actorId, "CREATE", "TIMETABLE_LESSON", id);
            return id;
        });
    }

    @Transactional
    public void updateLesson(
            UUID actorId,
            UUID id,
            UUID subjectId,
            UUID teacherId,
            String room,
            long version) {
        mutate(() -> {
            requireUpdated(store.updateLesson(id, subjectId, teacherId,
                    textOrNull(room), version, now()), "Lesson");
            audit(actorId, "UPDATE", "TIMETABLE_LESSON", id);
            return id;
        });
    }

    @Transactional
    public void deleteLesson(UUID actorId, UUID id, long version) {
        requireUpdated(store.deleteLesson(id, version), "Lesson");
        audit(actorId, "DELETE", "TIMETABLE_LESSON", id);
    }

    @Transactional
    public UUID createOverride(
            UUID actorId,
            UUID termId,
            UUID classId,
            LocalDate lessonDate,
            String session,
            int periodNumber,
            String overrideType,
            UUID subjectId,
            UUID teacherId,
            String room,
            String note) {
        validateSlot(lessonDate.getDayOfWeek().getValue(), session, periodNumber);
        String type = allowed(overrideType, OVERRIDE_TYPES, "overrideType");
        if ((type.equals("CANCELLED") && subjectId != null)
                || (!type.equals("CANCELLED") && subjectId == null)) {
            throw badRequest("Cancelled lessons must not have a subject; added or replaced lessons must have one");
        }
        return mutate(() -> {
            UUID id = store.createOverride(termId, classId, lessonDate, normalize(session),
                    periodNumber, type, subjectId, teacherId, textOrNull(room),
                    textOrNull(note), now());
            audit(actorId, "CREATE", "TIMETABLE_OVERRIDE", id);
            return id;
        });
    }

    @Transactional
    public void deleteOverride(UUID actorId, UUID id, long version) {
        requireUpdated(store.deleteOverride(id, version), "Timetable override");
        audit(actorId, "DELETE", "TIMETABLE_OVERRIDE", id);
    }

    @Transactional(readOnly = true)
    public AdminOperations.Grades getGrades(UUID studentId, UUID termId) {
        return store.loadGrades(studentId, termId);
    }

    @Transactional
    public UUID assignSubject(
            UUID actorId,
            UUID studentId,
            UUID termId,
            UUID subjectId,
            String assessmentMode,
            Integer annualLessonCount,
            int displayOrder) {
        String mode = allowed(assessmentMode, ASSESSMENT_MODES, "assessmentMode");
        if (displayOrder < 1 || (mode.equals("NUMERIC")
                && (annualLessonCount == null || annualLessonCount < 35))
                || (mode.equals("REMARK") && annualLessonCount != null)) {
            throw badRequest("Invalid subject assessment configuration");
        }
        return mutate(() -> {
            UUID id = store.assignSubject(studentId, termId, subjectId, mode,
                    annualLessonCount, displayOrder, now());
            audit(actorId, "CREATE", "STUDENT_TERM_SUBJECT", id);
            return id;
        });
    }

    @Transactional
    public UUID createAssessment(
            UUID actorId,
            UUID enrollmentId,
            String kind,
            String form,
            String label,
            Integer duration,
            String status,
            BigDecimal score,
            String outcome,
            LocalDate assessedOn,
            int displayOrder) {
        validateAssessment(form, label, duration, status, score, outcome, displayOrder);
        return mutate(() -> {
            UUID id = store.createAssessment(enrollmentId,
                    allowed(kind, ASSESSMENT_KINDS, "assessmentKind"),
                    normalize(form), requireText(label, "displayLabel"), duration,
                    normalize(status), score, normalizeNullable(outcome), assessedOn,
                    displayOrder, now());
            audit(actorId, "CREATE", "GRADE_ASSESSMENT", id);
            return id;
        });
    }

    @Transactional
    public void updateAssessment(
            UUID actorId,
            UUID id,
            String form,
            String label,
            Integer duration,
            String status,
            BigDecimal score,
            String outcome,
            LocalDate assessedOn,
            long version) {
        validateAssessment(form, label, duration, status, score, outcome, 1);
        mutate(() -> {
            requireUpdated(store.updateAssessment(id, normalize(form),
                    requireText(label, "displayLabel"), duration, normalize(status), score,
                    normalizeNullable(outcome), assessedOn, version, now()), "Assessment");
            audit(actorId, "UPDATE", "GRADE_ASSESSMENT", id);
            return id;
        });
    }

    @Transactional(readOnly = true)
    public java.util.List<AdminOperations.StudentForm> getForms(String status) {
        return store.loadForms(status == null || status.isBlank()
                ? null : allowed(status, FORM_STATUSES, "status"));
    }

    @Transactional
    public void updateFormStatus(UUID actorId, UUID id, String status, String note, long version) {
        String normalized = allowed(status, FORM_STATUSES, "status");
        requireUpdated(store.updateFormStatus(id, normalized, textOrNull(note), version, now()), "Form");
        audit(actorId, "UPDATE_STATUS", "STUDENT_FORM", id);
    }

    @Transactional(readOnly = true)
    public java.util.List<AdminOperations.Announcement> getAnnouncements() {
        return store.loadAnnouncements();
    }

    @Transactional
    public UUID createAnnouncement(
            UUID actorId,
            String title,
            String body,
            String audience,
            Integer gradeLevel,
            Instant publishedAt,
            Instant visibleFrom,
            Instant visibleUntil) {
        validatePublication(audience, gradeLevel, publishedAt, visibleFrom, visibleUntil);
        return mutate(() -> {
            UUID id = store.createAnnouncement(requireText(title, "title"),
                    requireText(body, "body"), normalize(audience), gradeLevel,
                    publishedAt, visibleFrom, visibleUntil, now());
            audit(actorId, "PUBLISH", "ANNOUNCEMENT", id);
            return id;
        });
    }

    @Transactional
    public void updateAnnouncement(
            UUID actorId,
            UUID id,
            String title,
            String body,
            String audience,
            Integer gradeLevel,
            Instant publishedAt,
            Instant visibleFrom,
            Instant visibleUntil,
            long version) {
        validatePublication(audience, gradeLevel, publishedAt, visibleFrom, visibleUntil);
        mutate(() -> {
            requireUpdated(store.updateAnnouncement(id, requireText(title, "title"),
                    requireText(body, "body"), normalize(audience), gradeLevel,
                    publishedAt, visibleFrom, visibleUntil, version, now()), "Announcement");
            audit(actorId, "UPDATE", "ANNOUNCEMENT", id);
            return id;
        });
    }

    @Transactional
    public void deleteAnnouncement(UUID actorId, UUID id, long version) {
        requireUpdated(store.deleteAnnouncement(id, version), "Announcement");
        audit(actorId, "DELETE", "ANNOUNCEMENT", id);
    }

    @Transactional(readOnly = true)
    public AdminOperations.Activities getActivities() {
        return store.loadActivities();
    }

    @Transactional
    public UUID createEvent(
            UUID actorId,
            String category,
            String title,
            String description,
            String location,
            Instant startsAt,
            Instant endsAt,
            String audience,
            Integer gradeLevel,
            Integer capacity,
            Instant registrationDeadline,
            Instant cancellationDeadline,
            boolean registrationEnabled) {
        validateActivityDates(startsAt, endsAt, registrationDeadline, cancellationDeadline);
        validateAudience(audience, gradeLevel);
        return mutate(() -> {
            UUID id = store.createEvent(allowed(category, EVENT_CATEGORIES, "category"),
                    requireText(title, "title"), requireText(description, "description"),
                    requireText(location, "location"), startsAt, endsAt, normalize(audience),
                    gradeLevel, positiveOrNull(capacity, "capacity"), registrationDeadline,
                    cancellationDeadline, registrationEnabled, now());
            audit(actorId, "CREATE", "SCHOOL_EVENT", id);
            return id;
        });
    }

    @Transactional
    public void updateEvent(
            UUID actorId,
            UUID id,
            String category,
            String title,
            String description,
            String location,
            Instant startsAt,
            Instant endsAt,
            String audience,
            Integer gradeLevel,
            Integer capacity,
            Instant registrationDeadline,
            Instant cancellationDeadline,
            boolean registrationEnabled,
            boolean enabled,
            long version) {
        validateActivityDates(startsAt, endsAt, registrationDeadline, cancellationDeadline);
        validateAudience(audience, gradeLevel);
        mutate(() -> {
            requireUpdated(store.updateEvent(id,
                    allowed(category, EVENT_CATEGORIES, "category"),
                    requireText(title, "title"), requireText(description, "description"),
                    requireText(location, "location"), startsAt, endsAt, normalize(audience),
                    gradeLevel, positiveOrNull(capacity, "capacity"), registrationDeadline,
                    cancellationDeadline, registrationEnabled, enabled, version, now()), "Event");
            audit(actorId, "UPDATE", "SCHOOL_EVENT", id);
            return id;
        });
    }

    @Transactional
    public UUID createClub(
            UUID actorId,
            String category,
            String name,
            String description,
            String advisorName,
            String meetingSchedule,
            String location,
            String audience,
            Integer gradeLevel,
            Integer capacity,
            Instant applicationDeadline,
            boolean acceptingApplications) {
        validateAudience(audience, gradeLevel);
        return mutate(() -> {
            UUID id = store.createClub(allowed(category, CLUB_CATEGORIES, "category"),
                    requireText(name, "name"), requireText(description, "description"),
                    requireText(advisorName, "advisorName"),
                    requireText(meetingSchedule, "meetingSchedule"),
                    requireText(location, "location"), normalize(audience), gradeLevel,
                    positiveOrNull(capacity, "capacity"), applicationDeadline,
                    acceptingApplications, now());
            audit(actorId, "CREATE", "SCHOOL_CLUB", id);
            return id;
        });
    }

    @Transactional
    public void updateClub(
            UUID actorId,
            UUID id,
            String category,
            String name,
            String description,
            String advisorName,
            String meetingSchedule,
            String location,
            String audience,
            Integer gradeLevel,
            Integer capacity,
            Instant applicationDeadline,
            boolean acceptingApplications,
            boolean enabled,
            long version) {
        validateAudience(audience, gradeLevel);
        mutate(() -> {
            requireUpdated(store.updateClub(id,
                    allowed(category, CLUB_CATEGORIES, "category"),
                    requireText(name, "name"), requireText(description, "description"),
                    requireText(advisorName, "advisorName"),
                    requireText(meetingSchedule, "meetingSchedule"),
                    requireText(location, "location"), normalize(audience), gradeLevel,
                    positiveOrNull(capacity, "capacity"), applicationDeadline,
                    acceptingApplications, enabled, version, now()), "Club");
            audit(actorId, "UPDATE", "SCHOOL_CLUB", id);
            return id;
        });
    }

    @Transactional
    public void updateEventRegistration(
            UUID actorId, UUID id, String status, long version) {
        requireUpdated(store.updateEventRegistration(id,
                allowed(status, EVENT_REGISTRATION_STATUSES, "status"), version, now()),
                "Event registration");
        audit(actorId, "UPDATE_STATUS", "EVENT_REGISTRATION", id);
    }

    @Transactional
    public void updateClubMembership(UUID actorId, UUID id, String status, long version) {
        requireUpdated(store.updateClubMembership(id,
                allowed(status, CLUB_MEMBERSHIP_STATUSES, "status"), version, now()),
                "Club membership");
        audit(actorId, "UPDATE_STATUS", "CLUB_MEMBERSHIP", id);
    }

    @Transactional(readOnly = true)
    public AdminOperations.AuditPage getAudit(String query, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw badRequest("Page must be non-negative and size must be between 1 and 100");
        }
        return store.loadAudit(textOrNull(query), page, size);
    }

    @Transactional(readOnly = true)
    public byte[] exportAuditCsv(String query) {
        StringBuilder csv = new StringBuilder("Thời gian,Quản trị viên,Hành động,Đối tượng,Mã đối tượng\r\n");
        store.exportAudit(textOrNull(query), 10_000).forEach(event -> csv
                .append(csv(event.occurredAt().toString())).append(',')
                .append(csv(event.actorName())).append(',')
                .append(csv(event.action())).append(',')
                .append(csv(event.entityType())).append(',')
                .append(csv(event.entityId().toString())).append("\r\n"));
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void validateSlot(int dayOfWeek, String session, int periodNumber) {
        if (dayOfWeek < 1 || dayOfWeek > 7 || periodNumber < 1 || periodNumber > 5) {
            throw badRequest("Invalid timetable slot");
        }
        allowed(session, SESSIONS, "session");
    }

    private void validateAssessment(
            String form,
            String label,
            Integer duration,
            String status,
            BigDecimal score,
            String outcome,
            int displayOrder) {
        allowed(form, ASSESSMENT_FORMS, "assessmentForm");
        allowed(status, ASSESSMENT_STATUSES, "status");
        requireText(label, "displayLabel");
        if (displayOrder < 1 || (duration != null && (duration < 1 || duration > 180))
                || (score != null && (score.signum() < 0 || score.compareTo(BigDecimal.TEN) > 0))) {
            throw badRequest("Invalid assessment value");
        }
        if ((score != null || outcome != null) && !normalize(status).equals("RECORDED")
                && !normalize(status).equals("ABSENT_FINALIZED")) {
            throw badRequest("Only recorded assessments may have a result");
        }
    }

    private void validatePublication(
            String audience,
            Integer gradeLevel,
            Instant publishedAt,
            Instant visibleFrom,
            Instant visibleUntil) {
        validateAudience(audience, gradeLevel);
        if (publishedAt == null || visibleFrom == null
                || (visibleUntil != null && !visibleUntil.isAfter(visibleFrom))) {
            throw badRequest("Invalid announcement visibility period");
        }
    }

    private void validateAudience(String audience, Integer gradeLevel) {
        String value = allowed(audience, AUDIENCES, "audience");
        if ((value.equals("ALL") && gradeLevel != null)
                || (value.equals("GRADE") && (gradeLevel == null || gradeLevel < 6 || gradeLevel > 12))) {
            throw badRequest("Invalid audience grade level");
        }
    }

    private void validateActivityDates(
            Instant startsAt,
            Instant endsAt,
            Instant registrationDeadline,
            Instant cancellationDeadline) {
        if (startsAt == null || endsAt == null || !endsAt.isAfter(startsAt)
                || (registrationDeadline != null && registrationDeadline.isAfter(startsAt))
                || (cancellationDeadline != null && cancellationDeadline.isAfter(startsAt))) {
            throw badRequest("Invalid activity dates");
        }
    }

    private void audit(UUID actorId, String action, String entityType, UUID entityId) {
        store.audit(actorId, action, entityType, entityId, "{}", now());
    }

    private Instant now() {
        return clock.instant();
    }

    private static void requireUpdated(boolean updated, String entity) {
        if (!updated) {
            throw new ApiException(HttpStatus.CONFLICT, ApiErrorCode.CONFLICT.name(),
                    entity + " was changed by another administrator");
        }
    }

    private static String allowed(String value, Set<String> allowed, String field) {
        String normalized = normalize(value);
        if (!allowed.contains(normalized)) {
            throw badRequest("Invalid " + field);
        }
        return normalized;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeNullable(String value) {
        String normalized = normalize(value);
        return normalized.isEmpty() ? null : normalized;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw badRequest(field + " is required");
        }
        return value.trim();
    }

    private static String textOrNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private static Integer positiveOrNull(Integer value, String field) {
        if (value != null && value < 1) {
            throw badRequest(field + " must be positive");
        }
        return value;
    }

    private static <T> T mutate(Mutation<T> mutation) {
        try {
            return mutation.execute();
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(HttpStatus.CONFLICT, ApiErrorCode.CONFLICT.name(),
                    "Dữ liệu xung đột hoặc đang được sử dụng");
        }
    }

    private static String csv(String value) {
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    private static ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_FAILED.name(), message);
    }

    @FunctionalInterface
    private interface Mutation<T> {
        T execute();
    }
}
