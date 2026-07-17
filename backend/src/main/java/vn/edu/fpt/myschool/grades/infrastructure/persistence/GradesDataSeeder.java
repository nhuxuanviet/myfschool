package vn.edu.fpt.myschool.grades.infrastructure.persistence;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.fpt.myschool.shared.time.SchoolTimeZone;

/**
 * Stable, student-owned grade examples for local development and browser E2E.
 * It runs after auth, academic-term, and subject seeders so every foreign key
 * is available before grade enrollment is inserted.
 */
@Component
@Profile("(dev | e2e) & !prod")
@Order(400)
class GradesDataSeeder implements ApplicationRunner {

    private static final UUID SEEDED_STUDENT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000201");
    private static final UUID CURRENT_ACADEMIC_TERM_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000302");
    private static final UUID HISTORICAL_ACADEMIC_TERM_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000304");

    private static final UUID MATHEMATICS_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000601");
    private static final UUID LITERATURE_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000602");
    private static final UUID PHYSICAL_EDUCATION_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000610");

    private static final UUID CURRENT_MATHEMATICS_ENROLLMENT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000001001");
    private static final UUID CURRENT_LITERATURE_ENROLLMENT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000001002");
    private static final UUID CURRENT_PHYSICAL_EDUCATION_ENROLLMENT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000001003");
    private static final UUID HISTORICAL_MATHEMATICS_ENROLLMENT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000001004");
    private static final UUID HISTORICAL_PHYSICAL_EDUCATION_ENROLLMENT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000001005");

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    GradesDataSeeder(JdbcTemplate jdbcTemplate, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        Instant now = clock.instant();
        LocalDate schoolDate = LocalDate.ofInstant(now, SchoolTimeZone.ZONE);
        seedEnrollments(now);
        seedCurrentTermAssessments(schoolDate, now);
        seedHistoricalTermAssessments(schoolDate, now);
    }

    private void seedEnrollments(Instant now) {
        insertEnrollment(
                CURRENT_MATHEMATICS_ENROLLMENT_ID,
                CURRENT_ACADEMIC_TERM_ID,
                MATHEMATICS_ID,
                "NUMERIC",
                105,
                1,
                now);
        insertEnrollment(
                CURRENT_LITERATURE_ENROLLMENT_ID,
                CURRENT_ACADEMIC_TERM_ID,
                LITERATURE_ID,
                "NUMERIC",
                105,
                2,
                now);
        insertEnrollment(
                CURRENT_PHYSICAL_EDUCATION_ENROLLMENT_ID,
                CURRENT_ACADEMIC_TERM_ID,
                PHYSICAL_EDUCATION_ID,
                "REMARK",
                null,
                3,
                now);
        insertEnrollment(
                HISTORICAL_MATHEMATICS_ENROLLMENT_ID,
                HISTORICAL_ACADEMIC_TERM_ID,
                MATHEMATICS_ID,
                "NUMERIC",
                105,
                1,
                now);
        insertEnrollment(
                HISTORICAL_PHYSICAL_EDUCATION_ENROLLMENT_ID,
                HISTORICAL_ACADEMIC_TERM_ID,
                PHYSICAL_EDUCATION_ID,
                "REMARK",
                null,
                2,
                now);
    }

    private void seedCurrentTermAssessments(LocalDate schoolDate, Instant now) {
        LocalDate termStart = schoolDate.minusDays(30);
        insertNumericAssessment(1101, CURRENT_MATHEMATICS_ENROLLMENT_ID, "REGULAR", "ORAL",
                "Miệng", null, "RECORDED", "9.0", termStart.plusDays(3), 1, now);
        insertNumericAssessment(1102, CURRENT_MATHEMATICS_ENROLLMENT_ID, "REGULAR", "WRITTEN",
                "15 phút", 15, "RECORDED", "8.5", termStart.plusDays(8), 2, now);
        insertNumericAssessment(1103, CURRENT_MATHEMATICS_ENROLLMENT_ID, "REGULAR", "WRITTEN",
                "45 phút", 45, "RECORDED", "9.0", termStart.plusDays(13), 3, now);
        insertNumericAssessment(1104, CURRENT_MATHEMATICS_ENROLLMENT_ID, "REGULAR", "PRESENTATION",
                "Thuyết trình", null, "RECORDED", "8.0", termStart.plusDays(18), 4, now);
        insertNumericAssessment(1105, CURRENT_MATHEMATICS_ENROLLMENT_ID, "MIDTERM", "WRITTEN",
                "Kiểm tra giữa kỳ", 45, "RECORDED", "8.5", termStart.plusDays(22), 5, now);
        insertNumericAssessment(1106, CURRENT_MATHEMATICS_ENROLLMENT_ID, "FINAL", "WRITTEN",
                "Kiểm tra cuối kỳ", 60, "RECORDED", "9.0", termStart.plusDays(27), 6, now);

        insertNumericAssessment(1111, CURRENT_LITERATURE_ENROLLMENT_ID, "REGULAR", "ORAL",
                "Miệng", null, "RECORDED", "7.0", termStart.plusDays(3), 1, now);
        insertNumericAssessment(1112, CURRENT_LITERATURE_ENROLLMENT_ID, "REGULAR", "WRITTEN",
                "15 phút", 15, "RECORDED", "8.0", termStart.plusDays(8), 2, now);
        insertNumericAssessment(1113, CURRENT_LITERATURE_ENROLLMENT_ID, "REGULAR", "WRITTEN",
                "45 phút", 45, "MAKE_UP_REQUIRED", null, null, 3, now);
        insertNumericAssessment(1114, CURRENT_LITERATURE_ENROLLMENT_ID, "REGULAR", "PRESENTATION",
                "Thuyết trình", null, "RECORDED", "8.0", termStart.plusDays(18), 4, now);
        insertNumericAssessment(1115, CURRENT_LITERATURE_ENROLLMENT_ID, "MIDTERM", "WRITTEN",
                "Kiểm tra giữa kỳ", 45, "RECORDED", "7.5", termStart.plusDays(22), 5, now);
        insertNumericAssessment(1116, CURRENT_LITERATURE_ENROLLMENT_ID, "FINAL", "WRITTEN",
                "Kiểm tra cuối kỳ", 60, "MAKE_UP_REQUIRED", null, null, 6, now);

        insertRemarkAssessment(1121, CURRENT_PHYSICAL_EDUCATION_ENROLLMENT_ID, "REGULAR", "PRACTICAL",
                "Thực hành kỹ thuật", null, "RECORDED", "ACHIEVED", termStart.plusDays(4), 1, now);
        insertRemarkAssessment(1122, CURRENT_PHYSICAL_EDUCATION_ENROLLMENT_ID, "REGULAR", "PRODUCT",
                "Sản phẩm học tập", null, "RECORDED", "ACHIEVED", termStart.plusDays(12), 2, now);
        insertRemarkAssessment(1123, CURRENT_PHYSICAL_EDUCATION_ENROLLMENT_ID, "MIDTERM", "PRACTICAL",
                "Đánh giá giữa kỳ", null, "RECORDED", "ACHIEVED", termStart.plusDays(20), 3, now);
        insertRemarkAssessment(1124, CURRENT_PHYSICAL_EDUCATION_ENROLLMENT_ID, "FINAL", "PROJECT",
                "Dự án cuối kỳ", null, "RECORDED", "ACHIEVED", termStart.plusDays(26), 4, now);
    }

    private void seedHistoricalTermAssessments(LocalDate schoolDate, Instant now) {
        LocalDate historicalTermEndsOn = schoolDate.minusMonths(6).minusDays(1);
        LocalDate firstAssessmentDate = historicalTermEndsOn.minusDays(27);
        insertNumericAssessment(1131, HISTORICAL_MATHEMATICS_ENROLLMENT_ID, "REGULAR", "ORAL",
                "Miệng", null, "RECORDED", "7.5", firstAssessmentDate, 1, now);
        insertNumericAssessment(1132, HISTORICAL_MATHEMATICS_ENROLLMENT_ID, "REGULAR", "WRITTEN",
                "15 phút", 15, "RECORDED", "8.0", firstAssessmentDate.plusDays(5), 2, now);
        insertNumericAssessment(1133, HISTORICAL_MATHEMATICS_ENROLLMENT_ID, "REGULAR", "WRITTEN",
                "45 phút", 45, "RECORDED", "8.5", firstAssessmentDate.plusDays(10), 3, now);
        insertNumericAssessment(1134, HISTORICAL_MATHEMATICS_ENROLLMENT_ID, "REGULAR", "PRESENTATION",
                "Thuyết trình", null, "RECORDED", "8.0", firstAssessmentDate.plusDays(15), 4, now);
        insertNumericAssessment(1135, HISTORICAL_MATHEMATICS_ENROLLMENT_ID, "MIDTERM", "WRITTEN",
                "Kiểm tra giữa kỳ", 45, "RECORDED", "8.0", firstAssessmentDate.plusDays(20), 5, now);
        insertNumericAssessment(1136, HISTORICAL_MATHEMATICS_ENROLLMENT_ID, "FINAL", "WRITTEN",
                "Kiểm tra cuối kỳ", 60, "RECORDED", "8.5", historicalTermEndsOn, 6, now);

        insertRemarkAssessment(1141, HISTORICAL_PHYSICAL_EDUCATION_ENROLLMENT_ID, "REGULAR", "PRACTICAL",
                "Thực hành kỹ thuật", null, "RECORDED", "ACHIEVED", firstAssessmentDate.plusDays(1), 1, now);
        insertRemarkAssessment(1142, HISTORICAL_PHYSICAL_EDUCATION_ENROLLMENT_ID, "REGULAR", "PRODUCT",
                "Sản phẩm học tập", null, "RECORDED", "NOT_ACHIEVED", firstAssessmentDate.plusDays(8), 2, now);
        insertRemarkAssessment(1143, HISTORICAL_PHYSICAL_EDUCATION_ENROLLMENT_ID, "MIDTERM", "PRACTICAL",
                "Đánh giá giữa kỳ", null, "RECORDED", "ACHIEVED", firstAssessmentDate.plusDays(16), 3, now);
        insertRemarkAssessment(1144, HISTORICAL_PHYSICAL_EDUCATION_ENROLLMENT_ID, "FINAL", "PROJECT",
                "Dự án cuối kỳ", null, "RECORDED", "ACHIEVED", historicalTermEndsOn, 4, now);
    }

    private void insertEnrollment(
            UUID id,
            UUID academicTermId,
            UUID subjectId,
            String assessmentMode,
            Integer annualLessonCount,
            int displayOrder,
            Instant now) {
        jdbcTemplate.update("""
                INSERT INTO student_term_subjects (
                    id, student_id, academic_term_id, subject_id, assessment_mode,
                    annual_lesson_count, display_order, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO NOTHING
                """,
                id,
                SEEDED_STUDENT_ID,
                academicTermId,
                subjectId,
                assessmentMode,
                annualLessonCount,
                displayOrder,
                Timestamp.from(now),
                Timestamp.from(now));
    }

    private void insertNumericAssessment(
            int identifier,
            UUID enrollmentId,
            String kind,
            String form,
            String displayLabel,
            Integer durationMinutes,
            String status,
            String score,
            LocalDate assessedOn,
            int displayOrder,
            Instant now) {
        insertAssessment(
                identifier,
                enrollmentId,
                "NUMERIC",
                kind,
                form,
                displayLabel,
                durationMinutes,
                status,
                score == null ? null : new BigDecimal(score),
                null,
                assessedOn,
                displayOrder,
                now);
    }

    private void insertRemarkAssessment(
            int identifier,
            UUID enrollmentId,
            String kind,
            String form,
            String displayLabel,
            Integer durationMinutes,
            String status,
            String outcome,
            LocalDate assessedOn,
            int displayOrder,
            Instant now) {
        insertAssessment(
                identifier,
                enrollmentId,
                "REMARK",
                kind,
                form,
                displayLabel,
                durationMinutes,
                status,
                null,
                outcome,
                assessedOn,
                displayOrder,
                now);
    }

    private void insertAssessment(
            int identifier,
            UUID enrollmentId,
            String assessmentMode,
            String kind,
            String form,
            String displayLabel,
            Integer durationMinutes,
            String status,
            BigDecimal score,
            String outcome,
            LocalDate assessedOn,
            int displayOrder,
            Instant now) {
        jdbcTemplate.update("""
                INSERT INTO grade_assessments (
                    id, student_term_subject_id, grade_column_id, assessment_mode,
                    assessment_kind, assessment_form,
                    display_label, duration_minutes, status, score, outcome, assessed_on,
                    display_order, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO NOTHING
                """,
                seededIdentifier(identifier),
                enrollmentId,
                gradeColumnId(enrollmentId, kind, form, displayLabel, durationMinutes,
                        displayOrder, now),
                assessmentMode,
                kind,
                form,
                displayLabel,
                durationMinutes,
                status,
                score,
                outcome,
                assessedOn == null ? null : Date.valueOf(assessedOn),
                displayOrder,
                Timestamp.from(now),
                Timestamp.from(now));
    }

    /**
     * Resolves the grade column a seeded mark belongs to, creating the book and column on first
     * sight.
     *
     * <p>Ids are derived exactly as V26 derives them, so a database migrated from older data and a
     * freshly seeded one agree on which row is which book and which column.
     *
     * <p>Seeded books are published: the student app has always shown these marks, and introducing
     * the publish gate must not blank it.
     */
    private UUID gradeColumnId(
            UUID enrollmentId,
            String kind,
            String form,
            String displayLabel,
            Integer durationMinutes,
            int displayOrder,
            Instant now) {
        jdbcTemplate.update("""
                INSERT INTO grade_books (
                    id, class_id, subject_id, academic_term_id, published_at, version,
                    created_at, updated_at
                )
                SELECT md5('grade-book:' || student.class_id::text || ':'
                           || term_subject.subject_id::text || ':'
                           || term_subject.academic_term_id::text)::uuid,
                       student.class_id, term_subject.subject_id, term_subject.academic_term_id,
                       ?, 0, ?, ?
                FROM student_term_subjects term_subject
                INNER JOIN students student ON student.id = term_subject.student_id
                WHERE term_subject.id = ? AND student.class_id IS NOT NULL
                ON CONFLICT (class_id, subject_id, academic_term_id) DO NOTHING
                """,
                Timestamp.from(now), Timestamp.from(now), Timestamp.from(now), enrollmentId);
        UUID bookId = jdbcTemplate.queryForObject("""
                SELECT book.id
                FROM student_term_subjects term_subject
                INNER JOIN students student ON student.id = term_subject.student_id
                INNER JOIN grade_books book
                    ON book.class_id = student.class_id
                    AND book.subject_id = term_subject.subject_id
                    AND book.academic_term_id = term_subject.academic_term_id
                WHERE term_subject.id = ?
                """, UUID.class, enrollmentId);
        jdbcTemplate.update("""
                INSERT INTO grade_columns (
                    id, grade_book_id, assessment_kind, assessment_form, display_label,
                    duration_minutes, display_order, created_at, updated_at
                ) VALUES (
                    md5('grade-column:' || ?::text || ':' || ?::text)::uuid,
                    ?, ?, ?, ?, ?, ?, ?, ?
                )
                ON CONFLICT (grade_book_id, display_order) DO NOTHING
                """,
                bookId, displayOrder, bookId, kind, form, displayLabel, durationMinutes,
                displayOrder, Timestamp.from(now), Timestamp.from(now));
        return jdbcTemplate.queryForObject(
                "SELECT id FROM grade_columns WHERE grade_book_id = ? AND display_order = ?",
                UUID.class, bookId, displayOrder);
    }

    private static UUID seededIdentifier(int identifier) {
        return UUID.fromString("00000000-0000-0000-0000-%012d".formatted(identifier));
    }
}
