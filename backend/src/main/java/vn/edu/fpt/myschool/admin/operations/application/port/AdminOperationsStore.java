package vn.edu.fpt.myschool.admin.operations.application.port;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import vn.edu.fpt.myschool.admin.operations.domain.AdminOperations;

public interface AdminOperationsStore {

    AdminOperations.Timetable loadTimetable(UUID academicTermId, UUID schoolClassId);

    UUID createLesson(UUID academicTermId, UUID schoolClassId, int dayOfWeek, String session,
            int periodNumber, UUID subjectId, UUID teacherId, String room, Instant now);

    boolean updateLesson(UUID id, UUID subjectId, UUID teacherId, String room,
            long version, Instant now);

    boolean deleteLesson(UUID id, long version);

    UUID createOverride(UUID academicTermId, UUID schoolClassId, LocalDate lessonDate,
            String session, int periodNumber, String overrideType, UUID subjectId,
            UUID teacherId, String room, String note, Instant now);

    boolean deleteOverride(UUID id, long version);

    AdminOperations.Grades loadGrades(UUID studentId, UUID academicTermId);

    UUID assignSubject(UUID studentId, UUID academicTermId, UUID subjectId,
            String assessmentMode, Integer annualLessonCount, int displayOrder, Instant now);

    UUID createAssessment(UUID studentTermSubjectId, String assessmentKind,
            String assessmentForm, String displayLabel, Integer durationMinutes, String status,
            BigDecimal score, String outcome, LocalDate assessedOn, int displayOrder, Instant now);

    boolean updateAssessment(UUID id, String assessmentForm, String displayLabel,
            Integer durationMinutes, String status, BigDecimal score, String outcome,
            LocalDate assessedOn, long version, Instant now);

    List<AdminOperations.StudentForm> loadForms(String status);

    boolean updateFormStatus(UUID id, String status, String note, long version, Instant now);

    List<AdminOperations.Announcement> loadAnnouncements();

    UUID createAnnouncement(String title, String body, String audience,
            Integer gradeLevel, Instant publishedAt, Instant visibleFrom,
            Instant visibleUntil, Instant now);

    boolean updateAnnouncement(UUID id, String title, String body, String audience,
            Integer gradeLevel, Instant publishedAt, Instant visibleFrom,
            Instant visibleUntil, long version, Instant now);

    boolean deleteAnnouncement(UUID id, long version);

    AdminOperations.Activities loadActivities();

    UUID createEvent(String category, String title, String description, String location,
            Instant startsAt, Instant endsAt, String audience, Integer gradeLevel,
            Integer capacity, Instant registrationDeadline, Instant cancellationDeadline,
            boolean registrationEnabled, Instant now);

    boolean updateEvent(UUID id, String category, String title, String description,
            String location, Instant startsAt, Instant endsAt, String audience,
            Integer gradeLevel, Integer capacity, Instant registrationDeadline,
            Instant cancellationDeadline, boolean registrationEnabled, boolean enabled,
            long version, Instant now);

    UUID createClub(String category, String name, String description, String advisorName,
            String meetingSchedule, String location, String audience, Integer gradeLevel,
            Integer capacity, Instant applicationDeadline, boolean acceptingApplications,
            Instant now);

    boolean updateClub(UUID id, String category, String name, String description,
            String advisorName, String meetingSchedule, String location, String audience,
            Integer gradeLevel, Integer capacity, Instant applicationDeadline,
            boolean acceptingApplications, boolean enabled, long version, Instant now);

    boolean updateEventRegistration(UUID id, String status, long version, Instant now);

    boolean updateClubMembership(UUID id, String status, long version, Instant now);

    AdminOperations.AuditPage loadAudit(String query, int page, int size);

    List<AdminOperations.AuditEvent> exportAudit(String query, int limit);

    void audit(UUID actorUserId, String action, String entityType, UUID entityId,
            String changedFields, Instant occurredAt);
}
