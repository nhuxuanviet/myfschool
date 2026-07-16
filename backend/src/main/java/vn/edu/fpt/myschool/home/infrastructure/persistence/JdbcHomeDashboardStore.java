package vn.edu.fpt.myschool.home.infrastructure.persistence;

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

import vn.edu.fpt.myschool.home.application.port.HomeDashboardStore;
import vn.edu.fpt.myschool.home.domain.AcademicTerm;
import vn.edu.fpt.myschool.home.domain.HomeAnnouncement;

@Repository
class JdbcHomeDashboardStore implements HomeDashboardStore {

    private static final int MAX_VISIBLE_ANNOUNCEMENTS = 10;

    private static final String ACTIVE_TERM_SQL = """
            SELECT academic_years.code AS academic_year,
                   academic_terms.code,
                   academic_terms.name,
                   academic_terms.starts_on,
                   academic_terms.ends_on
            FROM academic_terms
            INNER JOIN academic_years ON academic_years.id = academic_terms.academic_year_id
            WHERE academic_terms.starts_on <= :currentDate
              AND academic_terms.ends_on >= :currentDate
            ORDER BY academic_terms.starts_on DESC,
                     academic_terms.ends_on ASC,
                     academic_terms.code ASC,
                     academic_terms.id ASC
            LIMIT 1
            """;

    private static final String VISIBLE_ANNOUNCEMENTS_SQL = """
            SELECT id, title, body, published_at
            FROM announcements
            WHERE published_at <= :viewedAt
              AND visible_from <= :viewedAt
              AND (visible_until IS NULL OR visible_until > :viewedAt)
              AND (
                    audience = 'ALL'
                    OR (audience = 'GRADE' AND audience_grade_level = :gradeLevel)
              )
            ORDER BY published_at DESC, id DESC
            LIMIT :limit
            """;

    private static final String UPCOMING_VISIBLE_EVENTS_SQL = """
            SELECT COUNT(*)
            FROM school_events
            WHERE enabled = TRUE
              AND starts_at > :viewedAt
              AND (
                    audience = 'ALL'
                    OR (audience = 'GRADE' AND audience_grade_level = :gradeLevel)
              )
            """;

    private static final String PENDING_STUDENT_FORMS_SQL = """
            SELECT COUNT(*)
            FROM student_forms
            WHERE student_id = :studentId
              AND status IN ('SUBMITTED', 'IN_REVIEW')
            """;

    private static final String ACTIVE_CLUB_MEMBERSHIPS_SQL = """
            SELECT COUNT(*)
            FROM student_club_memberships
            WHERE student_id = :studentId AND status = 'ACTIVE'
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    JdbcHomeDashboardStore(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<AcademicTerm> findActiveAcademicTerm(LocalDate date) {
        return jdbcTemplate.query(
                        ACTIVE_TERM_SQL,
                        new MapSqlParameterSource()
                                .addValue("currentDate", java.sql.Date.valueOf(date), Types.DATE),
                        this::mapAcademicTerm)
                .stream()
                .findFirst();
    }

    @Override
    public List<HomeAnnouncement> findVisibleAnnouncements(int gradeLevel, Instant viewedAt) {
        return jdbcTemplate.query(
                VISIBLE_ANNOUNCEMENTS_SQL,
                new MapSqlParameterSource()
                        .addValue("gradeLevel", gradeLevel, Types.SMALLINT)
                        .addValue(
                                "viewedAt",
                                OffsetDateTime.ofInstant(viewedAt, ZoneOffset.UTC),
                                Types.TIMESTAMP_WITH_TIMEZONE)
                        .addValue("limit", MAX_VISIBLE_ANNOUNCEMENTS, Types.INTEGER),
                this::mapAnnouncement);
    }

    @Override
    public int countUpcomingVisibleEvents(int gradeLevel, Instant viewedAt) {
        Integer count = jdbcTemplate.queryForObject(
                UPCOMING_VISIBLE_EVENTS_SQL,
                new MapSqlParameterSource()
                        .addValue("gradeLevel", gradeLevel, Types.SMALLINT)
                        .addValue(
                                "viewedAt",
                                OffsetDateTime.ofInstant(viewedAt, ZoneOffset.UTC),
                                Types.TIMESTAMP_WITH_TIMEZONE),
                Integer.class);
        return count == null ? 0 : count;
    }

    @Override
    public int countPendingStudentForms(UUID studentId) {
        Integer count = jdbcTemplate.queryForObject(
                PENDING_STUDENT_FORMS_SQL,
                new MapSqlParameterSource().addValue("studentId", studentId),
                Integer.class);
        return count == null ? 0 : count;
    }

    @Override
    public int countActiveClubMemberships(UUID studentId) {
        Integer count = jdbcTemplate.queryForObject(
                ACTIVE_CLUB_MEMBERSHIPS_SQL,
                new MapSqlParameterSource().addValue("studentId", studentId),
                Integer.class);
        return count == null ? 0 : count;
    }

    private AcademicTerm mapAcademicTerm(ResultSet resultSet, int rowNumber) throws SQLException {
        return new AcademicTerm(
                resultSet.getString("academic_year"),
                resultSet.getString("code"),
                resultSet.getString("name"),
                resultSet.getObject("starts_on", LocalDate.class),
                resultSet.getObject("ends_on", LocalDate.class));
    }

    private HomeAnnouncement mapAnnouncement(ResultSet resultSet, int rowNumber) throws SQLException {
        return new HomeAnnouncement(
                resultSet.getObject("id", java.util.UUID.class),
                resultSet.getString("title"),
                resultSet.getString("body"),
                resultSet.getTimestamp("published_at").toInstant());
    }
}
