package vn.edu.fpt.myschool.admin.identity.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
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

    private static final Map<String, String> PARENT_SORTS = Map.of(
            "fullName,asc", "parent.full_name ASC, parent.id ASC",
            "fullName,desc", "parent.full_name DESC, parent.id DESC",
            "updatedAt,desc", "parent.updated_at DESC, parent.id DESC");

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

    // linked_students counts only links in force, so ending a link is reflected immediately.
    private static final String PARENT_FROM = """
            FROM parent_profiles parent
            LEFT JOIN users account ON account.id = parent.user_id
            WHERE (CAST(:query AS VARCHAR) IS NULL OR
                   lower(parent.full_name) LIKE lower('%' || CAST(:query AS VARCHAR) || '%') OR
                   account.phone_number LIKE '%' || CAST(:query AS VARCHAR) || '%')
              AND (CAST(:enabled AS BOOLEAN) IS NULL OR parent.enabled = :enabled)
            """;

    @Override
    public AdminIdentity.ParentPage findParents(
            String query, Boolean enabled, int page, int size, String sort) {
        String orderBy = PARENT_SORTS.get(sort);
        if (orderBy == null) {
            throw new IllegalArgumentException("Unsupported sort: " + sort);
        }
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("query", query)
                .addValue("enabled", enabled);

        Long total = namedJdbcTemplate.queryForObject(
                "SELECT count(*) " + PARENT_FROM, parameters, Long.class);

        List<AdminIdentity.Parent> items = namedJdbcTemplate.query(
                """
                SELECT parent.id, parent.user_id, parent.full_name, parent.email,
                       account.phone_number, parent.enabled, parent.version,
                       (SELECT count(*) FROM parent_student_links link
                        WHERE link.parent_id = parent.id AND link.effective_to IS NULL)
                           AS linked_students
                """
                        + PARENT_FROM
                        + " ORDER BY " + orderBy + " LIMIT :size OFFSET :offset",
                parameters.addValue("size", size).addValue("offset", (long) page * size),
                JdbcAdminIdentityStore::mapParent);

        return new AdminIdentity.ParentPage(items, page, size, total == null ? 0 : total);
    }

    @Override
    public Optional<AdminIdentity.Parent> findParent(UUID parentId) {
        return jdbcTemplate.query(
                        """
                        SELECT parent.id, parent.user_id, parent.full_name, parent.email,
                               account.phone_number, parent.enabled, parent.version,
                               (SELECT count(*) FROM parent_student_links link
                                WHERE link.parent_id = parent.id AND link.effective_to IS NULL)
                                   AS linked_students
                        FROM parent_profiles parent
                        LEFT JOIN users account ON account.id = parent.user_id
                        WHERE parent.id = ?
                        """,
                        JdbcAdminIdentityStore::mapParent,
                        parentId)
                .stream()
                .findFirst();
    }

    @Override
    public boolean phoneNumberExists(String phoneNumber) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM users WHERE phone_number = ?", Integer.class, phoneNumber);
        return count != null && count > 0;
    }

    @Override
    public UUID createParent(
            String fullName, String email, String phoneNumber, String passwordHash, Instant now) {
        UUID parentId = UUID.randomUUID();
        Timestamp timestamp = Timestamp.from(now);
        UUID userId = null;
        if (phoneNumber != null) {
            userId = UUID.randomUUID();
            jdbcTemplate.update(
                    """
                    INSERT INTO users (
                        id, phone_number, password_hash, enabled,
                        credentials_updated_at, created_at, updated_at
                    ) VALUES (?, ?, ?, TRUE, ?, ?, ?)
                    """,
                    userId, phoneNumber, passwordHash, timestamp, timestamp, timestamp);
            jdbcTemplate.update(
                    "INSERT INTO user_roles (user_id, role, created_at) VALUES (?, 'PARENT', ?)",
                    userId, timestamp);
        }
        jdbcTemplate.update(
                """
                INSERT INTO parent_profiles
                    (id, user_id, full_name, email, enabled, version, created_at, updated_at)
                VALUES (?, ?, ?, ?, TRUE, 0, ?, ?)
                """,
                parentId, userId, fullName, email, timestamp, timestamp);
        return parentId;
    }

    @Override
    public boolean updateParent(
            UUID parentId, String fullName, String email, boolean enabled,
            long expectedVersion, Instant now) {
        return jdbcTemplate.update(
                """
                UPDATE parent_profiles
                SET full_name = ?, email = ?, enabled = ?, version = version + 1, updated_at = ?
                WHERE id = ? AND version = ?
                """,
                fullName, email, enabled, Timestamp.from(now), parentId, expectedVersion) == 1;
    }

    @Override
    public List<AdminIdentity.GuardianLink> findLinks(
            UUID parentId, UUID studentId, boolean inForceOnly) {
        return jdbcTemplate.query(
                """
                SELECT link.id, link.parent_id, parent.full_name AS parent_full_name,
                       link.student_id, student.full_name AS student_full_name,
                       student.student_code, link.relationship, link.contact_order,
                       link.effective_from, link.effective_to
                FROM parent_student_links link
                INNER JOIN parent_profiles parent ON parent.id = link.parent_id
                INNER JOIN students student ON student.id = link.student_id
                WHERE (CAST(? AS UUID) IS NULL OR link.parent_id = ?)
                  AND (CAST(? AS UUID) IS NULL OR link.student_id = ?)
                  AND (? = FALSE OR link.effective_to IS NULL)
                ORDER BY link.contact_order ASC, link.effective_from DESC, link.id ASC
                """,
                JdbcAdminIdentityStore::mapLink,
                parentId, parentId, studentId, studentId, inForceOnly);
    }

    @Override
    public boolean studentExists(UUID studentId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM students WHERE id = ?", Integer.class, studentId);
        return count != null && count > 0;
    }

    @Override
    public boolean linkInForceExists(UUID parentId, UUID studentId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT count(*) FROM parent_student_links
                WHERE parent_id = ? AND student_id = ? AND effective_to IS NULL
                """,
                Integer.class,
                parentId, studentId);
        return count != null && count > 0;
    }

    @Override
    public UUID createLink(
            UUID parentId,
            UUID studentId,
            AdminIdentity.Relationship relationship,
            int contactOrder,
            LocalDate effectiveFrom,
            Instant now) {
        UUID id = UUID.randomUUID();
        Timestamp timestamp = Timestamp.from(now);
        jdbcTemplate.update(
                """
                INSERT INTO parent_student_links
                    (id, parent_id, student_id, relationship, contact_order,
                     effective_from, effective_to, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, NULL, ?, ?)
                """,
                id, parentId, studentId, relationship.name(), contactOrder,
                java.sql.Date.valueOf(effectiveFrom), timestamp, timestamp);
        return id;
    }

    @Override
    public boolean endLink(UUID linkId, LocalDate effectiveTo, Instant now) {
        return jdbcTemplate.update(
                """
                UPDATE parent_student_links
                SET effective_to = ?, updated_at = ?
                WHERE id = ? AND effective_to IS NULL AND effective_from <= ?
                """,
                java.sql.Date.valueOf(effectiveTo), Timestamp.from(now), linkId,
                java.sql.Date.valueOf(effectiveTo)) == 1;
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

    private static AdminIdentity.Parent mapParent(ResultSet rs, int rowNum) throws SQLException {
        return new AdminIdentity.Parent(
                rs.getObject("id", UUID.class),
                rs.getObject("user_id", UUID.class),
                rs.getString("full_name"),
                rs.getString("email"),
                rs.getString("phone_number"),
                rs.getBoolean("enabled"),
                rs.getLong("version"),
                rs.getInt("linked_students"));
    }

    private static AdminIdentity.GuardianLink mapLink(ResultSet rs, int rowNum) throws SQLException {
        java.sql.Date effectiveTo = rs.getDate("effective_to");
        return new AdminIdentity.GuardianLink(
                rs.getObject("id", UUID.class),
                rs.getObject("parent_id", UUID.class),
                rs.getString("parent_full_name"),
                rs.getObject("student_id", UUID.class),
                rs.getString("student_full_name"),
                rs.getString("student_code"),
                AdminIdentity.Relationship.valueOf(rs.getString("relationship")),
                rs.getInt("contact_order"),
                rs.getDate("effective_from").toLocalDate(),
                effectiveTo == null ? null : effectiveTo.toLocalDate());
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
