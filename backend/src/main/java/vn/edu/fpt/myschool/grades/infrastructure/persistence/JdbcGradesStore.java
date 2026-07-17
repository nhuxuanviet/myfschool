package vn.edu.fpt.myschool.grades.infrastructure.persistence;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import vn.edu.fpt.myschool.grades.application.port.GradesStore;
import vn.edu.fpt.myschool.grades.domain.AssessmentForm;
import vn.edu.fpt.myschool.grades.domain.AssessmentKind;
import vn.edu.fpt.myschool.grades.domain.AssessmentMode;
import vn.edu.fpt.myschool.grades.domain.AssessmentOutcome;
import vn.edu.fpt.myschool.grades.domain.AssessmentStatus;
import vn.edu.fpt.myschool.grades.domain.GradeAssessment;
import vn.edu.fpt.myschool.grades.domain.GradeSubjectEnrollment;
import vn.edu.fpt.myschool.grades.domain.GradeTerm;

@Repository
class JdbcGradesStore implements GradesStore {

    private static final String AVAILABLE_TERMS_SQL = """
            SELECT DISTINCT academic_terms.id,
                   academic_years.code AS academic_year,
                   academic_terms.code,
                   academic_terms.name,
                   academic_terms.starts_on,
                   academic_terms.ends_on
            FROM student_term_subjects enrollment
            INNER JOIN academic_terms ON academic_terms.id = enrollment.academic_term_id
            INNER JOIN academic_years ON academic_years.id = academic_terms.academic_year_id
            WHERE enrollment.student_id = :studentId
            ORDER BY academic_terms.starts_on DESC,
                     academic_terms.ends_on DESC,
                     academic_years.code DESC,
                     academic_terms.code DESC,
                     academic_terms.id DESC
            """;

    private static final String SUBJECT_ENROLLMENTS_SQL = """
            SELECT enrollment.id AS enrollment_id,
                   subject.code AS subject_code,
                   subject.name AS subject_name,
                   enrollment.assessment_mode,
                   enrollment.annual_lesson_count,
                   enrollment.display_order AS subject_display_order,
                   assessment.id AS assessment_id,
                   assessment.assessment_kind,
                   assessment.assessment_form,
                   assessment.display_label,
                   assessment.duration_minutes,
                   assessment.status,
                   assessment.score,
                   assessment.outcome,
                   assessment.assessed_on,
                   assessment.display_order AS assessment_display_order
            FROM student_term_subjects enrollment
            INNER JOIN subjects subject ON subject.id = enrollment.subject_id
            -- A mark reaches the student only once its grade book has been published, and
            -- only once it actually holds a value. PENDING is the teacher's own "not entered
            -- yet"; there is nothing to show and it is not the student's business.
            --
            -- Both conditions belong in the join, not a WHERE clause: a subject whose book is
            -- still unpublished must keep its row and simply carry no marks, rather than
            -- vanish from the report card.
            LEFT JOIN grade_assessments assessment
                ON assessment.student_term_subject_id = enrollment.id
                AND assessment.status <> 'PENDING'
                AND EXISTS (
                    SELECT 1
                    FROM grade_columns grade_column
                    INNER JOIN grade_books book ON book.id = grade_column.grade_book_id
                    WHERE grade_column.id = assessment.grade_column_id
                      AND book.published_at IS NOT NULL
                )
            WHERE enrollment.student_id = :studentId
              AND enrollment.academic_term_id = :academicTermId
            ORDER BY enrollment.display_order ASC,
                     subject.code ASC,
                     enrollment.id ASC,
                     assessment.display_order ASC,
                     assessment.id ASC
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    JdbcGradesStore(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<GradeTerm> findAvailableTerms(UUID studentId) {
        return jdbcTemplate.query(
                AVAILABLE_TERMS_SQL,
                new MapSqlParameterSource().addValue("studentId", studentId),
                this::mapTerm);
    }

    @Override
    public List<GradeSubjectEnrollment> findSubjectEnrollments(
            UUID studentId,
            UUID academicTermId) {
        return jdbcTemplate.query(
                SUBJECT_ENROLLMENTS_SQL,
                new MapSqlParameterSource()
                        .addValue("studentId", studentId)
                        .addValue("academicTermId", academicTermId),
                this::mapSubjectEnrollments);
    }

    private GradeTerm mapTerm(ResultSet resultSet, int rowNumber) throws SQLException {
        return new GradeTerm(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("academic_year"),
                resultSet.getString("code"),
                resultSet.getString("name"),
                resultSet.getObject("starts_on", LocalDate.class),
                resultSet.getObject("ends_on", LocalDate.class));
    }

    private List<GradeSubjectEnrollment> mapSubjectEnrollments(ResultSet resultSet)
            throws SQLException {
        Map<UUID, EnrollmentAccumulator> enrollments = new LinkedHashMap<>();
        while (resultSet.next()) {
            UUID enrollmentId = resultSet.getObject("enrollment_id", UUID.class);
            EnrollmentAccumulator enrollment = enrollments.get(enrollmentId);
            if (enrollment == null) {
                enrollment = mapEnrollment(resultSet, enrollmentId);
                enrollments.put(enrollmentId, enrollment);
            }
            UUID assessmentId = resultSet.getObject("assessment_id", UUID.class);
            if (assessmentId != null) {
                enrollment.assessments().add(mapAssessment(resultSet, assessmentId, enrollment.mode()));
            }
        }
        return enrollments.values().stream().map(EnrollmentAccumulator::toDomain).toList();
    }

    private EnrollmentAccumulator mapEnrollment(ResultSet resultSet, UUID enrollmentId)
            throws SQLException {
        return new EnrollmentAccumulator(
                enrollmentId,
                resultSet.getString("subject_code"),
                resultSet.getString("subject_name"),
                AssessmentMode.valueOf(resultSet.getString("assessment_mode")),
                resultSet.getObject("annual_lesson_count", Integer.class),
                resultSet.getInt("subject_display_order"),
                new ArrayList<>());
    }

    private GradeAssessment mapAssessment(
            ResultSet resultSet,
            UUID assessmentId,
            AssessmentMode assessmentMode) throws SQLException {
        return new GradeAssessment(
                assessmentId,
                assessmentMode,
                AssessmentKind.valueOf(resultSet.getString("assessment_kind")),
                AssessmentForm.valueOf(resultSet.getString("assessment_form")),
                resultSet.getString("display_label"),
                resultSet.getObject("duration_minutes", Integer.class),
                AssessmentStatus.valueOf(resultSet.getString("status")),
                resultSet.getObject("score", BigDecimal.class),
                mapOutcome(resultSet),
                resultSet.getObject("assessed_on", LocalDate.class),
                resultSet.getInt("assessment_display_order"));
    }

    private AssessmentOutcome mapOutcome(ResultSet resultSet) throws SQLException {
        String outcome = resultSet.getString("outcome");
        return outcome == null ? null : AssessmentOutcome.valueOf(outcome);
    }

    private record EnrollmentAccumulator(
            UUID id,
            String code,
            String name,
            AssessmentMode mode,
            Integer annualLessonCount,
            int displayOrder,
            List<GradeAssessment> assessments) {

        private GradeSubjectEnrollment toDomain() {
            return new GradeSubjectEnrollment(
                    id,
                    code,
                    name,
                    mode,
                    annualLessonCount,
                    displayOrder,
                    assessments);
        }
    }
}
