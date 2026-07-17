package vn.edu.fpt.myschool.grades;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/** Verifies the V26 grade-book invariants and that no existing mark was lost or hidden. */
@Testcontainers
@ActiveProfiles({"test", "e2e"})
@SpringBootTest
@Transactional
class GradeBookMigrationIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRESQL =
            new PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine"));

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID anyBookId() {
        return jdbcTemplate.queryForObject("SELECT id FROM grade_books LIMIT 1", UUID.class);
    }

    /** Losing a mark by leaving it out of a column is the failure this migration must not have. */
    @Test
    void givesEveryExistingMarkAColumn() {
        Integer orphans = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM grade_assessments WHERE grade_column_id IS NULL",
                Integer.class);
        assertThat(orphans).isZero();
    }

    @Test
    void buildsOneBookPerClassSubjectAndTerm() {
        Integer books = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM grade_books", Integer.class);
        Integer distinctSlots = jdbcTemplate.queryForObject(
                """
                SELECT count(*) FROM (
                    SELECT DISTINCT student.class_id, term_subject.subject_id,
                                    term_subject.academic_term_id
                    FROM student_term_subjects term_subject
                    INNER JOIN students student ON student.id = term_subject.student_id
                    WHERE student.class_id IS NOT NULL
                ) AS slots
                """,
                Integer.class);
        assertThat(books).isEqualTo(distinctSlots).isGreaterThan(0);
    }

    /** Introducing the publish gate must not blank the student app for marks that already existed. */
    @Test
    void marksEveryMigratedBookAsAlreadyPublished() {
        Integer unpublished = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM grade_books WHERE published_at IS NULL", Integer.class);
        assertThat(unpublished).isZero();
    }

    @Test
    void startsEveryMigratedBookUnlocked() {
        Integer locked = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM grade_books WHERE locked_at IS NOT NULL", Integer.class);
        assertThat(locked).isZero();
    }

    @Test
    void keepsEveryColumnInsideItsOwnBook() {
        Integer mismatched = jdbcTemplate.queryForObject(
                """
                SELECT count(*)
                FROM grade_assessments assessment
                INNER JOIN grade_columns grade_column ON grade_column.id = assessment.grade_column_id
                INNER JOIN student_term_subjects term_subject
                    ON term_subject.id = assessment.student_term_subject_id
                INNER JOIN students student ON student.id = term_subject.student_id
                INNER JOIN grade_books book ON book.id = grade_column.grade_book_id
                WHERE book.class_id IS DISTINCT FROM student.class_id
                   OR book.subject_id IS DISTINCT FROM term_subject.subject_id
                   OR book.academic_term_id IS DISTINCT FROM term_subject.academic_term_id
                """,
                Integer.class);
        assertThat(mismatched).isZero();
    }

    /** An empty cell must be representable, or a teacher cannot create a column and fill it later. */
    @Test
    void acceptsAPendingMarkWithNoValue() {
        UUID termSubjectId = jdbcTemplate.queryForObject(
                "SELECT id FROM student_term_subjects WHERE assessment_mode = 'NUMERIC' LIMIT 1",
                UUID.class);
        UUID columnId = jdbcTemplate.queryForObject(
                """
                SELECT grade_column.id FROM grade_columns grade_column
                INNER JOIN grade_assessments assessment
                    ON assessment.grade_column_id = grade_column.id
                WHERE assessment.student_term_subject_id = ?
                LIMIT 1
                """,
                UUID.class, termSubjectId);

        jdbcTemplate.update(
                """
                INSERT INTO grade_assessments (
                    id, student_term_subject_id, grade_column_id, assessment_mode,
                    assessment_kind, assessment_form, display_label, status,
                    score, outcome, display_order, created_at, updated_at
                ) VALUES (?, ?, ?, 'NUMERIC', 'REGULAR', 'ORAL', 'Miệng chờ nhập', 'PENDING',
                          NULL, NULL, 99, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                UUID.randomUUID(), termSubjectId, columnId);

        Integer pending = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM grade_assessments WHERE status = 'PENDING'", Integer.class);
        assertThat(pending).isEqualTo(1);
    }

    @Test
    void refusesAPendingMarkThatCarriesAScore() {
        UUID termSubjectId = jdbcTemplate.queryForObject(
                "SELECT id FROM student_term_subjects WHERE assessment_mode = 'NUMERIC' LIMIT 1",
                UUID.class);

        assertThatThrownBy(() -> jdbcTemplate.update(
                        """
                        INSERT INTO grade_assessments (
                            id, student_term_subject_id, assessment_mode, assessment_kind,
                            assessment_form, display_label, status, score, outcome,
                            display_order, created_at, updated_at
                        ) VALUES (?, ?, 'NUMERIC', 'REGULAR', 'ORAL', 'Sai', 'PENDING',
                                  8.0, NULL, 98, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                        UUID.randomUUID(), termSubjectId))
                .hasMessageContaining("ck_grade_assessments_result_by_status");
    }

    @Test
    void refusesASecondFinalColumnInOneBook() {
        UUID bookId = anyBookId();
        jdbcTemplate.update(
                """
                INSERT INTO grade_columns (
                    id, grade_book_id, assessment_kind, assessment_form, display_label,
                    display_order, created_at, updated_at
                ) VALUES (?, ?, 'FINAL', 'WRITTEN', 'Cuối kỳ thêm', 90,
                          CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                ON CONFLICT DO NOTHING
                """,
                UUID.randomUUID(), bookId);

        assertThatThrownBy(() -> jdbcTemplate.update(
                        """
                        INSERT INTO grade_columns (
                            id, grade_book_id, assessment_kind, assessment_form, display_label,
                            display_order, created_at, updated_at
                        ) VALUES (?, ?, 'FINAL', 'WRITTEN', 'Cuối kỳ trùng', 91,
                                  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                        UUID.randomUUID(), bookId))
                .hasMessageContaining("ux_grade_columns_book_final");
    }

    @Test
    void refusesTwoBooksForTheSameClassSubjectAndTerm() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                        """
                        INSERT INTO grade_books (
                            id, class_id, subject_id, academic_term_id, version,
                            created_at, updated_at
                        )
                        SELECT ?, class_id, subject_id, academic_term_id, 0,
                               CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                        FROM grade_books LIMIT 1
                        """,
                        UUID.randomUUID()))
                .hasMessageContaining("uk_grade_books_slot");
    }

    @Test
    void refusesALockWithoutSayingWhoLockedIt() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                        "UPDATE grade_books SET locked_at = CURRENT_TIMESTAMP WHERE id = ?",
                        anyBookId()))
                .hasMessageContaining("ck_grade_books_locked");
    }

    @Test
    void refusesADecidedChangeRequestWithoutADecider() {
        UUID assessmentId = jdbcTemplate.queryForObject(
                "SELECT id FROM grade_assessments WHERE status = 'RECORDED' LIMIT 1", UUID.class);
        UUID requesterId = jdbcTemplate.queryForObject("SELECT id FROM users LIMIT 1", UUID.class);

        assertThatThrownBy(() -> jdbcTemplate.update(
                        """
                        INSERT INTO grade_change_requests (
                            id, grade_assessment_id, requested_by, new_score, reason,
                            status, decided_by, decided_at, created_at, updated_at
                        ) VALUES (?, ?, ?, 9.0, 'Nhập nhầm', 'APPROVED', NULL, NULL,
                                  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                        UUID.randomUUID(), assessmentId, requesterId))
                .hasMessageContaining("ck_grade_change_requests_decision");
    }

    @Test
    void refusesAChangeRequestThatProposesBothAScoreAndAnOutcome() {
        UUID assessmentId = jdbcTemplate.queryForObject(
                "SELECT id FROM grade_assessments WHERE status = 'RECORDED' LIMIT 1", UUID.class);
        UUID requesterId = jdbcTemplate.queryForObject("SELECT id FROM users LIMIT 1", UUID.class);

        assertThatThrownBy(() -> jdbcTemplate.update(
                        """
                        INSERT INTO grade_change_requests (
                            id, grade_assessment_id, requested_by, new_score, new_outcome,
                            reason, status, created_at, updated_at
                        ) VALUES (?, ?, ?, 9.0, 'ACHIEVED', 'Sai', 'PENDING',
                                  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                        UUID.randomUUID(), assessmentId, requesterId))
                .hasMessageContaining("ck_grade_change_requests_proposal");
    }
}
