package vn.edu.fpt.myschool.clubs.infrastructure.persistence;

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

import vn.edu.fpt.myschool.clubs.application.port.ClubsStore;
import vn.edu.fpt.myschool.clubs.domain.ClubAudience;
import vn.edu.fpt.myschool.clubs.domain.ClubCategory;
import vn.edu.fpt.myschool.clubs.domain.ClubMembership;
import vn.edu.fpt.myschool.clubs.domain.ClubMembershipStatus;
import vn.edu.fpt.myschool.clubs.domain.ClubProjection;
import vn.edu.fpt.myschool.clubs.domain.SchoolClub;

@Repository
class JdbcClubsStore implements ClubsStore {
    private static final String CLUB_COLUMNS = """
            club.id, club.category, club.name, club.description, club.advisor_name,
            club.meeting_schedule, club.location, club.audience, club.audience_grade_level,
            club.capacity, club.application_deadline, club.accepting_applications
            """;
    private static final String PROJECTION_COLUMNS = CLUB_COLUMNS + """
            , COALESCE(counts.active_count, 0) AS active_count,
            COALESCE(membership.status, 'NOT_APPLIED') AS membership_status
            """;
    private static final String PROJECTION_JOINS = """
            LEFT JOIN (
                SELECT club_id, COUNT(*) AS active_count
                FROM student_club_memberships
                WHERE status = 'ACTIVE'
                GROUP BY club_id
            ) counts ON counts.club_id = club.id
            LEFT JOIN student_club_memberships membership
                ON membership.club_id = club.id AND membership.student_id = :studentId
            """;
    private static final String VISIBILITY = """
            club.enabled = TRUE
            AND (club.audience = 'ALL'
                OR (club.audience = 'GRADE' AND club.audience_grade_level = :gradeLevel))
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    JdbcClubsStore(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<ClubProjection> findVisible(
            UUID studentId, int gradeLevel, ClubCategory category) {
        String sql = """
                SELECT %s
                FROM school_clubs club
                %s
                WHERE %s
                  AND (:category IS NULL OR club.category = :category)
                ORDER BY club.category ASC, club.name ASC, club.id ASC
                """.formatted(PROJECTION_COLUMNS, PROJECTION_JOINS, VISIBILITY);
        return jdbcTemplate.query(sql,
                projectionParameters(studentId, gradeLevel)
                        .addValue("category", category == null ? null : category.name(), Types.VARCHAR),
                this::mapProjection);
    }

    @Override
    public Optional<ClubProjection> findVisibleById(
            UUID clubId, UUID studentId, int gradeLevel) {
        String sql = """
                SELECT %s
                FROM school_clubs club
                %s
                WHERE club.id = :clubId AND %s
                """.formatted(PROJECTION_COLUMNS, PROJECTION_JOINS, VISIBILITY);
        return jdbcTemplate.query(sql,
                        projectionParameters(studentId, gradeLevel).addValue("clubId", clubId),
                        this::mapProjection)
                .stream().findFirst();
    }

    @Override
    public Optional<SchoolClub> lockVisibleById(UUID clubId, int gradeLevel) {
        String sql = """
                SELECT %s
                FROM school_clubs club
                WHERE club.id = :clubId AND %s
                FOR UPDATE
                """.formatted(CLUB_COLUMNS, VISIBILITY);
        return jdbcTemplate.query(sql,
                        new MapSqlParameterSource()
                                .addValue("clubId", clubId)
                                .addValue("gradeLevel", gradeLevel, Types.SMALLINT),
                        this::mapClub)
                .stream().findFirst();
    }

    @Override
    public Optional<ClubMembership> findMembership(UUID clubId, UUID studentId) {
        return jdbcTemplate.query("""
                SELECT id, status
                FROM student_club_memberships
                WHERE club_id = :clubId AND student_id = :studentId
                """,
                        new MapSqlParameterSource()
                                .addValue("clubId", clubId)
                                .addValue("studentId", studentId),
                        (rs, row) -> new ClubMembership(
                                rs.getObject("id", UUID.class),
                                ClubMembershipStatus.valueOf(rs.getString("status"))))
                .stream().findFirst();
    }

    @Override
    public int countActiveMembers(UUID clubId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM student_club_memberships
                WHERE club_id = :clubId AND status = 'ACTIVE'
                """, new MapSqlParameterSource().addValue("clubId", clubId), Integer.class);
        return count == null ? 0 : count;
    }

    @Override
    public void createApplication(UUID id, UUID clubId, UUID studentId, Instant appliedAt) {
        jdbcTemplate.update("""
                INSERT INTO student_club_memberships (
                    id, club_id, student_id, status, applied_at, updated_at
                ) VALUES (:id, :clubId, :studentId, 'PENDING', :appliedAt, :appliedAt)
                """, membershipParameters(clubId, studentId, appliedAt).addValue("id", id));
    }

    @Override
    public void reactivateApplication(UUID clubId, UUID studentId, Instant appliedAt) {
        requireSingleChange(jdbcTemplate.update("""
                UPDATE student_club_memberships
                SET status = 'PENDING', applied_at = :appliedAt, updated_at = :appliedAt
                WHERE club_id = :clubId AND student_id = :studentId
                  AND status IN ('REJECTED', 'WITHDRAWN')
                """, membershipParameters(clubId, studentId, appliedAt)));
    }

    @Override
    public void withdrawApplication(UUID clubId, UUID studentId, Instant withdrawnAt) {
        requireSingleChange(jdbcTemplate.update("""
                UPDATE student_club_memberships
                SET status = 'WITHDRAWN', updated_at = :appliedAt
                WHERE club_id = :clubId AND student_id = :studentId AND status = 'PENDING'
                """, membershipParameters(clubId, studentId, withdrawnAt)));
    }

    private ClubProjection mapProjection(ResultSet rs, int row) throws SQLException {
        return new ClubProjection(
                mapClub(rs, row),
                rs.getInt("active_count"),
                ClubMembershipStatus.valueOf(rs.getString("membership_status")));
    }

    private SchoolClub mapClub(ResultSet rs, int row) throws SQLException {
        OffsetDateTime deadline = rs.getObject("application_deadline", OffsetDateTime.class);
        return new SchoolClub(
                rs.getObject("id", UUID.class),
                ClubCategory.valueOf(rs.getString("category")),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("advisor_name"),
                rs.getString("meeting_schedule"),
                rs.getString("location"),
                ClubAudience.valueOf(rs.getString("audience")),
                rs.getObject("audience_grade_level", Integer.class),
                rs.getObject("capacity", Integer.class),
                deadline == null ? null : deadline.toInstant(),
                rs.getBoolean("accepting_applications"));
    }

    private static MapSqlParameterSource projectionParameters(UUID studentId, int gradeLevel) {
        return new MapSqlParameterSource()
                .addValue("studentId", studentId)
                .addValue("gradeLevel", gradeLevel, Types.SMALLINT);
    }

    private static MapSqlParameterSource membershipParameters(
            UUID clubId, UUID studentId, Instant appliedAt) {
        return new MapSqlParameterSource()
                .addValue("clubId", clubId)
                .addValue("studentId", studentId)
                .addValue("appliedAt", OffsetDateTime.ofInstant(appliedAt, ZoneOffset.UTC),
                        Types.TIMESTAMP_WITH_TIMEZONE);
    }

    private static void requireSingleChange(int changed) {
        if (changed != 1) {
            throw new IllegalStateException("Expected exactly one club membership to change");
        }
    }
}
