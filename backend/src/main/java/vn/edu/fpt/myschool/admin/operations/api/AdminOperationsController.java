package vn.edu.fpt.myschool.admin.operations.api;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.fpt.myschool.admin.operations.application.AdminOperationsService;
import vn.edu.fpt.myschool.admin.operations.domain.AdminOperations;

@RestController
@RequestMapping("/api/v1/admin/operations")
@Tag(name = "Admin operations", description = "Timetable, grades, forms, announcements, activities and audit")
@SecurityRequirement(name = "bearerAuth")
public class AdminOperationsController {

    private final AdminOperationsService service;

    public AdminOperationsController(AdminOperationsService service) {
        this.service = service;
    }

    @GetMapping("/timetable")
    public AdminOperations.Timetable getTimetable(
            @RequestParam UUID academicTermId,
            @RequestParam UUID schoolClassId) {
        return service.getTimetable(academicTermId, schoolClassId);
    }

    @PostMapping("/timetable/lessons")
    public ResponseEntity<MutationResponse> createLesson(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody LessonRequest request) {
        UUID id = service.createLesson(actorId(jwt), request.academicTermId(),
                request.schoolClassId(), request.dayOfWeek(), request.session(),
                request.periodNumber(), request.subjectId(), request.teacherName(), request.room());
        return created("/api/v1/admin/operations/timetable/lessons/" + id, id);
    }

    @PutMapping("/timetable/lessons/{lessonId}")
    public ResponseEntity<Void> updateLesson(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID lessonId,
            @Valid @RequestBody UpdateLessonRequest request) {
        service.updateLesson(actorId(jwt), lessonId, request.subjectId(),
                request.teacherName(), request.room(), request.version());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/timetable/lessons/{lessonId}")
    public ResponseEntity<Void> deleteLesson(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID lessonId,
            @RequestParam long version) {
        service.deleteLesson(actorId(jwt), lessonId, version);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/timetable/overrides")
    public ResponseEntity<MutationResponse> createOverride(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody OverrideRequest request) {
        UUID id = service.createOverride(actorId(jwt), request.academicTermId(),
                request.schoolClassId(), request.lessonDate(), request.session(),
                request.periodNumber(), request.overrideType(), request.subjectId(),
                request.teacherName(), request.room(), request.note());
        return created("/api/v1/admin/operations/timetable/overrides/" + id, id);
    }

    @DeleteMapping("/timetable/overrides/{overrideId}")
    public ResponseEntity<Void> deleteOverride(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID overrideId,
            @RequestParam long version) {
        service.deleteOverride(actorId(jwt), overrideId, version);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/grades")
    public AdminOperations.Grades getGrades(
            @RequestParam UUID studentId,
            @RequestParam UUID academicTermId) {
        return service.getGrades(studentId, academicTermId);
    }

    @PostMapping("/grades/subjects")
    public ResponseEntity<MutationResponse> assignSubject(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AssignSubjectRequest request) {
        UUID id = service.assignSubject(actorId(jwt), request.studentId(),
                request.academicTermId(), request.subjectId(), request.assessmentMode(),
                request.annualLessonCount(), request.displayOrder());
        return created("/api/v1/admin/operations/grades/subjects/" + id, id);
    }

    @PostMapping("/grades/assessments")
    public ResponseEntity<MutationResponse> createAssessment(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AssessmentRequest request) {
        UUID id = service.createAssessment(actorId(jwt), request.studentTermSubjectId(),
                request.assessmentKind(), request.assessmentForm(), request.displayLabel(),
                request.durationMinutes(), request.status(), request.score(), request.outcome(),
                request.assessedOn(), request.displayOrder());
        return created("/api/v1/admin/operations/grades/assessments/" + id, id);
    }

    @PutMapping("/grades/assessments/{assessmentId}")
    public ResponseEntity<Void> updateAssessment(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID assessmentId,
            @Valid @RequestBody UpdateAssessmentRequest request) {
        service.updateAssessment(actorId(jwt), assessmentId, request.assessmentForm(),
                request.displayLabel(), request.durationMinutes(), request.status(),
                request.score(), request.outcome(), request.assessedOn(), request.version());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/forms")
    public List<AdminOperations.StudentForm> getForms(
            @RequestParam(required = false) String status) {
        return service.getForms(status);
    }

    @PatchMapping("/forms/{formId}/status")
    public ResponseEntity<Void> updateFormStatus(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID formId,
            @Valid @RequestBody StatusRequest request) {
        service.updateFormStatus(actorId(jwt), formId, request.status(), request.note(), request.version());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/announcements")
    public List<AdminOperations.Announcement> getAnnouncements() {
        return service.getAnnouncements();
    }

    @PostMapping("/announcements")
    public ResponseEntity<MutationResponse> createAnnouncement(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AnnouncementRequest request) {
        UUID id = service.createAnnouncement(actorId(jwt), request.title(), request.body(),
                request.audience(), request.audienceGradeLevel(), request.publishedAt(),
                request.visibleFrom(), request.visibleUntil());
        return created("/api/v1/admin/operations/announcements/" + id, id);
    }

    @PutMapping("/announcements/{announcementId}")
    public ResponseEntity<Void> updateAnnouncement(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID announcementId,
            @Valid @RequestBody UpdateAnnouncementRequest request) {
        service.updateAnnouncement(actorId(jwt), announcementId, request.title(), request.body(),
                request.audience(), request.audienceGradeLevel(), request.publishedAt(),
                request.visibleFrom(), request.visibleUntil(), request.version());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/announcements/{announcementId}")
    public ResponseEntity<Void> deleteAnnouncement(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID announcementId,
            @RequestParam long version) {
        service.deleteAnnouncement(actorId(jwt), announcementId, version);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/activities")
    public AdminOperations.Activities getActivities() {
        return service.getActivities();
    }

    @PostMapping("/activities/events")
    public ResponseEntity<MutationResponse> createEvent(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody EventRequest request) {
        UUID id = service.createEvent(actorId(jwt), request.category(), request.title(),
                request.description(), request.location(), request.startsAt(), request.endsAt(),
                request.audience(), request.audienceGradeLevel(), request.capacity(),
                request.registrationDeadline(), request.cancellationDeadline(),
                request.registrationEnabled());
        return created("/api/v1/admin/operations/activities/events/" + id, id);
    }

    @PutMapping("/activities/events/{eventId}")
    public ResponseEntity<Void> updateEvent(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID eventId,
            @Valid @RequestBody UpdateEventRequest request) {
        service.updateEvent(actorId(jwt), eventId, request.category(), request.title(),
                request.description(), request.location(), request.startsAt(), request.endsAt(),
                request.audience(), request.audienceGradeLevel(), request.capacity(),
                request.registrationDeadline(), request.cancellationDeadline(),
                request.registrationEnabled(), request.enabled(), request.version());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/activities/clubs")
    public ResponseEntity<MutationResponse> createClub(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ClubRequest request) {
        UUID id = service.createClub(actorId(jwt), request.category(), request.name(),
                request.description(), request.advisorName(), request.meetingSchedule(),
                request.location(), request.audience(), request.audienceGradeLevel(),
                request.capacity(), request.applicationDeadline(), request.acceptingApplications());
        return created("/api/v1/admin/operations/activities/clubs/" + id, id);
    }

    @PutMapping("/activities/clubs/{clubId}")
    public ResponseEntity<Void> updateClub(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID clubId,
            @Valid @RequestBody UpdateClubRequest request) {
        service.updateClub(actorId(jwt), clubId, request.category(), request.name(),
                request.description(), request.advisorName(), request.meetingSchedule(),
                request.location(), request.audience(), request.audienceGradeLevel(),
                request.capacity(), request.applicationDeadline(), request.acceptingApplications(),
                request.enabled(), request.version());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/activities/event-registrations/{registrationId}")
    public ResponseEntity<Void> updateEventRegistration(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID registrationId,
            @Valid @RequestBody StatusRequest request) {
        service.updateEventRegistration(actorId(jwt), registrationId,
                request.status(), request.version());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/activities/club-memberships/{membershipId}")
    public ResponseEntity<Void> updateClubMembership(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID membershipId,
            @Valid @RequestBody StatusRequest request) {
        service.updateClubMembership(actorId(jwt), membershipId,
                request.status(), request.version());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/audit")
    public AdminOperations.AuditPage getAudit(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.getAudit(query, page, size);
    }

    @GetMapping(value = "/audit/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportAudit(
            @RequestParam(required = false) String query) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=nhat-ky-quan-tri.csv")
                .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                .body(service.exportAuditCsv(query));
    }

    private static UUID actorId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    private static ResponseEntity<MutationResponse> created(String path, UUID id) {
        return ResponseEntity.created(URI.create(path)).body(new MutationResponse(id));
    }

    public record MutationResponse(UUID id) {
    }

    public record LessonRequest(
            @NotNull UUID academicTermId,
            @NotNull UUID schoolClassId,
            @Min(1) @Max(7) int dayOfWeek,
            @NotBlank String session,
            @Min(1) @Max(5) int periodNumber,
            @NotNull UUID subjectId,
            @Size(max = 120) String teacherName,
            @Size(max = 32) String room) {
    }

    public record UpdateLessonRequest(
            @NotNull UUID subjectId,
            @Size(max = 120) String teacherName,
            @Size(max = 32) String room,
            @Min(0) long version) {
    }

    public record OverrideRequest(
            @NotNull UUID academicTermId,
            @NotNull UUID schoolClassId,
            @NotNull LocalDate lessonDate,
            @NotBlank String session,
            @Min(1) @Max(5) int periodNumber,
            @NotBlank String overrideType,
            UUID subjectId,
            @Size(max = 120) String teacherName,
            @Size(max = 32) String room,
            @Size(max = 500) String note) {
    }

    public record AssignSubjectRequest(
            @NotNull UUID studentId,
            @NotNull UUID academicTermId,
            @NotNull UUID subjectId,
            @NotBlank String assessmentMode,
            Integer annualLessonCount,
            @Min(1) int displayOrder) {
    }

    public record AssessmentRequest(
            @NotNull UUID studentTermSubjectId,
            @NotBlank String assessmentKind,
            @NotBlank String assessmentForm,
            @NotBlank @Size(max = 120) String displayLabel,
            Integer durationMinutes,
            @NotBlank String status,
            BigDecimal score,
            String outcome,
            LocalDate assessedOn,
            @Min(1) int displayOrder) {
    }

    public record UpdateAssessmentRequest(
            @NotBlank String assessmentForm,
            @NotBlank @Size(max = 120) String displayLabel,
            Integer durationMinutes,
            @NotBlank String status,
            BigDecimal score,
            String outcome,
            LocalDate assessedOn,
            @Min(0) long version) {
    }

    public record StatusRequest(
            @NotBlank String status,
            @Size(max = 500) String note,
            @Min(0) long version) {
    }

    public record AnnouncementRequest(
            @NotBlank @Size(max = 160) String title,
            @NotBlank String body,
            @NotBlank String audience,
            Integer audienceGradeLevel,
            @NotNull Instant publishedAt,
            @NotNull Instant visibleFrom,
            Instant visibleUntil) {
    }

    public record UpdateAnnouncementRequest(
            @NotBlank @Size(max = 160) String title,
            @NotBlank String body,
            @NotBlank String audience,
            Integer audienceGradeLevel,
            @NotNull Instant publishedAt,
            @NotNull Instant visibleFrom,
            Instant visibleUntil,
            @Min(0) long version) {
    }

    public record EventRequest(
            @NotBlank String category,
            @NotBlank @Size(max = 160) String title,
            @NotBlank String description,
            @NotBlank @Size(max = 160) String location,
            @NotNull Instant startsAt,
            @NotNull Instant endsAt,
            @NotBlank String audience,
            Integer audienceGradeLevel,
            Integer capacity,
            Instant registrationDeadline,
            Instant cancellationDeadline,
            boolean registrationEnabled) {
    }

    public record UpdateEventRequest(
            @NotBlank String category,
            @NotBlank @Size(max = 160) String title,
            @NotBlank String description,
            @NotBlank @Size(max = 160) String location,
            @NotNull Instant startsAt,
            @NotNull Instant endsAt,
            @NotBlank String audience,
            Integer audienceGradeLevel,
            Integer capacity,
            Instant registrationDeadline,
            Instant cancellationDeadline,
            boolean registrationEnabled,
            boolean enabled,
            @Min(0) long version) {
    }

    public record ClubRequest(
            @NotBlank String category,
            @NotBlank @Size(max = 160) String name,
            @NotBlank String description,
            @NotBlank @Size(max = 120) String advisorName,
            @NotBlank @Size(max = 200) String meetingSchedule,
            @NotBlank @Size(max = 160) String location,
            @NotBlank String audience,
            Integer audienceGradeLevel,
            Integer capacity,
            Instant applicationDeadline,
            boolean acceptingApplications) {
    }

    public record UpdateClubRequest(
            @NotBlank String category,
            @NotBlank @Size(max = 160) String name,
            @NotBlank String description,
            @NotBlank @Size(max = 120) String advisorName,
            @NotBlank @Size(max = 200) String meetingSchedule,
            @NotBlank @Size(max = 160) String location,
            @NotBlank String audience,
            Integer audienceGradeLevel,
            Integer capacity,
            Instant applicationDeadline,
            boolean acceptingApplications,
            boolean enabled,
            @Min(0) long version) {
    }
}
