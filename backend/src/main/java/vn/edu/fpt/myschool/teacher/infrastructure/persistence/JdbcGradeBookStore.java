package vn.edu.fpt.myschool.teacher.infrastructure.persistence;

import java.math.BigDecimal;
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

import vn.edu.fpt.myschool.teacher.application.port.GradeBookStore;
import vn.edu.fpt.myschool.teacher.domain.GradeBookView;

@Repository
class JdbcGradeBookStore implements GradeBookStore {

    private static final String BOOK_SELECT = """
            SELECT book.id, book.class_id, school_class.code AS class_code,
                   book.subject_id, subject.code AS subject_code, subject.name AS subject_name,
                   book.academic_term_id, term.code AS academic_term_code,
                   book.published_at, book.locked_at, book.version
            FROM grade_books book
            INNER JOIN school_classes school_class ON school_class.id = book.class_id
            INNER JOIN subjects subject ON subject.id = book.subject_id
            INNER JOIN academic_terms term ON term.id = book.academic_term_id
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    JdbcGradeBookStore(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<GradeBookView.Book> findBook(UUID bookId) {
        return jdbcTemplate.query(
                        BOOK_SELECT + " WHERE book.id = :bookId",
                        new MapSqlParameterSource("bookId", bookId),
                        JdbcGradeBookStore::mapBook)
                .stream()
                .findFirst();
    }

    @Override
    public Optional<GradeBookView.Book> findBookBySlot(
            UUID classId, UUID subjectId, UUID academicTermId) {
        return jdbcTemplate.query(
                        BOOK_SELECT + """
                         WHERE book.class_id = :classId
                           AND book.subject_id = :subjectId
                           AND book.academic_term_id = :academicTermId
                        """,
                        new MapSqlParameterSource("classId", classId)
                                .addValue("subjectId", subjectId)
                                .addValue("academicTermId", academicTermId),
                        JdbcGradeBookStore::mapBook)
                .stream()
                .findFirst();
    }

    @Override
    public UUID createBook(UUID classId, UUID subjectId, UUID academicTermId, Instant now) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO grade_books (
                    id, class_id, subject_id, academic_term_id, version, created_at, updated_at
                ) VALUES (:id, :classId, :subjectId, :academicTermId, 0, :now, :now)
                ON CONFLICT (class_id, subject_id, academic_term_id) DO NOTHING
                """,
                new MapSqlParameterSource("id", id)
                        .addValue("classId", classId)
                        .addValue("subjectId", subjectId)
                        .addValue("academicTermId", academicTermId)
                        .addValue("now", Timestamp.from(now)));
        return findBookBySlot(classId, subjectId, academicTermId)
                .map(GradeBookView.Book::id)
                .orElseThrow(() -> new IllegalStateException("Grade book vanished after creation"));
    }

    @Override
    public List<GradeBookView.Column> findColumns(UUID bookId) {
        return jdbcTemplate.query(
                """
                SELECT id, assessment_kind, assessment_form, display_label,
                       duration_minutes, display_order
                FROM grade_columns
                WHERE grade_book_id = :bookId
                ORDER BY display_order
                """,
                new MapSqlParameterSource("bookId", bookId),
                (rs, rowNum) -> new GradeBookView.Column(
                        rs.getObject("id", UUID.class),
                        rs.getString("assessment_kind"),
                        rs.getString("assessment_form"),
                        rs.getString("display_label"),
                        rs.getObject("duration_minutes", Integer.class),
                        rs.getInt("display_order")));
    }

    @Override
    public int nextColumnOrder(UUID bookId) {
        Integer max = jdbcTemplate.queryForObject(
                "SELECT COALESCE(max(display_order), 0) FROM grade_columns WHERE grade_book_id = :bookId",
                new MapSqlParameterSource("bookId", bookId),
                Integer.class);
        return (max == null ? 0 : max) + 1;
    }

    @Override
    public UUID createColumn(
            UUID bookId,
            String assessmentKind,
            String assessmentForm,
            String displayLabel,
            Integer durationMinutes,
            int displayOrder,
            Instant now) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO grade_columns (
                    id, grade_book_id, assessment_kind, assessment_form, display_label,
                    duration_minutes, display_order, created_at, updated_at
                ) VALUES (:id, :bookId, :kind, :form, :label, :duration, :order, :now, :now)
                """,
                new MapSqlParameterSource("id", id)
                        .addValue("bookId", bookId)
                        .addValue("kind", assessmentKind)
                        .addValue("form", assessmentForm)
                        .addValue("label", displayLabel)
                        .addValue("duration", durationMinutes)
                        .addValue("order", displayOrder)
                        .addValue("now", Timestamp.from(now)));
        return id;
    }

    /**
     * One empty cell per student enrolled in this book's subject and term.
     *
     * <p>Driven from student_term_subjects rather than the class roll: a student only has a mark
     * in a subject they are enrolled in, and enrolment is what carries the assessment mode.
     */
    @Override
    public int createPendingMarks(UUID bookId, UUID columnId, Instant now) {
        return jdbcTemplate.update(
                """
                INSERT INTO grade_assessments (
                    id, student_term_subject_id, grade_column_id, assessment_mode,
                    assessment_kind, assessment_form, display_label, duration_minutes,
                    status, score, outcome, display_order, created_at, updated_at
                )
                SELECT gen_random_uuid(), enrollment.id, grade_column.id,
                       enrollment.assessment_mode, grade_column.assessment_kind,
                       grade_column.assessment_form, grade_column.display_label,
                       grade_column.duration_minutes, 'PENDING', NULL, NULL,
                       grade_column.display_order, :now, :now
                FROM grade_columns grade_column
                INNER JOIN grade_books book ON book.id = grade_column.grade_book_id
                INNER JOIN students student ON student.class_id = book.class_id
                INNER JOIN student_term_subjects enrollment
                    ON enrollment.student_id = student.id
                    AND enrollment.subject_id = book.subject_id
                    AND enrollment.academic_term_id = book.academic_term_id
                WHERE grade_column.id = :columnId AND book.id = :bookId
                ON CONFLICT (student_term_subject_id, display_order) DO NOTHING
                """,
                new MapSqlParameterSource("bookId", bookId)
                        .addValue("columnId", columnId)
                        .addValue("now", Timestamp.from(now)));
    }

    @Override
    public List<GradeBookView.Mark> findMarks(UUID bookId) {
        return jdbcTemplate.query(
                """
                SELECT assessment.id AS assessment_id, assessment.grade_column_id,
                       enrollment.student_id, assessment.status, assessment.score,
                       assessment.outcome
                FROM grade_assessments assessment
                INNER JOIN grade_columns grade_column
                    ON grade_column.id = assessment.grade_column_id
                INNER JOIN student_term_subjects enrollment
                    ON enrollment.id = assessment.student_term_subject_id
                WHERE grade_column.grade_book_id = :bookId
                ORDER BY grade_column.display_order, enrollment.student_id
                """,
                new MapSqlParameterSource("bookId", bookId),
                (rs, rowNum) -> new GradeBookView.Mark(
                        rs.getObject("assessment_id", UUID.class),
                        rs.getObject("grade_column_id", UUID.class),
                        rs.getObject("student_id", UUID.class),
                        rs.getString("status"),
                        rs.getBigDecimal("score"),
                        rs.getString("outcome")));
    }

    @Override
    public Optional<UUID> findBookIdByAssessment(UUID assessmentId) {
        return jdbcTemplate.query(
                        """
                        SELECT grade_column.grade_book_id
                        FROM grade_assessments assessment
                        INNER JOIN grade_columns grade_column
                            ON grade_column.id = assessment.grade_column_id
                        WHERE assessment.id = :assessmentId
                        """,
                        new MapSqlParameterSource("assessmentId", assessmentId),
                        (rs, rowNum) -> rs.getObject("grade_book_id", UUID.class))
                .stream()
                .findFirst();
    }

    @Override
    public boolean recordMark(
            UUID assessmentId, BigDecimal score, String outcome, String status, Instant now) {
        return jdbcTemplate.update(
                """
                UPDATE grade_assessments
                SET score = :score, outcome = :outcome, status = :status, updated_at = :now
                WHERE id = :assessmentId
                """,
                new MapSqlParameterSource("assessmentId", assessmentId)
                        .addValue("score", score)
                        .addValue("outcome", outcome)
                        .addValue("status", status)
                        .addValue("now", Timestamp.from(now))) == 1;
    }

    @Override
    public boolean publish(UUID bookId, UUID publishedBy, long expectedVersion, Instant now) {
        return jdbcTemplate.update(
                """
                UPDATE grade_books
                SET published_at = :now, published_by = :publishedBy,
                    version = version + 1, updated_at = :now
                WHERE id = :bookId AND version = :expectedVersion
                """,
                new MapSqlParameterSource("bookId", bookId)
                        .addValue("publishedBy", publishedBy)
                        .addValue("expectedVersion", expectedVersion)
                        .addValue("now", Timestamp.from(now))) == 1;
    }

    @Override
    public boolean unpublish(UUID bookId, long expectedVersion, Instant now) {
        return jdbcTemplate.update(
                """
                UPDATE grade_books
                SET published_at = NULL, published_by = NULL,
                    version = version + 1, updated_at = :now
                WHERE id = :bookId AND version = :expectedVersion
                """,
                new MapSqlParameterSource("bookId", bookId)
                        .addValue("expectedVersion", expectedVersion)
                        .addValue("now", Timestamp.from(now))) == 1;
    }

    private static GradeBookView.Book mapBook(ResultSet rs, int rowNum) throws SQLException {
        Timestamp publishedAt = rs.getTimestamp("published_at");
        Timestamp lockedAt = rs.getTimestamp("locked_at");
        return new GradeBookView.Book(
                rs.getObject("id", UUID.class),
                rs.getObject("class_id", UUID.class),
                rs.getString("class_code"),
                rs.getObject("subject_id", UUID.class),
                rs.getString("subject_code"),
                rs.getString("subject_name"),
                rs.getObject("academic_term_id", UUID.class),
                rs.getString("academic_term_code"),
                publishedAt == null ? null : publishedAt.toInstant(),
                lockedAt == null ? null : lockedAt.toInstant(),
                rs.getLong("version"));
    }
}
