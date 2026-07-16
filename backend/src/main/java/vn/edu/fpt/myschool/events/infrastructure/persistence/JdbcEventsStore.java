package vn.edu.fpt.myschool.events.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import vn.edu.fpt.myschool.events.application.port.EventsStore;
import vn.edu.fpt.myschool.events.domain.EventAudience;
import vn.edu.fpt.myschool.events.domain.EventCategory;
import vn.edu.fpt.myschool.events.domain.EventProjection;
import vn.edu.fpt.myschool.events.domain.EventRegistration;
import vn.edu.fpt.myschool.events.domain.EventRegistrationStatus;
import vn.edu.fpt.myschool.events.domain.SchoolEvent;

@Repository
class JdbcEventsStore implements EventsStore {

    private static final String VISIBLE_EVENTS_SQL = """
            WITH registration_counts AS (
                SELECT event_id, COUNT(*) AS registered_count
                FROM student_event_registrations
                WHERE status = 'REGISTERED'
                GROUP BY event_id
            )
            SELECT event.id,
                   event.category,
                   event.title,
                   event.description,
                   event.location,
                   event.starts_at,
                   event.ends_at,
                   event.audience,
                   event.audience_grade_level,
                   event.capacity,
                   event.registration_deadline,
                   event.cancellation_deadline,
                   event.registration_enabled,
                   COALESCE(registration_counts.registered_count, 0) AS registered_count,
                   COALESCE(student_registration.status, 'NOT_REGISTERED') AS registration_status
            FROM school_events event
            LEFT JOIN registration_counts ON registration_counts.event_id = event.id
            LEFT JOIN student_event_registrations student_registration
                ON student_registration.event_id = event.id
                AND student_registration.student_id = :studentId
            WHERE event.enabled = TRUE
              AND (event.audience = 'ALL'
                    OR (event.audience = 'GRADE'
                        AND event.audience_grade_level = :gradeLevel))
              AND (:category IS NULL OR event.category = :category)
              AND (:includePast = TRUE OR event.ends_at >= :viewedAt)
            ORDER BY CASE WHEN event.ends_at < :viewedAt THEN 1 ELSE 0 END ASC,
                     CASE WHEN event.ends_at < :viewedAt THEN event.starts_at END DESC,
                     CASE WHEN event.ends_at >= :viewedAt THEN event.starts_at END ASC,
                     event.id ASC
            """;

    private static final String VISIBLE_EVENT_SQL = """
            WITH registration_counts AS (
                SELECT event_id, COUNT(*) AS registered_count
                FROM student_event_registrations
                WHERE status = 'REGISTERED'
                GROUP BY event_id
            )
            SELECT event.id,
                   event.category,
                   event.title,
                   event.description,
                   event.location,
                   event.starts_at,
                   event.ends_at,
                   event.audience,
                   event.audience_grade_level,
                   event.capacity,
                   event.registration_deadline,
                   event.cancellation_deadline,
                   event.registration_enabled,
                   COALESCE(registration_counts.registered_count, 0) AS registered_count,
                   COALESCE(student_registration.status, 'NOT_REGISTERED') AS registration_status
            FROM school_events event
            LEFT JOIN registration_counts ON registration_counts.event_id = event.id
            LEFT JOIN student_event_registrations student_registration
                ON student_registration.event_id = event.id
                AND student_registration.student_id = :studentId
            WHERE event.id = :eventId
              AND event.enabled = TRUE
              AND (event.audience = 'ALL'
                    OR (event.audience = 'GRADE'
                        AND event.audience_grade_level = :gradeLevel))
            """;

    private static final String LOCK_VISIBLE_EVENT_SQL = """
            SELECT event.id,
                   event.category,
                   event.title,
                   event.description,
                   event.location,
                   event.starts_at,
                   event.ends_at,
                   event.audience,
                   event.audience_grade_level,
                   event.capacity,
                   event.registration_deadline,
                   event.cancellation_deadline,
                   event.registration_enabled
            FROM school_events event
            WHERE event.id = :eventId
              AND event.enabled = TRUE
              AND (event.audience = 'ALL'
                    OR (event.audience = 'GRADE'
                        AND event.audience_grade_level = :gradeLevel))
            FOR UPDATE
            """;

    private static final String REGISTRATION_SQL = """
            SELECT id, status
            FROM student_event_registrations
            WHERE event_id = :eventId
              AND student_id = :studentId
            """;

    private static final String REGISTERED_COUNT_SQL = """
            SELECT COUNT(*)
            FROM student_event_registrations
            WHERE event_id = :eventId
              AND status = 'REGISTERED'
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    JdbcEventsStore(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<EventProjection> findVisibleEvents(
            UUID studentId,
            int gradeLevel,
            EventCategory category,
            boolean includePast,
            Instant viewedAt) {
        return jdbcTemplate.query(
                VISIBLE_EVENTS_SQL,
                eventParameters(studentId, gradeLevel)
                        .addValue(
                                "category",
                                category == null ? null : category.name(),
                                Types.VARCHAR)
                        .addValue("includePast", includePast, Types.BOOLEAN)
                        .addValue("viewedAt", utc(viewedAt), Types.TIMESTAMP_WITH_TIMEZONE),
                this::mapProjection);
    }

    @Override
    public Optional<EventProjection> findVisibleEvent(UUID eventId, UUID studentId, int gradeLevel) {
        return jdbcTemplate.query(
                        VISIBLE_EVENT_SQL,
                        eventParameters(studentId, gradeLevel).addValue("eventId", eventId),
                        this::mapProjection)
                .stream()
                .findFirst();
    }

    @Override
    public Optional<SchoolEvent> lockVisibleEvent(UUID eventId, int gradeLevel) {
        return jdbcTemplate.query(
                        LOCK_VISIBLE_EVENT_SQL,
                        new MapSqlParameterSource()
                                .addValue("eventId", eventId)
                                .addValue("gradeLevel", gradeLevel, Types.SMALLINT),
                        this::mapEvent)
                .stream()
                .findFirst();
    }

    @Override
    public Optional<EventRegistration> findRegistration(UUID eventId, UUID studentId) {
        return jdbcTemplate.query(
                        REGISTRATION_SQL,
                        new MapSqlParameterSource()
                                .addValue("eventId", eventId)
                                .addValue("studentId", studentId),
                        this::mapRegistration)
                .stream()
                .findFirst();
    }

    @Override
    public int countRegistered(UUID eventId) {
        Integer count = jdbcTemplate.queryForObject(
                REGISTERED_COUNT_SQL,
                new MapSqlParameterSource().addValue("eventId", eventId),
                Integer.class);
        return count == null ? 0 : count;
    }

    @Override
    public void createRegistration(
            UUID registrationId,
            UUID eventId,
            UUID studentId,
            Instant registeredAt) {
        jdbcTemplate.update("""
                INSERT INTO student_event_registrations (
                    id, event_id, student_id, status, registered_at, cancelled_at, created_at, updated_at
                ) VALUES (
                    :registrationId, :eventId, :studentId, 'REGISTERED', :registeredAt, NULL,
                    :registeredAt, :registeredAt
                )
                """,
                new MapSqlParameterSource()
                        .addValue("registrationId", registrationId)
                        .addValue("eventId", eventId)
                        .addValue("studentId", studentId)
                        .addValue("registeredAt", utc(registeredAt), Types.TIMESTAMP_WITH_TIMEZONE));
    }

    @Override
    public void reactivateRegistration(UUID eventId, UUID studentId, Instant registeredAt) {
        int changed = jdbcTemplate.update("""
                UPDATE student_event_registrations
                SET status = 'REGISTERED',
                    registered_at = :registeredAt,
                    cancelled_at = NULL,
                    updated_at = :registeredAt
                WHERE event_id = :eventId
                  AND student_id = :studentId
                  AND status = 'CANCELLED'
                """,
                new MapSqlParameterSource()
                        .addValue("eventId", eventId)
                        .addValue("studentId", studentId)
                        .addValue("registeredAt", utc(registeredAt), Types.TIMESTAMP_WITH_TIMEZONE));
        requireSingleRegistrationChange(changed);
    }

    @Override
    public void cancelRegistration(UUID eventId, UUID studentId, Instant cancelledAt) {
        int changed = jdbcTemplate.update("""
                UPDATE student_event_registrations
                SET status = 'CANCELLED',
                    cancelled_at = :cancelledAt,
                    updated_at = :cancelledAt
                WHERE event_id = :eventId
                  AND student_id = :studentId
                  AND status = 'REGISTERED'
                """,
                new MapSqlParameterSource()
                        .addValue("eventId", eventId)
                        .addValue("studentId", studentId)
                        .addValue("cancelledAt", utc(cancelledAt), Types.TIMESTAMP_WITH_TIMEZONE));
        requireSingleRegistrationChange(changed);
    }

    private MapSqlParameterSource eventParameters(UUID studentId, int gradeLevel) {
        return new MapSqlParameterSource()
                .addValue("studentId", studentId)
                .addValue("gradeLevel", gradeLevel, Types.SMALLINT);
    }

    private EventProjection mapProjection(ResultSet resultSet, int rowNumber) throws SQLException {
        return new EventProjection(
                mapEvent(resultSet, rowNumber),
                resultSet.getInt("registered_count"),
                EventRegistrationStatus.valueOf(resultSet.getString("registration_status")));
    }

    private SchoolEvent mapEvent(ResultSet resultSet, int rowNumber) throws SQLException {
        return new SchoolEvent(
                resultSet.getObject("id", UUID.class),
                EventCategory.valueOf(resultSet.getString("category")),
                resultSet.getString("title"),
                resultSet.getString("description"),
                resultSet.getString("location"),
                instant(resultSet, "starts_at"),
                instant(resultSet, "ends_at"),
                EventAudience.valueOf(resultSet.getString("audience")),
                resultSet.getObject("audience_grade_level", Integer.class),
                resultSet.getObject("capacity", Integer.class),
                nullableInstant(resultSet, "registration_deadline"),
                nullableInstant(resultSet, "cancellation_deadline"),
                resultSet.getBoolean("registration_enabled"));
    }

    private EventRegistration mapRegistration(ResultSet resultSet, int rowNumber) throws SQLException {
        return new EventRegistration(
                resultSet.getObject("id", UUID.class),
                EventRegistrationStatus.valueOf(resultSet.getString("status")));
    }

    private static Instant instant(ResultSet resultSet, String column) throws SQLException {
        return resultSet.getObject(column, OffsetDateTime.class).toInstant();
    }

    private static Instant nullableInstant(ResultSet resultSet, String column) throws SQLException {
        OffsetDateTime timestamp = resultSet.getObject(column, OffsetDateTime.class);
        return timestamp == null ? null : timestamp.toInstant();
    }

    private static OffsetDateTime utc(Instant instant) {
        return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private static void requireSingleRegistrationChange(int changed) {
        if (changed != 1) {
            throw new IllegalStateException("Expected exactly one event registration to change");
        }
    }
}
