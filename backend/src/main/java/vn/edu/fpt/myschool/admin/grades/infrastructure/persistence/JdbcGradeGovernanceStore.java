package vn.edu.fpt.myschool.admin.grades.infrastructure.persistence;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import vn.edu.fpt.myschool.admin.grades.application.port.GradeGovernanceStore;
import vn.edu.fpt.myschool.admin.grades.domain.GradeGovernance;

@Repository
class JdbcGradeGovernanceStore implements GradeGovernanceStore {

    private static final String REQUEST_SELECT = """
            SELECT request.id, request.grade_assessment_id, student.full_name AS student_full_name,
                   student.student_code, school_class.code AS class_code,
                   subject.name AS subject_name, assessment.display_label,
                   request.old_score, request.old_outcome, request.new_score, request.new_outcome,
                   request.reason, request.status, requester.phone_number AS requested_by_name,
                   request.created_at, request.decided_at, request.decision_note
            FROM grade_change_requests request
            INNER JOIN grade_assessments assessment ON assessment.id = request.grade_assessment_id
            INNER JOIN student_term_subjects enrollment
                ON enrollment.id = assessment.student_term_subject_id
            INNER JOIN students student ON student.id = enrollment.student_id
            INNER JOIN subjects subject ON subject.id = enrollment.subject_id
            LEFT JOIN school_classes school_class ON school_class.id = student.class_id
            INNER JOIN users requester ON requester.id = request.requested_by
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    JdbcGradeGovernanceStore(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<GradeGovernance.BookSummary> findBooks(UUID academicTermId, Boolean locked) {
        return jdbcTemplate.query(
                """
                SELECT book.id, school_class.code AS class_code, subject.name AS subject_name,
                       term.code AS academic_term_code, book.published_at, book.locked_at,
                       teacher.full_name AS teacher_full_name, book.version,
                       (SELECT count(*) FROM grade_columns c WHERE c.grade_book_id = book.id)
                           AS column_count,
                       (SELECT count(*) FROM grade_assessments a
                        INNER JOIN grade_columns c2 ON c2.id = a.grade_column_id
                        WHERE c2.grade_book_id = book.id AND a.status = 'PENDING')
                           AS pending_mark_count
                FROM grade_books book
                INNER JOIN school_classes school_class ON school_class.id = book.class_id
                INNER JOIN subjects subject ON subject.id = book.subject_id
                INNER JOIN academic_terms term ON term.id = book.academic_term_id
                -- The responsible teacher comes from the assignment, which is the only record
                -- that says who owns this book. A book with no assignment shows no teacher
                -- rather than disappearing: that gap is what an administrator needs to see.
                LEFT JOIN teacher_subject_assignments assignment
                    ON assignment.class_id = book.class_id
                    AND assignment.subject_id = book.subject_id
                    AND assignment.academic_term_id = book.academic_term_id
                LEFT JOIN teacher_profiles teacher ON teacher.id = assignment.teacher_id
                WHERE (CAST(:academicTermId AS UUID) IS NULL
                       OR book.academic_term_id = :academicTermId)
                  AND (CAST(:locked AS BOOLEAN) IS NULL
                       OR (book.locked_at IS NOT NULL) = CAST(:locked AS BOOLEAN))
                ORDER BY term.starts_on DESC, school_class.code, subject.name
                """,
                new MapSqlParameterSource("academicTermId", academicTermId)
                        .addValue("locked", locked),
                (rs, rowNum) -> new GradeGovernance.BookSummary(
                        rs.getObject("id", UUID.class),
                        rs.getString("class_code"),
                        rs.getString("subject_name"),
                        rs.getString("academic_term_code"),
                        instant(rs, "published_at"),
                        instant(rs, "locked_at"),
                        rs.getString("teacher_full_name"),
                        rs.getInt("column_count"),
                        rs.getInt("pending_mark_count"),
                        rs.getLong("version")));
    }

    @Override
    public boolean lock(UUID bookId, UUID lockedBy, long expectedVersion, Instant now) {
        return jdbcTemplate.update(
                """
                UPDATE grade_books
                SET locked_at = :now, locked_by = :lockedBy, version = version + 1, updated_at = :now
                WHERE id = :bookId AND version = :expectedVersion AND locked_at IS NULL
                """,
                new MapSqlParameterSource("bookId", bookId)
                        .addValue("lockedBy", lockedBy)
                        .addValue("expectedVersion", expectedVersion)
                        .addValue("now", Timestamp.from(now))) == 1;
    }

    @Override
    public boolean unlock(UUID bookId, long expectedVersion, Instant now) {
        return jdbcTemplate.update(
                """
                UPDATE grade_books
                SET locked_at = NULL, locked_by = NULL, version = version + 1, updated_at = :now
                WHERE id = :bookId AND version = :expectedVersion AND locked_at IS NOT NULL
                """,
                new MapSqlParameterSource("bookId", bookId)
                        .addValue("expectedVersion", expectedVersion)
                        .addValue("now", Timestamp.from(now))) == 1;
    }

    @Override
    public List<GradeGovernance.ChangeRequest> findChangeRequests(String status) {
        return jdbcTemplate.query(
                REQUEST_SELECT
                        + " WHERE (CAST(:status AS VARCHAR) IS NULL OR request.status = :status)"
                        + " ORDER BY request.created_at DESC",
                new MapSqlParameterSource("status", status),
                JdbcGradeGovernanceStore::mapRequest);
    }

    @Override
    public Optional<GradeGovernance.ChangeRequest> findChangeRequest(UUID requestId) {
        return jdbcTemplate.query(
                        REQUEST_SELECT + " WHERE request.id = :requestId",
                        new MapSqlParameterSource("requestId", requestId),
                        JdbcGradeGovernanceStore::mapRequest)
                .stream()
                .findFirst();
    }

    /** The mark's current value is copied into the request so the decision stays readable later. */
    @Override
    public UUID createChangeRequest(
            UUID assessmentId,
            UUID requestedBy,
            BigDecimal newScore,
            String newOutcome,
            String reason,
            Instant now) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO grade_change_requests (
                    id, grade_assessment_id, requested_by, old_score, old_outcome,
                    new_score, new_outcome, reason, status, created_at, updated_at
                )
                SELECT :id, assessment.id, :requestedBy, assessment.score, assessment.outcome,
                       :newScore, :newOutcome, :reason, 'PENDING', :now, :now
                FROM grade_assessments assessment
                WHERE assessment.id = :assessmentId
                """,
                new MapSqlParameterSource("id", id)
                        .addValue("assessmentId", assessmentId)
                        .addValue("requestedBy", requestedBy)
                        .addValue("newScore", newScore)
                        .addValue("newOutcome", newOutcome)
                        .addValue("reason", reason)
                        .addValue("now", Timestamp.from(now)));
        return id;
    }

    @Override
    public boolean decide(
            UUID requestId, String status, UUID decidedBy, String decisionNote, Instant now) {
        return jdbcTemplate.update(
                """
                UPDATE grade_change_requests
                SET status = :status, decided_by = :decidedBy, decided_at = :now,
                    decision_note = :note, updated_at = :now
                WHERE id = :requestId AND status = 'PENDING'
                """,
                new MapSqlParameterSource("requestId", requestId)
                        .addValue("status", status)
                        .addValue("decidedBy", decidedBy)
                        .addValue("note", decisionNote)
                        .addValue("now", Timestamp.from(now))) == 1;
    }

    @Override
    public boolean applyChange(UUID assessmentId, BigDecimal score, String outcome, Instant now) {
        return jdbcTemplate.update(
                """
                UPDATE grade_assessments
                SET score = :score, outcome = :outcome, status = 'RECORDED', updated_at = :now
                WHERE id = :assessmentId
                """,
                new MapSqlParameterSource("assessmentId", assessmentId)
                        .addValue("score", score)
                        .addValue("outcome", outcome)
                        .addValue("now", Timestamp.from(now))) == 1;
    }

    @Override
    public void recordAudit(
            UUID actorUserId,
            String action,
            String entityType,
            UUID entityId,
            String changedFieldsJson,
            Instant occurredAt) {
        jdbcTemplate.update(
                """
                INSERT INTO admin_audit_events (
                    id, actor_user_id, action, entity_type, entity_id, changed_fields, occurred_at
                ) VALUES (:id, :actorUserId, :action, :entityType, :entityId,
                          CAST(:changedFields AS JSONB), :occurredAt)
                """,
                new MapSqlParameterSource("id", UUID.randomUUID())
                        .addValue("actorUserId", actorUserId)
                        .addValue("action", action)
                        .addValue("entityType", entityType)
                        .addValue("entityId", entityId)
                        .addValue("changedFields", changedFieldsJson)
                        .addValue("occurredAt", Timestamp.from(occurredAt)));
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private static GradeGovernance.ChangeRequest mapRequest(ResultSet rs, int rowNum)
            throws SQLException {
        return new GradeGovernance.ChangeRequest(
                rs.getObject("id", UUID.class),
                rs.getObject("grade_assessment_id", UUID.class),
                rs.getString("student_full_name"),
                rs.getString("student_code"),
                rs.getString("class_code"),
                rs.getString("subject_name"),
                rs.getString("display_label"),
                rs.getBigDecimal("old_score"),
                rs.getString("old_outcome"),
                rs.getBigDecimal("new_score"),
                rs.getString("new_outcome"),
                rs.getString("reason"),
                rs.getString("status"),
                rs.getString("requested_by_name"),
                instant(rs, "created_at"),
                instant(rs, "decided_at"),
                rs.getString("decision_note"));
    }
}
