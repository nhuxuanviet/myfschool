package vn.edu.fpt.myschool.parent.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import vn.edu.fpt.myschool.parent.application.port.ParentStore;
import vn.edu.fpt.myschool.parent.domain.ParentChild;

@Repository
class JdbcParentStore implements ParentStore {

    /**
     * A link counts only while it is in force today.
     *
     * <p>effective_to is the day the link stops applying, so a link ending today still applies
     * today. Ended links stay in the table as a record and must not grant anything.
     */
    // Concatenated as a plain string, not spliced into a text block: text blocks strip
    // trailing spaces, which silently glued "AND" onto the next word.
    private static final String LINK_IN_FORCE =
            " AND link.effective_from <= CURRENT_DATE"
            + " AND (link.effective_to IS NULL OR link.effective_to > CURRENT_DATE)";

    private final NamedParameterJdbcTemplate jdbcTemplate;

    JdbcParentStore(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<UUID> findParentIdByUserId(UUID userId) {
        return jdbcTemplate.query(
                        """
                        SELECT id FROM parent_profiles
                        WHERE user_id = :userId AND enabled = TRUE
                        """,
                        new MapSqlParameterSource("userId", userId),
                        (rs, rowNum) -> rs.getObject("id", UUID.class))
                .stream()
                .findFirst();
    }

    @Override
    public List<ParentChild> findChildren(UUID parentId) {
        return jdbcTemplate.query(
                """
                SELECT student.id AS student_id, student.student_code, student.full_name,
                       student.grade_level, school_class.code AS class_code,
                       link.relationship, link.contact_order
                FROM parent_student_links link
                INNER JOIN students student ON student.id = link.student_id
                LEFT JOIN school_classes school_class ON school_class.id = student.class_id
                WHERE link.parent_id = :parentId
                """ + LINK_IN_FORCE + " ORDER BY link.contact_order, student.full_name",
                new MapSqlParameterSource("parentId", parentId),
                (rs, rowNum) -> new ParentChild(
                        rs.getObject("student_id", UUID.class),
                        rs.getString("student_code"),
                        rs.getString("full_name"),
                        rs.getInt("grade_level"),
                        rs.getString("class_code"),
                        rs.getString("relationship"),
                        rs.getInt("contact_order")));
    }

    @Override
    public boolean isLinkedToStudent(UUID parentId, UUID studentId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT count(*) FROM parent_student_links link
                WHERE link.parent_id = :parentId
                  AND link.student_id = :studentId
                """ + LINK_IN_FORCE,
                new MapSqlParameterSource("parentId", parentId).addValue("studentId", studentId),
                Integer.class);
        return count != null && count > 0;
    }
}
