package vn.edu.fpt.myschool.admin.dashboard.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import vn.edu.fpt.myschool.admin.dashboard.application.port.AdminDashboardStore;
import vn.edu.fpt.myschool.admin.dashboard.domain.AdminDashboard;

@Repository
class JdbcAdminDashboardStore implements AdminDashboardStore {

    private static final String METRICS_SQL = """
            SELECT
                (SELECT COUNT(*)
                 FROM students s
                 INNER JOIN users u ON u.id = s.user_id
                 WHERE u.role = 'STUDENT' AND u.enabled = TRUE) AS total_students,
                (SELECT COUNT(DISTINCT s.class_name)
                 FROM students s
                 INNER JOIN users u ON u.id = s.user_id
                 WHERE u.role = 'STUDENT' AND u.enabled = TRUE) AS active_classes,
                (SELECT COUNT(*)
                 FROM student_forms
                 WHERE status IN ('SUBMITTED', 'IN_REVIEW')) AS pending_forms,
                (SELECT COUNT(*)
                 FROM school_events
                 WHERE starts_at >= ?) AS upcoming_events,
                (SELECT COUNT(*)
                 FROM student_club_memberships
                 WHERE status = 'PENDING') AS pending_club_applications,
                (SELECT COUNT(DISTINCT student_term_subject_id)
                 FROM grade_assessments
                 WHERE updated_at >= ?) AS recently_updated_grades
            """;

    private static final String RECENT_ACTIVITY_SQL = """
            SELECT events.id,
                   events.event_type,
                   COALESCE(admin_profiles.full_name, 'Hệ thống') AS actor_name,
                   events.occurred_at
            FROM security_audit_events events
            LEFT JOIN admin_profiles ON admin_profiles.user_id = events.user_id
            ORDER BY events.occurred_at DESC, events.id DESC
            LIMIT ?
            """;

    private final JdbcTemplate jdbcTemplate;

    JdbcAdminDashboardStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public AdminDashboard.Metrics loadMetrics(Instant now, Instant recentGradeCutoff) {
        return jdbcTemplate.queryForObject(
                METRICS_SQL,
                this::mapMetrics,
                Timestamp.from(now),
                Timestamp.from(recentGradeCutoff));
    }

    @Override
    public List<AdminDashboard.Activity> findRecentActivities(int limit) {
        return jdbcTemplate.query(RECENT_ACTIVITY_SQL, this::mapActivity, limit);
    }

    private AdminDashboard.Metrics mapMetrics(ResultSet resultSet, int rowNumber)
            throws SQLException {
        return new AdminDashboard.Metrics(
                resultSet.getLong("total_students"),
                resultSet.getLong("active_classes"),
                resultSet.getLong("pending_forms"),
                resultSet.getLong("upcoming_events"),
                resultSet.getLong("pending_club_applications"),
                resultSet.getLong("recently_updated_grades"));
    }

    private AdminDashboard.Activity mapActivity(ResultSet resultSet, int rowNumber)
            throws SQLException {
        return new AdminDashboard.Activity(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("event_type"),
                resultSet.getString("actor_name"),
                resultSet.getObject("occurred_at", OffsetDateTime.class).toInstant());
    }
}
