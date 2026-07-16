package vn.edu.fpt.myschool.admin.operations.infrastructure.persistence;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import vn.edu.fpt.myschool.admin.operations.application.port.AdminOperationsStore;
import vn.edu.fpt.myschool.admin.operations.domain.AdminOperations;

@Repository
class JdbcAdminOperationsStore implements AdminOperationsStore {

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    JdbcAdminOperationsStore(
            JdbcTemplate jdbcTemplate,
            NamedParameterJdbcTemplate namedJdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedJdbcTemplate = namedJdbcTemplate;
    }

    @Override
    public AdminOperations.Timetable loadTimetable(UUID academicTermId, UUID schoolClassId) {
        Map<String, Object> parameters = Map.of(
                "termId", academicTermId,
                "classId", schoolClassId);
        List<AdminOperations.Period> periods = namedJdbcTemplate.query("""
                SELECT id, academic_term_id, session, period_number,
                       start_time, end_time, version
                FROM term_period_definitions
                WHERE academic_term_id = :termId
                ORDER BY CASE session WHEN 'MORNING' THEN 0 ELSE 1 END,
                         period_number, id
                """, parameters, this::mapPeriod);
        List<AdminOperations.Lesson> lessons = namedJdbcTemplate.query("""
                SELECT entry.id, entry.academic_term_id, entry.school_class_id,
                       entry.day_of_week, entry.session, entry.period_number,
                       entry.subject_id, subject.name AS subject_name,
                       entry.teacher_name, entry.room, entry.version
                FROM class_timetable_entries entry
                INNER JOIN subjects subject ON subject.id = entry.subject_id
                WHERE entry.academic_term_id = :termId
                  AND entry.school_class_id = :classId
                ORDER BY entry.day_of_week,
                         CASE entry.session WHEN 'MORNING' THEN 0 ELSE 1 END,
                         entry.period_number, entry.id
                """, parameters, this::mapLesson);
        List<AdminOperations.LessonOverride> overrides = namedJdbcTemplate.query("""
                SELECT override_entry.id, override_entry.academic_term_id,
                       override_entry.school_class_id, override_entry.lesson_date,
                       override_entry.session, override_entry.period_number,
                       override_entry.override_type, override_entry.subject_id,
                       subject.name AS subject_name, override_entry.teacher_name,
                       override_entry.room, override_entry.note, override_entry.version
                FROM timetable_overrides override_entry
                LEFT JOIN subjects subject ON subject.id = override_entry.subject_id
                WHERE override_entry.academic_term_id = :termId
                  AND override_entry.school_class_id = :classId
                ORDER BY override_entry.lesson_date DESC,
                         CASE override_entry.session WHEN 'MORNING' THEN 0 ELSE 1 END,
                         override_entry.period_number, override_entry.id
                """, parameters, this::mapOverride);
        return new AdminOperations.Timetable(periods, lessons, overrides);
    }

    @Override
    public UUID createLesson(
            UUID academicTermId,
            UUID schoolClassId,
            int dayOfWeek,
            String session,
            int periodNumber,
            UUID subjectId,
            String teacherName,
            String room,
            Instant now) {
        UUID id = UUID.randomUUID();
        namedJdbcTemplate.update("""
                INSERT INTO class_timetable_entries (
                    id, academic_term_id, class_name, school_class_id, day_of_week,
                    session, period_number, subject_id, teacher_name, room,
                    created_at, updated_at, version
                ) VALUES (
                    :id, :termId,
                    (SELECT code FROM school_classes WHERE id = :classId),
                    :classId, :dayOfWeek, :session, :periodNumber, :subjectId,
                    :teacherName, :room, :now, :now, 0
                )
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("termId", academicTermId)
                .addValue("classId", schoolClassId)
                .addValue("dayOfWeek", dayOfWeek)
                .addValue("session", session)
                .addValue("periodNumber", periodNumber)
                .addValue("subjectId", subjectId)
                .addValue("teacherName", teacherName)
                .addValue("room", room)
                .addValue("now", Timestamp.from(now)));
        return id;
    }

    @Override
    public boolean updateLesson(
            UUID id,
            UUID subjectId,
            String teacherName,
            String room,
            long version,
            Instant now) {
        return namedJdbcTemplate.update("""
                UPDATE class_timetable_entries
                SET subject_id = :subjectId,
                    teacher_name = :teacherName,
                    room = :room,
                    version = version + 1,
                    updated_at = :now
                WHERE id = :id AND version = :version
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("subjectId", subjectId)
                .addValue("teacherName", teacherName)
                .addValue("room", room)
                .addValue("version", version)
                .addValue("now", Timestamp.from(now))) == 1;
    }

    @Override
    public boolean deleteLesson(UUID id, long version) {
        return namedJdbcTemplate.update(
                "DELETE FROM class_timetable_entries WHERE id = :id AND version = :version",
                Map.of("id", id, "version", version)) == 1;
    }

    @Override
    public UUID createOverride(
            UUID academicTermId,
            UUID schoolClassId,
            LocalDate lessonDate,
            String session,
            int periodNumber,
            String overrideType,
            UUID subjectId,
            String teacherName,
            String room,
            String note,
            Instant now) {
        UUID id = UUID.randomUUID();
        namedJdbcTemplate.update("""
                INSERT INTO timetable_overrides (
                    id, academic_term_id, class_name, school_class_id, lesson_date,
                    session, period_number, override_type, subject_id, teacher_name,
                    room, note, created_at, updated_at, version
                ) VALUES (
                    :id, :termId,
                    (SELECT code FROM school_classes WHERE id = :classId),
                    :classId, :lessonDate, :session, :periodNumber, :overrideType,
                    :subjectId, :teacherName, :room, :note, :now, :now, 0
                )
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("termId", academicTermId)
                .addValue("classId", schoolClassId)
                .addValue("lessonDate", lessonDate)
                .addValue("session", session)
                .addValue("periodNumber", periodNumber)
                .addValue("overrideType", overrideType)
                .addValue("subjectId", subjectId)
                .addValue("teacherName", teacherName)
                .addValue("room", room)
                .addValue("note", note)
                .addValue("now", Timestamp.from(now)));
        return id;
    }

    @Override
    public boolean deleteOverride(UUID id, long version) {
        return namedJdbcTemplate.update(
                "DELETE FROM timetable_overrides WHERE id = :id AND version = :version",
                Map.of("id", id, "version", version)) == 1;
    }

    @Override
    public AdminOperations.Grades loadGrades(UUID studentId, UUID academicTermId) {
        Map<String, Object> parameters = Map.of(
                "studentId", studentId,
                "termId", academicTermId);
        List<AdminOperations.StudentSubject> subjects = namedJdbcTemplate.query("""
                SELECT enrollment.id, enrollment.student_id, enrollment.academic_term_id,
                       enrollment.subject_id, subject.name AS subject_name,
                       enrollment.assessment_mode, enrollment.annual_lesson_count,
                       enrollment.display_order, enrollment.version
                FROM student_term_subjects enrollment
                INNER JOIN subjects subject ON subject.id = enrollment.subject_id
                WHERE enrollment.student_id = :studentId
                  AND enrollment.academic_term_id = :termId
                ORDER BY enrollment.display_order, enrollment.id
                """, parameters, this::mapStudentSubject);
        List<AdminOperations.Assessment> assessments = namedJdbcTemplate.query("""
                SELECT assessment.id, assessment.student_term_subject_id,
                       assessment.assessment_kind, assessment.assessment_form,
                       assessment.display_label, assessment.duration_minutes,
                       assessment.status, assessment.score, assessment.outcome,
                       assessment.assessed_on, assessment.display_order,
                       assessment.version
                FROM grade_assessments assessment
                INNER JOIN student_term_subjects enrollment
                    ON enrollment.id = assessment.student_term_subject_id
                WHERE enrollment.student_id = :studentId
                  AND enrollment.academic_term_id = :termId
                ORDER BY enrollment.display_order, assessment.display_order, assessment.id
                """, parameters, this::mapAssessment);
        return new AdminOperations.Grades(subjects, assessments);
    }

    @Override
    public UUID assignSubject(
            UUID studentId,
            UUID academicTermId,
            UUID subjectId,
            String assessmentMode,
            Integer annualLessonCount,
            int displayOrder,
            Instant now) {
        UUID id = UUID.randomUUID();
        namedJdbcTemplate.update("""
                INSERT INTO student_term_subjects (
                    id, student_id, academic_term_id, subject_id, assessment_mode,
                    annual_lesson_count, display_order, created_at, updated_at, version
                ) VALUES (
                    :id, :studentId, :termId, :subjectId, :assessmentMode,
                    :annualLessonCount, :displayOrder, :now, :now, 0
                )
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("studentId", studentId)
                .addValue("termId", academicTermId)
                .addValue("subjectId", subjectId)
                .addValue("assessmentMode", assessmentMode)
                .addValue("annualLessonCount", annualLessonCount)
                .addValue("displayOrder", displayOrder)
                .addValue("now", Timestamp.from(now)));
        return id;
    }

    @Override
    public UUID createAssessment(
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
            Instant now) {
        UUID id = UUID.randomUUID();
        namedJdbcTemplate.update("""
                INSERT INTO grade_assessments (
                    id, student_term_subject_id, assessment_mode, assessment_kind,
                    assessment_form, display_label, duration_minutes, status, score,
                    outcome, assessed_on, display_order, created_at, updated_at, version
                ) VALUES (
                    :id, :enrollmentId,
                    (SELECT assessment_mode FROM student_term_subjects WHERE id = :enrollmentId),
                    :kind, :form, :label, :duration, :status, :score, :outcome,
                    :assessedOn, :displayOrder, :now, :now, 0
                )
                """, assessmentParameters(id, studentTermSubjectId, assessmentKind,
                        assessmentForm, displayLabel, durationMinutes, status, score,
                        outcome, assessedOn, displayOrder, now));
        return id;
    }

    @Override
    public boolean updateAssessment(
            UUID id,
            String assessmentForm,
            String displayLabel,
            Integer durationMinutes,
            String status,
            BigDecimal score,
            String outcome,
            LocalDate assessedOn,
            long version,
            Instant now) {
        return namedJdbcTemplate.update("""
                UPDATE grade_assessments
                SET assessment_form = :form,
                    display_label = :label,
                    duration_minutes = :duration,
                    status = :status,
                    score = :score,
                    outcome = :outcome,
                    assessed_on = :assessedOn,
                    version = version + 1,
                    updated_at = :now
                WHERE id = :id AND version = :version
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("form", assessmentForm)
                .addValue("label", displayLabel)
                .addValue("duration", durationMinutes)
                .addValue("status", status)
                .addValue("score", score)
                .addValue("outcome", outcome)
                .addValue("assessedOn", assessedOn)
                .addValue("version", version)
                .addValue("now", Timestamp.from(now))) == 1;
    }

    @Override
    public List<AdminOperations.StudentForm> loadForms(String status) {
        return namedJdbcTemplate.query("""
                SELECT form.id, form.student_id, student.student_code,
                       student.full_name AS student_name, form.form_type, form.reason,
                       form.starts_on, form.ends_on, form.status, form.submitted_at,
                       form.updated_at, form.version
                FROM student_forms form
                INNER JOIN students student ON student.id = form.student_id
                WHERE CAST(:status AS VARCHAR) IS NULL OR form.status = :status
                ORDER BY form.updated_at DESC, form.id DESC
                LIMIT 200
                """, new MapSqlParameterSource("status", status), this::mapStudentForm);
    }

    @Override
    public boolean updateFormStatus(
            UUID id,
            String status,
            String note,
            long version,
            Instant now) {
        int updated = namedJdbcTemplate.update("""
                UPDATE student_forms
                SET status = :status, updated_at = :now, version = version + 1
                WHERE id = :id AND version = :version
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("status", status)
                .addValue("version", version)
                .addValue("now", Timestamp.from(now)));
        if (updated == 1) {
            namedJdbcTemplate.update("""
                    INSERT INTO student_form_status_history (
                        id, form_id, sequence_number, status, occurred_at, note
                    ) VALUES (
                        :historyId, :formId,
                        (SELECT COALESCE(MAX(sequence_number), 0) + 1
                         FROM student_form_status_history WHERE form_id = :formId),
                        :status, :now, :note
                    )
                    """, new MapSqlParameterSource()
                    .addValue("historyId", UUID.randomUUID())
                    .addValue("formId", id)
                    .addValue("status", status)
                    .addValue("now", Timestamp.from(now))
                    .addValue("note", note));
        }
        return updated == 1;
    }

    @Override
    public List<AdminOperations.Announcement> loadAnnouncements() {
        return jdbcTemplate.query("""
                SELECT id, title, body, audience, audience_grade_level, published_at,
                       visible_from, visible_until, version
                FROM announcements
                ORDER BY published_at DESC, id DESC
                LIMIT 200
                """, this::mapAnnouncement);
    }

    @Override
    public UUID createAnnouncement(
            String title,
            String body,
            String audience,
            Integer gradeLevel,
            Instant publishedAt,
            Instant visibleFrom,
            Instant visibleUntil,
            Instant now) {
        UUID id = UUID.randomUUID();
        namedJdbcTemplate.update("""
                INSERT INTO announcements (
                    id, title, body, audience, audience_grade_level, published_at,
                    visible_from, visible_until, created_at, updated_at, version
                ) VALUES (
                    :id, :title, :body, :audience, :gradeLevel, :publishedAt,
                    :visibleFrom, :visibleUntil, :now, :now, 0
                )
                """, announcementParameters(id, title, body, audience, gradeLevel,
                        publishedAt, visibleFrom, visibleUntil, 0, now));
        return id;
    }

    @Override
    public boolean updateAnnouncement(
            UUID id,
            String title,
            String body,
            String audience,
            Integer gradeLevel,
            Instant publishedAt,
            Instant visibleFrom,
            Instant visibleUntil,
            long version,
            Instant now) {
        return namedJdbcTemplate.update("""
                UPDATE announcements
                SET title = :title, body = :body, audience = :audience,
                    audience_grade_level = :gradeLevel, published_at = :publishedAt,
                    visible_from = :visibleFrom, visible_until = :visibleUntil,
                    updated_at = :now, version = version + 1
                WHERE id = :id AND version = :version
                """, announcementParameters(id, title, body, audience, gradeLevel,
                        publishedAt, visibleFrom, visibleUntil, version, now)) == 1;
    }

    @Override
    public boolean deleteAnnouncement(UUID id, long version) {
        return namedJdbcTemplate.update(
                "DELETE FROM announcements WHERE id = :id AND version = :version",
                Map.of("id", id, "version", version)) == 1;
    }

    @Override
    public AdminOperations.Activities loadActivities() {
        List<AdminOperations.Event> events = jdbcTemplate.query("""
                SELECT event.id, event.category, event.title, event.description,
                       event.location, event.starts_at, event.ends_at, event.audience,
                       event.audience_grade_level, event.capacity,
                       event.registration_deadline, event.cancellation_deadline,
                       event.registration_enabled, event.enabled, event.version,
                       COUNT(registration.id) FILTER (WHERE registration.status = 'REGISTERED')
                           AS registered_count
                FROM school_events event
                LEFT JOIN student_event_registrations registration
                    ON registration.event_id = event.id
                GROUP BY event.id
                ORDER BY event.starts_at DESC, event.id DESC
                LIMIT 200
                """, this::mapEvent);
        List<AdminOperations.EventRegistration> registrations = jdbcTemplate.query("""
                SELECT registration.id, registration.event_id, registration.student_id,
                       student.student_code, student.full_name AS student_name,
                       registration.status, registration.registered_at,
                       registration.version
                FROM student_event_registrations registration
                INNER JOIN students student ON student.id = registration.student_id
                ORDER BY registration.registered_at DESC, registration.id DESC
                LIMIT 500
                """, this::mapEventRegistration);
        List<AdminOperations.Club> clubs = jdbcTemplate.query("""
                SELECT club.id, club.category, club.name, club.description,
                       club.advisor_name, club.meeting_schedule, club.location,
                       club.audience, club.audience_grade_level, club.capacity,
                       club.application_deadline, club.accepting_applications,
                       club.enabled, club.version,
                       COUNT(membership.id) FILTER (WHERE membership.status = 'ACTIVE')
                           AS active_count
                FROM school_clubs club
                LEFT JOIN student_club_memberships membership ON membership.club_id = club.id
                GROUP BY club.id
                ORDER BY club.name, club.id
                LIMIT 200
                """, this::mapClub);
        List<AdminOperations.ClubMembership> memberships = jdbcTemplate.query("""
                SELECT membership.id, membership.club_id, membership.student_id,
                       student.student_code, student.full_name AS student_name,
                       membership.status, membership.applied_at, membership.version
                FROM student_club_memberships membership
                INNER JOIN students student ON student.id = membership.student_id
                ORDER BY membership.applied_at DESC, membership.id DESC
                LIMIT 500
                """, this::mapClubMembership);
        return new AdminOperations.Activities(events, registrations, clubs, memberships);
    }

    @Override
    public UUID createEvent(
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
            Instant now) {
        UUID id = UUID.randomUUID();
        namedJdbcTemplate.update("""
                INSERT INTO school_events (
                    id, category, title, description, location, starts_at, ends_at,
                    audience, audience_grade_level, capacity, registration_deadline,
                    cancellation_deadline, registration_enabled, enabled,
                    created_at, updated_at, version
                ) VALUES (
                    :id, :category, :title, :description, :location, :startsAt, :endsAt,
                    :audience, :gradeLevel, :capacity, :registrationDeadline,
                    :cancellationDeadline, :registrationEnabled, TRUE, :now, :now, 0
                )
                """, eventParameters(id, category, title, description, location,
                        startsAt, endsAt, audience, gradeLevel, capacity,
                        registrationDeadline, cancellationDeadline, registrationEnabled,
                        true, 0, now));
        return id;
    }

    @Override
    public boolean updateEvent(
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
            long version,
            Instant now) {
        return namedJdbcTemplate.update("""
                UPDATE school_events
                SET category = :category, title = :title, description = :description,
                    location = :location, starts_at = :startsAt, ends_at = :endsAt,
                    audience = :audience, audience_grade_level = :gradeLevel,
                    capacity = :capacity, registration_deadline = :registrationDeadline,
                    cancellation_deadline = :cancellationDeadline,
                    registration_enabled = :registrationEnabled, enabled = :enabled,
                    updated_at = :now, version = version + 1
                WHERE id = :id AND version = :version
                """, eventParameters(id, category, title, description, location,
                        startsAt, endsAt, audience, gradeLevel, capacity,
                        registrationDeadline, cancellationDeadline, registrationEnabled,
                        enabled, version, now)) == 1;
    }

    @Override
    public UUID createClub(
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
            Instant now) {
        UUID id = UUID.randomUUID();
        namedJdbcTemplate.update("""
                INSERT INTO school_clubs (
                    id, category, name, description, advisor_name, meeting_schedule,
                    location, audience, audience_grade_level, capacity,
                    application_deadline, accepting_applications, enabled,
                    created_at, updated_at, version
                ) VALUES (
                    :id, :category, :name, :description, :advisorName, :schedule,
                    :location, :audience, :gradeLevel, :capacity, :deadline,
                    :accepting, TRUE, :now, :now, 0
                )
                """, clubParameters(id, category, name, description, advisorName,
                        meetingSchedule, location, audience, gradeLevel, capacity,
                        applicationDeadline, acceptingApplications, true, 0, now));
        return id;
    }

    @Override
    public boolean updateClub(
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
            long version,
            Instant now) {
        return namedJdbcTemplate.update("""
                UPDATE school_clubs
                SET category = :category, name = :name, description = :description,
                    advisor_name = :advisorName, meeting_schedule = :schedule,
                    location = :location, audience = :audience,
                    audience_grade_level = :gradeLevel, capacity = :capacity,
                    application_deadline = :deadline,
                    accepting_applications = :accepting, enabled = :enabled,
                    updated_at = :now, version = version + 1
                WHERE id = :id AND version = :version
                """, clubParameters(id, category, name, description, advisorName,
                        meetingSchedule, location, audience, gradeLevel, capacity,
                        applicationDeadline, acceptingApplications, enabled, version, now)) == 1;
    }

    @Override
    public boolean updateEventRegistration(UUID id, String status, long version, Instant now) {
        return namedJdbcTemplate.update("""
                UPDATE student_event_registrations
                SET status = :status,
                    cancelled_at = CASE WHEN :status = 'CANCELLED' THEN :now ELSE NULL END,
                    updated_at = :now,
                    version = version + 1
                WHERE id = :id AND version = :version
                """, mutationParameters(id, status, version, now)) == 1;
    }

    @Override
    public boolean updateClubMembership(UUID id, String status, long version, Instant now) {
        return namedJdbcTemplate.update("""
                UPDATE student_club_memberships
                SET status = :status, updated_at = :now, version = version + 1
                WHERE id = :id AND version = :version
                """, mutationParameters(id, status, version, now)) == 1;
    }

    @Override
    public AdminOperations.AuditPage loadAudit(String query, int page, int size) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("query", query)
                .addValue("limit", size)
                .addValue("offset", page * size);
        String predicate = """
                WHERE CAST(:query AS VARCHAR) IS NULL
                   OR lower(audit.action) LIKE lower('%' || CAST(:query AS VARCHAR) || '%')
                   OR lower(audit.entity_type) LIKE lower('%' || CAST(:query AS VARCHAR) || '%')
                   OR lower(COALESCE(profile.full_name, '')) LIKE lower('%' || CAST(:query AS VARCHAR) || '%')
                """;
        Long total = namedJdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM admin_audit_events audit
                LEFT JOIN admin_profiles profile ON profile.user_id = audit.actor_user_id
                """ + predicate, parameters, Long.class);
        List<AdminOperations.AuditEvent> items = namedJdbcTemplate.query("""
                SELECT audit.id, audit.actor_user_id,
                       COALESCE(profile.full_name, 'Quản trị viên') AS actor_name,
                       audit.action, audit.entity_type, audit.entity_id,
                       audit.changed_fields::text AS changed_fields,
                       audit.occurred_at
                FROM admin_audit_events audit
                LEFT JOIN admin_profiles profile ON profile.user_id = audit.actor_user_id
                """ + predicate + """
                ORDER BY audit.occurred_at DESC, audit.id DESC
                LIMIT :limit OFFSET :offset
                """, parameters, this::mapAuditEvent);
        long count = total == null ? 0 : total;
        int totalPages = count == 0 ? 0 : (int) Math.ceil((double) count / size);
        return new AdminOperations.AuditPage(items, page, size, count, totalPages);
    }

    @Override
    public List<AdminOperations.AuditEvent> exportAudit(String query, int limit) {
        return namedJdbcTemplate.query("""
                SELECT audit.id, audit.actor_user_id,
                       COALESCE(profile.full_name, 'Quản trị viên') AS actor_name,
                       audit.action, audit.entity_type, audit.entity_id,
                       audit.changed_fields::text AS changed_fields,
                       audit.occurred_at
                FROM admin_audit_events audit
                LEFT JOIN admin_profiles profile ON profile.user_id = audit.actor_user_id
                WHERE CAST(:query AS VARCHAR) IS NULL
                   OR lower(audit.action) LIKE lower('%' || CAST(:query AS VARCHAR) || '%')
                   OR lower(audit.entity_type) LIKE lower('%' || CAST(:query AS VARCHAR) || '%')
                   OR lower(COALESCE(profile.full_name, '')) LIKE lower('%' || CAST(:query AS VARCHAR) || '%')
                ORDER BY audit.occurred_at DESC, audit.id DESC
                LIMIT :limit
                """, new MapSqlParameterSource()
                .addValue("query", query)
                .addValue("limit", limit), this::mapAuditEvent);
    }

    @Override
    public void audit(
            UUID actorUserId,
            String action,
            String entityType,
            UUID entityId,
            String changedFields,
            Instant occurredAt) {
        namedJdbcTemplate.update("""
                INSERT INTO admin_audit_events (
                    id, actor_user_id, action, entity_type, entity_id,
                    changed_fields, occurred_at
                ) VALUES (
                    :id, :actorId, :action, :entityType, :entityId,
                    CAST(:changedFields AS JSONB), :occurredAt
                )
                """, new MapSqlParameterSource()
                .addValue("id", UUID.randomUUID())
                .addValue("actorId", actorUserId)
                .addValue("action", action)
                .addValue("entityType", entityType)
                .addValue("entityId", entityId)
                .addValue("changedFields", changedFields)
                .addValue("occurredAt", Timestamp.from(occurredAt)));
    }

    private MapSqlParameterSource assessmentParameters(
            UUID id,
            UUID enrollmentId,
            String kind,
            String form,
            String label,
            Integer duration,
            String status,
            BigDecimal score,
            String outcome,
            LocalDate assessedOn,
            int displayOrder,
            Instant now) {
        return new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("enrollmentId", enrollmentId)
                .addValue("kind", kind)
                .addValue("form", form)
                .addValue("label", label)
                .addValue("duration", duration)
                .addValue("status", status)
                .addValue("score", score)
                .addValue("outcome", outcome)
                .addValue("assessedOn", assessedOn)
                .addValue("displayOrder", displayOrder)
                .addValue("now", Timestamp.from(now));
    }

    private MapSqlParameterSource announcementParameters(
            UUID id,
            String title,
            String body,
            String audience,
            Integer gradeLevel,
            Instant publishedAt,
            Instant visibleFrom,
            Instant visibleUntil,
            long version,
            Instant now) {
        return new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("title", title)
                .addValue("body", body)
                .addValue("audience", audience)
                .addValue("gradeLevel", gradeLevel)
                .addValue("publishedAt", Timestamp.from(publishedAt))
                .addValue("visibleFrom", Timestamp.from(visibleFrom))
                .addValue("visibleUntil", visibleUntil == null ? null : Timestamp.from(visibleUntil))
                .addValue("version", version)
                .addValue("now", Timestamp.from(now));
    }

    private MapSqlParameterSource eventParameters(
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
            long version,
            Instant now) {
        return new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("category", category)
                .addValue("title", title)
                .addValue("description", description)
                .addValue("location", location)
                .addValue("startsAt", Timestamp.from(startsAt))
                .addValue("endsAt", Timestamp.from(endsAt))
                .addValue("audience", audience)
                .addValue("gradeLevel", gradeLevel)
                .addValue("capacity", capacity)
                .addValue("registrationDeadline", registrationDeadline == null ? null : Timestamp.from(registrationDeadline))
                .addValue("cancellationDeadline", cancellationDeadline == null ? null : Timestamp.from(cancellationDeadline))
                .addValue("registrationEnabled", registrationEnabled)
                .addValue("enabled", enabled)
                .addValue("version", version)
                .addValue("now", Timestamp.from(now));
    }

    private MapSqlParameterSource clubParameters(
            UUID id,
            String category,
            String name,
            String description,
            String advisorName,
            String schedule,
            String location,
            String audience,
            Integer gradeLevel,
            Integer capacity,
            Instant deadline,
            boolean accepting,
            boolean enabled,
            long version,
            Instant now) {
        return new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("category", category)
                .addValue("name", name)
                .addValue("description", description)
                .addValue("advisorName", advisorName)
                .addValue("schedule", schedule)
                .addValue("location", location)
                .addValue("audience", audience)
                .addValue("gradeLevel", gradeLevel)
                .addValue("capacity", capacity)
                .addValue("deadline", deadline == null ? null : Timestamp.from(deadline))
                .addValue("accepting", accepting)
                .addValue("enabled", enabled)
                .addValue("version", version)
                .addValue("now", Timestamp.from(now));
    }

    private MapSqlParameterSource mutationParameters(
            UUID id, String status, long version, Instant now) {
        return new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("status", status)
                .addValue("version", version)
                .addValue("now", Timestamp.from(now));
    }

    private AdminOperations.Period mapPeriod(ResultSet row, int index) throws SQLException {
        return new AdminOperations.Period(
                row.getObject("id", UUID.class),
                row.getObject("academic_term_id", UUID.class),
                row.getString("session"),
                row.getInt("period_number"),
                row.getTime("start_time").toLocalTime(),
                row.getTime("end_time").toLocalTime(),
                row.getLong("version"));
    }

    private AdminOperations.Lesson mapLesson(ResultSet row, int index) throws SQLException {
        return new AdminOperations.Lesson(
                row.getObject("id", UUID.class),
                row.getObject("academic_term_id", UUID.class),
                row.getObject("school_class_id", UUID.class),
                row.getInt("day_of_week"),
                row.getString("session"),
                row.getInt("period_number"),
                row.getObject("subject_id", UUID.class),
                row.getString("subject_name"),
                row.getString("teacher_name"),
                row.getString("room"),
                row.getLong("version"));
    }

    private AdminOperations.LessonOverride mapOverride(ResultSet row, int index) throws SQLException {
        return new AdminOperations.LessonOverride(
                row.getObject("id", UUID.class),
                row.getObject("academic_term_id", UUID.class),
                row.getObject("school_class_id", UUID.class),
                row.getObject("lesson_date", LocalDate.class),
                row.getString("session"),
                row.getInt("period_number"),
                row.getString("override_type"),
                row.getObject("subject_id", UUID.class),
                row.getString("subject_name"),
                row.getString("teacher_name"),
                row.getString("room"),
                row.getString("note"),
                row.getLong("version"));
    }

    private AdminOperations.StudentSubject mapStudentSubject(ResultSet row, int index) throws SQLException {
        return new AdminOperations.StudentSubject(
                row.getObject("id", UUID.class),
                row.getObject("student_id", UUID.class),
                row.getObject("academic_term_id", UUID.class),
                row.getObject("subject_id", UUID.class),
                row.getString("subject_name"),
                row.getString("assessment_mode"),
                (Integer) row.getObject("annual_lesson_count"),
                row.getInt("display_order"),
                row.getLong("version"));
    }

    private AdminOperations.Assessment mapAssessment(ResultSet row, int index) throws SQLException {
        return new AdminOperations.Assessment(
                row.getObject("id", UUID.class),
                row.getObject("student_term_subject_id", UUID.class),
                row.getString("assessment_kind"),
                row.getString("assessment_form"),
                row.getString("display_label"),
                (Integer) row.getObject("duration_minutes"),
                row.getString("status"),
                row.getBigDecimal("score"),
                row.getString("outcome"),
                row.getObject("assessed_on", LocalDate.class),
                row.getInt("display_order"),
                row.getLong("version"));
    }

    private AdminOperations.StudentForm mapStudentForm(ResultSet row, int index) throws SQLException {
        return new AdminOperations.StudentForm(
                row.getObject("id", UUID.class),
                row.getObject("student_id", UUID.class),
                row.getString("student_code"),
                row.getString("student_name"),
                row.getString("form_type"),
                row.getString("reason"),
                row.getObject("starts_on", LocalDate.class),
                row.getObject("ends_on", LocalDate.class),
                row.getString("status"),
                instant(row, "submitted_at"),
                instant(row, "updated_at"),
                row.getLong("version"));
    }

    private AdminOperations.Announcement mapAnnouncement(ResultSet row, int index) throws SQLException {
        return new AdminOperations.Announcement(
                row.getObject("id", UUID.class),
                row.getString("title"),
                row.getString("body"),
                row.getString("audience"),
                (Integer) row.getObject("audience_grade_level"),
                instant(row, "published_at"),
                instant(row, "visible_from"),
                nullableInstant(row, "visible_until"),
                row.getLong("version"));
    }

    private AdminOperations.Event mapEvent(ResultSet row, int index) throws SQLException {
        return new AdminOperations.Event(
                row.getObject("id", UUID.class),
                row.getString("category"),
                row.getString("title"),
                row.getString("description"),
                row.getString("location"),
                instant(row, "starts_at"),
                instant(row, "ends_at"),
                row.getString("audience"),
                (Integer) row.getObject("audience_grade_level"),
                (Integer) row.getObject("capacity"),
                nullableInstant(row, "registration_deadline"),
                nullableInstant(row, "cancellation_deadline"),
                row.getBoolean("registration_enabled"),
                row.getBoolean("enabled"),
                row.getLong("version"),
                row.getInt("registered_count"));
    }

    private AdminOperations.EventRegistration mapEventRegistration(ResultSet row, int index) throws SQLException {
        return new AdminOperations.EventRegistration(
                row.getObject("id", UUID.class),
                row.getObject("event_id", UUID.class),
                row.getObject("student_id", UUID.class),
                row.getString("student_code"),
                row.getString("student_name"),
                row.getString("status"),
                instant(row, "registered_at"),
                row.getLong("version"));
    }

    private AdminOperations.Club mapClub(ResultSet row, int index) throws SQLException {
        return new AdminOperations.Club(
                row.getObject("id", UUID.class),
                row.getString("category"),
                row.getString("name"),
                row.getString("description"),
                row.getString("advisor_name"),
                row.getString("meeting_schedule"),
                row.getString("location"),
                row.getString("audience"),
                (Integer) row.getObject("audience_grade_level"),
                (Integer) row.getObject("capacity"),
                nullableInstant(row, "application_deadline"),
                row.getBoolean("accepting_applications"),
                row.getBoolean("enabled"),
                row.getLong("version"),
                row.getInt("active_count"));
    }

    private AdminOperations.ClubMembership mapClubMembership(ResultSet row, int index) throws SQLException {
        return new AdminOperations.ClubMembership(
                row.getObject("id", UUID.class),
                row.getObject("club_id", UUID.class),
                row.getObject("student_id", UUID.class),
                row.getString("student_code"),
                row.getString("student_name"),
                row.getString("status"),
                instant(row, "applied_at"),
                row.getLong("version"));
    }

    private AdminOperations.AuditEvent mapAuditEvent(ResultSet row, int index) throws SQLException {
        return new AdminOperations.AuditEvent(
                row.getObject("id", UUID.class),
                row.getObject("actor_user_id", UUID.class),
                row.getString("actor_name"),
                row.getString("action"),
                row.getString("entity_type"),
                row.getObject("entity_id", UUID.class),
                row.getString("changed_fields"),
                instant(row, "occurred_at"));
    }

    private static Instant instant(ResultSet row, String column) throws SQLException {
        return row.getTimestamp(column).toInstant();
    }

    private static Instant nullableInstant(ResultSet row, String column) throws SQLException {
        Timestamp value = row.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }
}
