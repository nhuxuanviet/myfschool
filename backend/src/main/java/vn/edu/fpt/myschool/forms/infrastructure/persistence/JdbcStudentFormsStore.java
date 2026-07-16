package vn.edu.fpt.myschool.forms.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import vn.edu.fpt.myschool.forms.application.port.StudentFormsStore;
import vn.edu.fpt.myschool.forms.domain.StudentFormDetails;
import vn.edu.fpt.myschool.forms.domain.StudentFormStatus;
import vn.edu.fpt.myschool.forms.domain.StudentFormStatusEntry;
import vn.edu.fpt.myschool.forms.domain.StudentFormSummary;
import vn.edu.fpt.myschool.forms.domain.StudentFormType;

@Repository
class JdbcStudentFormsStore implements StudentFormsStore {

    private static final String SUMMARY_COLUMNS = """
            form.id,
            form.form_type,
            form.starts_on,
            form.ends_on,
            form.status,
            form.submitted_at,
            form.updated_at
            """;

    private static final String LIST_SQL = """
            SELECT %s
            FROM student_forms form
            WHERE form.student_id = :studentId
              AND (:status IS NULL OR form.status = :status)
            ORDER BY form.submitted_at DESC, form.id DESC
            """.formatted(SUMMARY_COLUMNS);

    private static final String DETAILS_SQL = """
            SELECT %s,
                   form.reason
            FROM student_forms form
            WHERE form.student_id = :studentId
              AND form.id = :formId
            """.formatted(SUMMARY_COLUMNS);

    private static final String LOCK_SQL = """
            SELECT %s
            FROM student_forms form
            WHERE form.student_id = :studentId
              AND form.id = :formId
            FOR UPDATE
            """.formatted(SUMMARY_COLUMNS);

    private static final String TIMELINE_SQL = """
            SELECT id, status, occurred_at, note
            FROM student_form_status_history
            WHERE form_id = :formId
            ORDER BY sequence_number ASC
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    JdbcStudentFormsStore(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<StudentFormSummary> findByStudent(UUID studentId, StudentFormStatus status) {
        return jdbcTemplate.query(
                LIST_SQL,
                new MapSqlParameterSource()
                        .addValue("studentId", studentId)
                        .addValue("status", status == null ? null : status.name(), Types.VARCHAR),
                this::mapSummary);
    }

    @Override
    public Optional<StudentFormDetails> findByStudentAndId(UUID studentId, UUID formId) {
        Optional<FormRow> form = jdbcTemplate.query(
                DETAILS_SQL,
                formParameters(studentId, formId),
                (resultSet, rowNumber) -> new FormRow(
                        mapSummary(resultSet, rowNumber),
                        resultSet.getString("reason")))
                .stream()
                .findFirst();
        return form.map(row -> new StudentFormDetails(
                row.summary(),
                row.reason(),
                findTimeline(formId)));
    }

    @Override
    public Optional<StudentFormSummary> lockByStudentAndId(UUID studentId, UUID formId) {
        return jdbcTemplate.query(
                        LOCK_SQL,
                        formParameters(studentId, formId),
                        this::mapSummary)
                .stream()
                .findFirst();
    }

    @Override
    public void create(
            UUID formId,
            UUID studentId,
            StudentFormType type,
            String reason,
            LocalDate startsOn,
            LocalDate endsOn,
            Instant submittedAt) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("formId", formId)
                .addValue("studentId", studentId)
                .addValue("formType", type.name(), Types.VARCHAR)
                .addValue("reason", reason, Types.VARCHAR)
                .addValue("startsOn", startsOn == null ? null : java.sql.Date.valueOf(startsOn), Types.DATE)
                .addValue("endsOn", endsOn == null ? null : java.sql.Date.valueOf(endsOn), Types.DATE)
                .addValue("submittedAt", utc(submittedAt), Types.TIMESTAMP_WITH_TIMEZONE);
        jdbcTemplate.update("""
                INSERT INTO student_forms (
                    id, student_id, form_type, reason, starts_on, ends_on, status,
                    submitted_at, updated_at
                ) VALUES (
                    :formId, :studentId, :formType, :reason, :startsOn, :endsOn, 'SUBMITTED',
                    :submittedAt, :submittedAt
                )
                """, parameters);
        insertHistory(formId, 1, StudentFormStatus.SUBMITTED, submittedAt, "Đơn đã được gửi");
    }

    @Override
    public void cancel(UUID formId, Instant cancelledAt) {
        int changed = jdbcTemplate.update("""
                UPDATE student_forms
                SET status = 'CANCELLED',
                    updated_at = :cancelledAt
                WHERE id = :formId
                  AND status IN ('SUBMITTED', 'IN_REVIEW')
                """,
                new MapSqlParameterSource()
                        .addValue("formId", formId)
                        .addValue("cancelledAt", utc(cancelledAt), Types.TIMESTAMP_WITH_TIMEZONE));
        if (changed != 1) {
            throw new IllegalStateException("Expected exactly one student form to be cancelled");
        }
        insertHistory(
                formId,
                nextHistorySequence(formId),
                StudentFormStatus.CANCELLED,
                cancelledAt,
                "Học sinh đã hủy đơn");
    }

    private List<StudentFormStatusEntry> findTimeline(UUID formId) {
        return jdbcTemplate.query(
                TIMELINE_SQL,
                new MapSqlParameterSource().addValue("formId", formId),
                (resultSet, rowNumber) -> new StudentFormStatusEntry(
                        resultSet.getObject("id", UUID.class),
                        StudentFormStatus.valueOf(resultSet.getString("status")),
                        instant(resultSet, "occurred_at"),
                        resultSet.getString("note")));
    }

    private void insertHistory(
            UUID formId,
            int sequenceNumber,
            StudentFormStatus status,
            Instant occurredAt,
            String note) {
        jdbcTemplate.update("""
                INSERT INTO student_form_status_history (
                    id, form_id, sequence_number, status, occurred_at, note
                ) VALUES (
                    :id, :formId, :sequenceNumber, :status, :occurredAt, :note
                )
                """,
                new MapSqlParameterSource()
                        .addValue("id", UUID.randomUUID())
                        .addValue("formId", formId)
                        .addValue("sequenceNumber", sequenceNumber, Types.INTEGER)
                        .addValue("status", status.name(), Types.VARCHAR)
                        .addValue("occurredAt", utc(occurredAt), Types.TIMESTAMP_WITH_TIMEZONE)
                        .addValue("note", note, Types.VARCHAR));
    }

    private int nextHistorySequence(UUID formId) {
        Integer sequence = jdbcTemplate.queryForObject("""
                SELECT COALESCE(MAX(sequence_number), 0) + 1
                FROM student_form_status_history
                WHERE form_id = :formId
                """,
                new MapSqlParameterSource().addValue("formId", formId),
                Integer.class);
        if (sequence == null) {
            throw new IllegalStateException("Could not allocate student form history sequence");
        }
        return sequence;
    }

    private StudentFormSummary mapSummary(ResultSet resultSet, int rowNumber) throws SQLException {
        return new StudentFormSummary(
                resultSet.getObject("id", UUID.class),
                StudentFormType.valueOf(resultSet.getString("form_type")),
                resultSet.getObject("starts_on", LocalDate.class),
                resultSet.getObject("ends_on", LocalDate.class),
                StudentFormStatus.valueOf(resultSet.getString("status")),
                instant(resultSet, "submitted_at"),
                instant(resultSet, "updated_at"));
    }

    private static MapSqlParameterSource formParameters(UUID studentId, UUID formId) {
        return new MapSqlParameterSource()
                .addValue("studentId", studentId)
                .addValue("formId", formId);
    }

    private static Instant instant(ResultSet resultSet, String column) throws SQLException {
        return resultSet.getObject(column, OffsetDateTime.class).toInstant();
    }

    private static OffsetDateTime utc(Instant instant) {
        return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private record FormRow(StudentFormSummary summary, String reason) {
    }
}
