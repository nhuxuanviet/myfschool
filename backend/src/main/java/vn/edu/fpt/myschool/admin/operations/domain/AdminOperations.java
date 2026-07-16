package vn.edu.fpt.myschool.admin.operations.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public final class AdminOperations {

    private AdminOperations() {
    }

    public record Timetable(
            List<Period> periods,
            List<Lesson> lessons,
            List<LessonOverride> overrides) {
    }

    public record Period(
            UUID id,
            UUID academicTermId,
            String session,
            int periodNumber,
            LocalTime startTime,
            LocalTime endTime,
            long version) {
    }

    public record Lesson(
            UUID id,
            UUID academicTermId,
            UUID schoolClassId,
            int dayOfWeek,
            String session,
            int periodNumber,
            UUID subjectId,
            String subjectName,
            UUID teacherId,
            String teacherName,
            String room,
            long version) {
    }

    public record LessonOverride(
            UUID id,
            UUID academicTermId,
            UUID schoolClassId,
            LocalDate lessonDate,
            String session,
            int periodNumber,
            String overrideType,
            UUID subjectId,
            String subjectName,
            UUID teacherId,
            String teacherName,
            String room,
            String note,
            long version) {
    }

    public record Grades(
            List<StudentSubject> subjects,
            List<Assessment> assessments) {
    }

    public record StudentSubject(
            UUID id,
            UUID studentId,
            UUID academicTermId,
            UUID subjectId,
            String subjectName,
            String assessmentMode,
            Integer annualLessonCount,
            int displayOrder,
            long version) {
    }

    public record Assessment(
            UUID id,
            UUID studentTermSubjectId,
            String assessmentKind,
            String assessmentForm,
            String displayLabel,
            Integer durationMinutes,
            String status,
            BigDecimal score,
            String outcome,
            LocalDate assessedOn,
            int displayOrder,
            long version) {
    }

    public record StudentForm(
            UUID id,
            UUID studentId,
            String studentCode,
            String studentName,
            String formType,
            String reason,
            LocalDate startsOn,
            LocalDate endsOn,
            String status,
            Instant submittedAt,
            Instant updatedAt,
            long version) {
    }

    public record Announcement(
            UUID id,
            String title,
            String body,
            String audience,
            Integer audienceGradeLevel,
            Instant publishedAt,
            Instant visibleFrom,
            Instant visibleUntil,
            long version) {
    }

    public record Activities(
            List<Event> events,
            List<EventRegistration> eventRegistrations,
            List<Club> clubs,
            List<ClubMembership> clubMemberships) {
    }

    public record Event(
            UUID id,
            String category,
            String title,
            String description,
            String location,
            Instant startsAt,
            Instant endsAt,
            String audience,
            Integer audienceGradeLevel,
            Integer capacity,
            Instant registrationDeadline,
            Instant cancellationDeadline,
            boolean registrationEnabled,
            boolean enabled,
            long version,
            int registeredCount) {
    }

    public record EventRegistration(
            UUID id,
            UUID eventId,
            UUID studentId,
            String studentCode,
            String studentName,
            String status,
            Instant registeredAt,
            long version) {
    }

    public record Club(
            UUID id,
            String category,
            String name,
            String description,
            String advisorName,
            String meetingSchedule,
            String location,
            String audience,
            Integer audienceGradeLevel,
            Integer capacity,
            Instant applicationDeadline,
            boolean acceptingApplications,
            boolean enabled,
            long version,
            int activeCount) {
    }

    public record ClubMembership(
            UUID id,
            UUID clubId,
            UUID studentId,
            String studentCode,
            String studentName,
            String status,
            Instant appliedAt,
            long version) {
    }

    public record AuditPage(
            List<AuditEvent> items,
            int page,
            int size,
            long totalElements,
            int totalPages) {
    }

    public record AuditEvent(
            UUID id,
            UUID actorUserId,
            String actorName,
            String action,
            String entityType,
            UUID entityId,
            String changedFields,
            Instant occurredAt) {
    }
}
