package vn.edu.fpt.myschool.admin.identity.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import vn.edu.fpt.myschool.admin.identity.application.port.AdminIdentityStore;
import vn.edu.fpt.myschool.admin.identity.domain.AdminIdentity;

@Repository
class JdbcAdminIdentityStore implements AdminIdentityStore {

    private static final Map<String, String> TEACHER_SORTS = Map.of(
            "fullName,asc", "teacher.full_name ASC, teacher.id ASC",
            "fullName,desc", "teacher.full_name DESC, teacher.id DESC",
            "teacherCode,asc", "teacher.teacher_code ASC, teacher.id ASC",
            "updatedAt,desc", "teacher.updated_at DESC, teacher.id DESC");

    // The phone number lives on the account, not the profile, so it is only known for a teacher
    // who has one. A LEFT JOIN keeps accountless teachers in the list rather than hiding them.
    private static final String TEACHER_FROM = """
            FROM teacher_profiles teacher
            LEFT JOIN users account ON account.id = teacher.user_id
            WHERE (CAST(:query AS VARCHAR) IS NULL OR
                   lower(teacher.full_name) LIKE lower('%' || CAST(:query AS VARCHAR) || '%') OR
                   lower(teacher.teacher_code) LIKE lower('%' || CAST(:query AS VARCHAR) || '%'))
              AND (CAST(:enabled AS BOOLEAN) IS NULL OR teacher.enabled = :enabled)
              AND (CAST(:hasAccount AS BOOLEAN) IS NULL
                   OR (teacher.user_id IS NOT NULL) = CAST(:hasAccount AS BOOLEAN))
            """;

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    JdbcAdminIdentityStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    @Override
    public AdminIdentity.TeacherPage findTeachers(
            String query, Boolean enabled, Boolean hasAccount, int page, int size, String sort) {
        String orderBy = TEACHER_SORTS.get(sort);
        if (orderBy == null) {
            throw new IllegalArgumentException("Unsupported sort: " + sort);
        }
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("query", query)
                .addValue("enabled", enabled)
                .addValue("hasAccount", hasAccount);

        Long total = namedJdbcTemplate.queryForObject(
                "SELECT count(*) " + TEACHER_FROM, parameters, Long.class);

        List<AdminIdentity.Teacher> items = namedJdbcTemplate.query(
                """
                SELECT teacher.id, teacher.user_id, teacher.teacher_code, teacher.full_name,
                       teacher.email, account.phone_number, teacher.enabled, teacher.version
                """
                        + TEACHER_FROM
                        + " ORDER BY " + orderBy + " LIMIT :size OFFSET :offset",
                parameters.addValue("size", size).addValue("offset", (long) page * size),
                JdbcAdminIdentityStore::mapTeacher);

        return new AdminIdentity.TeacherPage(items, page, size, total == null ? 0 : total);
    }

    @Override
    public Optional<AdminIdentity.Teacher> findTeacher(UUID teacherId) {
        return jdbcTemplate.query(
                        """
                        SELECT teacher.id, teacher.user_id, teacher.teacher_code, teacher.full_name,
                               teacher.email, account.phone_number, teacher.enabled, teacher.version
                        FROM teacher_profiles teacher
                        LEFT JOIN users account ON account.id = teacher.user_id
                        WHERE teacher.id = ?
                        """,
                        JdbcAdminIdentityStore::mapTeacher,
                        teacherId)
                .stream()
                .findFirst();
    }

    @Override
    public boolean teacherCodeExists(String teacherCode, UUID excludingTeacherId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT count(*) FROM teacher_profiles
                WHERE lower(teacher_code) = lower(?) AND (CAST(? AS UUID) IS NULL OR id <> ?)
                """,
                Integer.class,
                teacherCode, excludingTeacherId, excludingTeacherId);
        return count != null && count > 0;
    }

    @Override
    public UUID createTeacher(String teacherCode, String fullName, String email, Instant now) {
        UUID id = UUID.randomUUID();
        Timestamp timestamp = Timestamp.from(now);
        jdbcTemplate.update(
                """
                INSERT INTO teacher_profiles
                    (id, user_id, teacher_code, full_name, email, enabled, version,
                     created_at, updated_at)
                VALUES (?, NULL, ?, ?, ?, TRUE, 0, ?, ?)
                """,
                id, teacherCode, fullName, email, timestamp, timestamp);
        return id;
    }

    @Override
    public boolean updateTeacher(
            UUID teacherId,
            String teacherCode,
            String fullName,
            String email,
            boolean enabled,
            long expectedVersion,
            Instant now) {
        return jdbcTemplate.update(
                """
                UPDATE teacher_profiles
                SET teacher_code = ?, full_name = ?, email = ?, enabled = ?,
                    version = version + 1, updated_at = ?
                WHERE id = ? AND version = ?
                """,
                teacherCode, fullName, email, enabled, Timestamp.from(now),
                teacherId, expectedVersion) == 1;
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
                    id, actor_user_id, action, entity_type, entity_id,
                    changed_fields, occurred_at
                ) VALUES (?, ?, ?, ?, ?, CAST(? AS JSONB), ?)
                """,
                UUID.randomUUID(), actorUserId, action, entityType, entityId,
                changedFieldsJson, Timestamp.from(occurredAt));
    }

    private static AdminIdentity.Teacher mapTeacher(ResultSet rs, int rowNum) throws SQLException {
        return new AdminIdentity.Teacher(
                rs.getObject("id", UUID.class),
                rs.getObject("user_id", UUID.class),
                rs.getString("teacher_code"),
                rs.getString("full_name"),
                rs.getString("email"),
                rs.getString("phone_number"),
                rs.getBoolean("enabled"),
                rs.getLong("version"));
    }
}
