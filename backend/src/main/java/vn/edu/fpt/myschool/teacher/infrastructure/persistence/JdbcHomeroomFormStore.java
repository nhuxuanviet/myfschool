package vn.edu.fpt.myschool.teacher.infrastructure.persistence;

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

import vn.edu.fpt.myschool.teacher.application.port.HomeroomFormStore;
import vn.edu.fpt.myschool.teacher.domain.HomeroomForm;

@Repository
class JdbcHomeroomFormStore implements HomeroomFormStore {

    // LEAVE_OF_ABSENCE only, in SQL rather than in a caller's filter: an administrative form
    // must be unreachable here even if someone passes its id (spec §7.2).
    private static final String FORM_SELECT = """
            SELECT form.id, form.student_id, student.student_code,
                   student.full_name AS student_full_name,
                   school_class.code AS class_code, form.reason, form.starts_on, form.ends_on,
                   form.status, form.submitted_by_role, form.submitted_at
            FROM student_forms form
            INNER JOIN students student ON student.id = form.student_id
            LEFT JOIN school_classes school_class ON school_class.id = student.class_id
            WHERE form.form_type = 'LEAVE_OF_ABSENCE'
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    JdbcHomeroomFormStore(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<HomeroomForm> findLeaveFormsForHomeroom(UUID teacherId, String status) {
        return jdbcTemplate.query(
                FORM_SELECT + """
                  AND EXISTS (
                      SELECT 1 FROM homeroom_assignments homeroom
                      WHERE homeroom.teacher_id = :teacherId
                        AND homeroom.class_id = student.class_id
                  )
                  AND (CAST(:status AS VARCHAR) IS NULL OR form.status = :status)
                ORDER BY form.submitted_at DESC, form.id DESC
                """,
                new MapSqlParameterSource("teacherId", teacherId).addValue("status", status),
                JdbcHomeroomFormStore::mapForm);
    }

    @Override
    public Optional<HomeroomForm> findLeaveForm(UUID formId) {
        return jdbcTemplate.query(
                        FORM_SELECT + " AND form.id = :formId",
                        new MapSqlParameterSource("formId", formId),
                        JdbcHomeroomFormStore::mapForm)
                .stream()
                .findFirst();
    }

    @Override
    public boolean isHomeroomTeacherOfFormStudent(UUID teacherId, UUID formId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT count(*)
                FROM student_forms form
                INNER JOIN students student ON student.id = form.student_id
                INNER JOIN homeroom_assignments homeroom ON homeroom.class_id = student.class_id
                WHERE form.id = :formId AND homeroom.teacher_id = :teacherId
                """,
                new MapSqlParameterSource("formId", formId).addValue("teacherId", teacherId),
                Integer.class);
        return count != null && count > 0;
    }

    /** Only an open form moves: a decided form is final and re-deciding it would erase a decision. */
    @Override
    public boolean updateStatus(UUID formId, String status, Instant now) {
        return jdbcTemplate.update(
                """
                UPDATE student_forms
                SET status = :status, updated_at = :now
                WHERE id = :formId AND status IN ('SUBMITTED', 'IN_REVIEW')
                """,
                new MapSqlParameterSource("formId", formId)
                        .addValue("status", status)
                        .addValue("now", Timestamp.from(now))) == 1;
    }

    @Override
    public void appendHistory(
            UUID formId, String status, UUID actorUserId, String note, Instant occurredAt) {
        jdbcTemplate.update(
                """
                INSERT INTO student_form_status_history (
                    id, form_id, sequence_number, status, actor_user_id, actor_role,
                    occurred_at, note
                )
                SELECT :id, :formId,
                       COALESCE(max(history.sequence_number), 0) + 1,
                       :status, :actorUserId, 'TEACHER', :occurredAt, :note
                FROM student_form_status_history history
                WHERE history.form_id = :formId
                """,
                new MapSqlParameterSource("id", UUID.randomUUID())
                        .addValue("formId", formId)
                        .addValue("status", status)
                        .addValue("actorUserId", actorUserId)
                        .addValue("occurredAt", Timestamp.from(occurredAt))
                        .addValue("note", note));
    }

    private static HomeroomForm mapForm(ResultSet rs, int rowNum) throws SQLException {
        java.sql.Date startsOn = rs.getDate("starts_on");
        java.sql.Date endsOn = rs.getDate("ends_on");
        Timestamp submittedAt = rs.getTimestamp("submitted_at");
        return new HomeroomForm(
                rs.getObject("id", UUID.class),
                rs.getObject("student_id", UUID.class),
                rs.getString("student_code"),
                rs.getString("student_full_name"),
                rs.getString("class_code"),
                rs.getString("reason"),
                startsOn == null ? null : startsOn.toLocalDate(),
                endsOn == null ? null : endsOn.toLocalDate(),
                rs.getString("status"),
                rs.getString("submitted_by_role"),
                submittedAt == null ? null : submittedAt.toInstant());
    }
}
