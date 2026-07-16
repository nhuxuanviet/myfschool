package vn.edu.fpt.myschool.admin.academics.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import vn.edu.fpt.myschool.admin.academics.application.port.AdminAcademicsStore;
import vn.edu.fpt.myschool.admin.academics.domain.AdminAcademics;

@Repository
class JdbcAdminAcademicsStore implements AdminAcademicsStore {

    private static final Map<StudentSort, String> STUDENT_SORTS = Map.of(
            StudentSort.FULL_NAME_ASC, "lower(student.full_name) ASC, student.id ASC",
            StudentSort.FULL_NAME_DESC, "lower(student.full_name) DESC, student.id DESC",
            StudentSort.STUDENT_CODE_ASC, "student.student_code ASC, student.id ASC",
            StudentSort.UPDATED_AT_DESC, "student.updated_at DESC, student.id DESC");

    private static final String STUDENT_FROM = """
            FROM students student
            INNER JOIN users account ON account.id = student.user_id
            LEFT JOIN school_classes school_class ON school_class.id = student.class_id
            WHERE EXISTS (SELECT 1 FROM user_roles ur
                          WHERE ur.user_id = account.id AND ur.role = 'STUDENT')
              AND (CAST(:query AS VARCHAR) IS NULL OR
                   lower(student.full_name) LIKE lower('%' || CAST(:query AS VARCHAR) || '%') OR
                   lower(student.student_code) LIKE lower('%' || CAST(:query AS VARCHAR) || '%') OR
                   account.phone_number LIKE '%' || CAST(:query AS VARCHAR) || '%')
              AND (CAST(:gradeLevel AS INTEGER) IS NULL OR student.grade_level = :gradeLevel)
              AND (CAST(:classId AS UUID) IS NULL OR student.class_id = :classId)
              AND (CAST(:enabled AS BOOLEAN) IS NULL OR account.enabled = :enabled)
            """;

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    JdbcAdminAcademicsStore(
            JdbcTemplate jdbcTemplate,
            NamedParameterJdbcTemplate namedJdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedJdbcTemplate = namedJdbcTemplate;
    }

    @Override
    public AdminAcademics.Catalog loadCatalog() {
        List<AdminAcademics.AcademicYear> years = jdbcTemplate.query(
                """
                SELECT id, code, starts_on, ends_on, version
                FROM academic_years
                ORDER BY starts_on DESC, id
                """,
                this::mapAcademicYear);
        List<AdminAcademics.AcademicTerm> terms = jdbcTemplate.query(
                """
                SELECT id, academic_year_id, code, name, starts_on, ends_on, version
                FROM academic_terms
                ORDER BY starts_on DESC, id
                """,
                this::mapAcademicTerm);
        List<AdminAcademics.Subject> subjects = jdbcTemplate.query(
                """
                SELECT id, code, name, enabled, version
                FROM subjects
                ORDER BY enabled DESC, name, id
                """,
                this::mapSubject);
        List<AdminAcademics.SchoolClass> classes = jdbcTemplate.query(
                """
                SELECT school_class.id,
                       school_class.academic_year_id,
                       school_class.code,
                       school_class.name,
                       school_class.grade_level,
                       school_class.enabled,
                       school_class.version,
                       COUNT(student.id) AS student_count
                FROM school_classes school_class
                LEFT JOIN students student ON student.class_id = school_class.id
                GROUP BY school_class.id
                ORDER BY school_class.enabled DESC,
                         school_class.grade_level,
                         school_class.code,
                         school_class.id
                """,
                this::mapSchoolClass);
        return new AdminAcademics.Catalog(years, terms, subjects, classes);
    }

    @Override
    public AdminAcademics.StudentPage findStudents(
            String query,
            Integer gradeLevel,
            UUID classId,
            Boolean enabled,
            int page,
            int size,
            StudentSort sort) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("query", query)
                .addValue("gradeLevel", gradeLevel)
                .addValue("classId", classId)
                .addValue("enabled", enabled)
                .addValue("limit", size)
                .addValue("offset", page * size);
        Long total = namedJdbcTemplate.queryForObject(
                "SELECT COUNT(*) " + STUDENT_FROM,
                parameters,
                Long.class);
        String dataSql = """
                SELECT student.id,
                       student.student_code,
                       student.full_name,
                       account.phone_number,
                       student.grade_level,
                       student.class_id,
                       COALESCE(school_class.code, student.class_name) AS class_code,
                       account.enabled,
                       student.version,
                       student.updated_at
                """ + STUDENT_FROM + " ORDER BY " + STUDENT_SORTS.get(sort)
                + " LIMIT :limit OFFSET :offset";
        List<AdminAcademics.Student> items = namedJdbcTemplate.query(
                dataSql,
                parameters,
                this::mapStudent);
        long totalElements = total == null ? 0 : total;
        int totalPages = totalElements == 0 ? 0 : (int) ((totalElements + size - 1) / size);
        return new AdminAcademics.StudentPage(items, page, size, totalElements, totalPages);
    }

    @Override
    public Optional<AdminAcademics.SchoolClass> findClass(UUID classId) {
        List<AdminAcademics.SchoolClass> results = jdbcTemplate.query(
                """
                SELECT school_class.id,
                       school_class.academic_year_id,
                       school_class.code,
                       school_class.name,
                       school_class.grade_level,
                       school_class.enabled,
                       school_class.version,
                       COUNT(student.id) AS student_count
                FROM school_classes school_class
                LEFT JOIN students student ON student.class_id = school_class.id
                WHERE school_class.id = ?
                GROUP BY school_class.id
                """,
                this::mapSchoolClass,
                classId);
        return results.stream().findFirst();
    }

    @Override
    public Optional<AdminAcademics.Student> findStudent(UUID studentId) {
        List<AdminAcademics.Student> results = jdbcTemplate.query(
                """
                SELECT student.id,
                       student.student_code,
                       student.full_name,
                       account.phone_number,
                       student.grade_level,
                       student.class_id,
                       COALESCE(school_class.code, student.class_name) AS class_code,
                       account.enabled,
                       student.version,
                       student.updated_at
                FROM students student
                INNER JOIN users account ON account.id = student.user_id
                LEFT JOIN school_classes school_class ON school_class.id = student.class_id
                WHERE student.id = ?
                  AND EXISTS (SELECT 1 FROM user_roles ur
                              WHERE ur.user_id = account.id AND ur.role = 'STUDENT')
                """,
                this::mapStudent,
                studentId);
        return results.stream().findFirst();
    }

    @Override
    public UUID createAcademicYear(
            String code,
            java.time.LocalDate startsOn,
            java.time.LocalDate endsOn,
            Instant now) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO academic_years (
                    id, code, starts_on, ends_on, version, created_at, updated_at
                ) VALUES (?, ?, ?, ?, 0, ?, ?)
                """,
                id, code, startsOn, endsOn, Timestamp.from(now), Timestamp.from(now));
        return id;
    }

    @Override
    public boolean updateAcademicYear(
            UUID id,
            String code,
            java.time.LocalDate startsOn,
            java.time.LocalDate endsOn,
            long version,
            Instant now) {
        return jdbcTemplate.update(
                """
                UPDATE academic_years
                SET code = ?, starts_on = ?, ends_on = ?,
                    version = version + 1, updated_at = ?
                WHERE id = ? AND version = ?
                """,
                code, startsOn, endsOn, Timestamp.from(now), id, version) == 1;
    }

    @Override
    public UUID createAcademicTerm(
            UUID academicYearId,
            String code,
            String name,
            java.time.LocalDate startsOn,
            java.time.LocalDate endsOn,
            Instant now) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO academic_terms (
                    id, academic_year_id, code, name, starts_on, ends_on,
                    version, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, 0, ?, ?)
                """,
                id, academicYearId, code, name, startsOn, endsOn,
                Timestamp.from(now), Timestamp.from(now));
        return id;
    }

    @Override
    public boolean updateAcademicTerm(
            UUID id,
            UUID academicYearId,
            String code,
            String name,
            java.time.LocalDate startsOn,
            java.time.LocalDate endsOn,
            long version,
            Instant now) {
        return jdbcTemplate.update(
                """
                UPDATE academic_terms
                SET academic_year_id = ?, code = ?, name = ?, starts_on = ?, ends_on = ?,
                    version = version + 1, updated_at = ?
                WHERE id = ? AND version = ?
                """,
                academicYearId, code, name, startsOn, endsOn,
                Timestamp.from(now), id, version) == 1;
    }

    @Override
    public UUID createSubject(String code, String name, Instant now) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO subjects (
                    id, code, name, enabled, version, created_at, updated_at
                ) VALUES (?, ?, ?, TRUE, 0, ?, ?)
                """,
                id, code, name, Timestamp.from(now), Timestamp.from(now));
        return id;
    }

    @Override
    public boolean updateSubject(
            UUID id,
            String code,
            String name,
            boolean enabled,
            long version,
            Instant now) {
        return jdbcTemplate.update(
                """
                UPDATE subjects
                SET code = ?, name = ?, enabled = ?, version = version + 1, updated_at = ?
                WHERE id = ? AND version = ?
                """,
                code, name, enabled, Timestamp.from(now), id, version) == 1;
    }

    @Override
    public UUID createClass(
            UUID academicYearId,
            String code,
            String name,
            int gradeLevel,
            Instant now) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO school_classes (
                    id, academic_year_id, code, name, grade_level,
                    enabled, version, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, TRUE, 0, ?, ?)
                """,
                id, academicYearId, code, name, gradeLevel,
                Timestamp.from(now), Timestamp.from(now));
        return id;
    }

    @Override
    public boolean updateClass(
            UUID id,
            UUID academicYearId,
            String code,
            String name,
            int gradeLevel,
            boolean enabled,
            long version,
            Instant now) {
        int updated = jdbcTemplate.update(
                """
                UPDATE school_classes
                SET academic_year_id = ?, code = ?, name = ?, grade_level = ?, enabled = ?,
                    version = version + 1, updated_at = ?
                WHERE id = ? AND version = ?
                """,
                academicYearId, code, name, gradeLevel, enabled,
                Timestamp.from(now), id, version);
        if (updated == 1) {
            jdbcTemplate.update(
                    """
                    UPDATE students
                    SET class_name = ?, grade_level = ?, updated_at = ?
                    WHERE class_id = ?
                    """,
                    code, gradeLevel, Timestamp.from(now), id);
        }
        return updated == 1;
    }

    @Override
    public boolean deleteAcademicYear(UUID id, long version) {
        return jdbcTemplate.update(
                "DELETE FROM academic_years WHERE id = ? AND version = ?", id, version) == 1;
    }

    @Override
    public boolean deleteAcademicTerm(UUID id, long version) {
        return jdbcTemplate.update(
                "DELETE FROM academic_terms WHERE id = ? AND version = ?", id, version) == 1;
    }

    @Override
    public boolean deleteSubject(UUID id, long version) {
        return jdbcTemplate.update(
                "DELETE FROM subjects WHERE id = ? AND version = ?", id, version) == 1;
    }

    @Override
    public boolean deleteClass(UUID id, long version) {
        return jdbcTemplate.update(
                "DELETE FROM school_classes WHERE id = ? AND version = ?", id, version) == 1;
    }

    @Override
    public UUID createStudent(
            String phoneNumber,
            String passwordHash,
            String studentCode,
            String fullName,
            AdminAcademics.SchoolClass schoolClass,
            Instant now) {
        UUID userId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        Timestamp timestamp = Timestamp.from(now);
        jdbcTemplate.update(
                """
                INSERT INTO users (
                    id, phone_number, password_hash, enabled,
                    credentials_updated_at, created_at, updated_at
                ) VALUES (?, ?, ?, TRUE, ?, ?, ?)
                """,
                userId, phoneNumber, passwordHash, timestamp, timestamp, timestamp);
        // user_roles is the source of truth for authorisation, so it must be written in the
        // same transaction that creates the account. An account with no role row cannot be
        // loaded at all.
        jdbcTemplate.update(
                """
                INSERT INTO user_roles (user_id, role, created_at)
                VALUES (?, 'STUDENT', ?)
                """,
                userId, timestamp);
        jdbcTemplate.update(
                """
                INSERT INTO students (
                    id, user_id, student_code, full_name, grade_level,
                    class_name, class_id, version, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, 0, ?, ?)
                """,
                studentId, userId, studentCode, fullName, schoolClass.gradeLevel(),
                schoolClass.code(), schoolClass.id(), timestamp, timestamp);
        return studentId;
    }

    @Override
    public boolean updateStudent(
            UUID studentId,
            String phoneNumber,
            String studentCode,
            String fullName,
            AdminAcademics.SchoolClass schoolClass,
            boolean enabled,
            long version,
            Instant now) {
        Timestamp timestamp = Timestamp.from(now);
        int updated = jdbcTemplate.update(
                """
                UPDATE students
                SET student_code = ?, full_name = ?, grade_level = ?,
                    class_name = ?, class_id = ?, version = version + 1, updated_at = ?
                WHERE id = ? AND version = ?
                """,
                studentCode, fullName, schoolClass.gradeLevel(), schoolClass.code(),
                schoolClass.id(), timestamp, studentId, version);
        if (updated == 0) {
            return false;
        }
        jdbcTemplate.update(
                """
                UPDATE users account
                SET phone_number = ?, enabled = ?, updated_at = ?
                FROM students student
                WHERE student.user_id = account.id AND student.id = ?
                """,
                phoneNumber, enabled, timestamp, studentId);
        return true;
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

    private AdminAcademics.AcademicYear mapAcademicYear(ResultSet resultSet, int rowNumber)
            throws SQLException {
        return new AdminAcademics.AcademicYear(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("code"),
                resultSet.getObject("starts_on", java.time.LocalDate.class),
                resultSet.getObject("ends_on", java.time.LocalDate.class),
                resultSet.getLong("version"));
    }

    private AdminAcademics.AcademicTerm mapAcademicTerm(ResultSet resultSet, int rowNumber)
            throws SQLException {
        return new AdminAcademics.AcademicTerm(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("academic_year_id", UUID.class),
                resultSet.getString("code"),
                resultSet.getString("name"),
                resultSet.getObject("starts_on", java.time.LocalDate.class),
                resultSet.getObject("ends_on", java.time.LocalDate.class),
                resultSet.getLong("version"));
    }

    private AdminAcademics.Subject mapSubject(ResultSet resultSet, int rowNumber)
            throws SQLException {
        return new AdminAcademics.Subject(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("code"),
                resultSet.getString("name"),
                resultSet.getBoolean("enabled"),
                resultSet.getLong("version"));
    }

    private AdminAcademics.SchoolClass mapSchoolClass(ResultSet resultSet, int rowNumber)
            throws SQLException {
        return new AdminAcademics.SchoolClass(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("academic_year_id", UUID.class),
                resultSet.getString("code"),
                resultSet.getString("name"),
                resultSet.getInt("grade_level"),
                resultSet.getBoolean("enabled"),
                resultSet.getLong("version"),
                resultSet.getLong("student_count"));
    }

    private AdminAcademics.Student mapStudent(ResultSet resultSet, int rowNumber)
            throws SQLException {
        return new AdminAcademics.Student(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("student_code"),
                resultSet.getString("full_name"),
                resultSet.getString("phone_number"),
                resultSet.getInt("grade_level"),
                resultSet.getObject("class_id", UUID.class),
                resultSet.getString("class_code"),
                resultSet.getBoolean("enabled"),
                resultSet.getLong("version"),
                resultSet.getObject("updated_at", OffsetDateTime.class).toInstant());
    }
}
