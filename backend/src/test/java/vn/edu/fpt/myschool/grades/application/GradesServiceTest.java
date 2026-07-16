package vn.edu.fpt.myschool.grades.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import vn.edu.fpt.myschool.auth.application.port.StudentProfileStore;
import vn.edu.fpt.myschool.auth.domain.StudentProfile;
import vn.edu.fpt.myschool.grades.application.port.GradesStore;
import vn.edu.fpt.myschool.grades.domain.AssessmentForm;
import vn.edu.fpt.myschool.grades.domain.AssessmentKind;
import vn.edu.fpt.myschool.grades.domain.AssessmentMode;
import vn.edu.fpt.myschool.grades.domain.AssessmentOutcome;
import vn.edu.fpt.myschool.grades.domain.AssessmentStatus;
import vn.edu.fpt.myschool.grades.domain.GradeAssessment;
import vn.edu.fpt.myschool.grades.domain.GradeSubjectEnrollment;
import vn.edu.fpt.myschool.grades.domain.GradeTerm;
import vn.edu.fpt.myschool.grades.domain.SemesterGrades;
import vn.edu.fpt.myschool.grades.domain.TermResult;

class GradesServiceTest {

    private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID STUDENT_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

    @Test
    void selectsAnActiveTermUsingTheVietnameseSchoolDateAtAUtcBoundary() {
        GradeTerm historical = term(1, "HK2", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30));
        GradeTerm active = term(2, "HK1", LocalDate.of(2026, 7, 13), LocalDate.of(2026, 12, 31));
        Clock utcClockAtSundayEvening = Clock.fixed(
                Instant.parse("2026-07-12T18:30:00Z"), ZoneOffset.UTC);
        GradesService service = service(
                List.of(historical, active),
                Map.of(active.id(), List.of()),
                utcClockAtSundayEvening);

        SemesterGrades grades = service.getSemesterGrades(USER_ID.toString(), null);

        // 18:30 Sunday UTC is 01:30 Monday in Asia/Ho_Chi_Minh.
        assertThat(grades.selectedTerm().id()).isEqualTo(active.id());
        assertThat(grades.availableTerms()).extracting(GradeTerm::id)
                .containsExactly(active.id(), historical.id());
    }

    @Test
    void calculatesTheCircularTwentyTwoWeightedAverageWithHalfUpRounding() {
        GradeTerm term = term(1, "HK1", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30));
        GradeSubjectEnrollment mathematics = numericEnrollment(
                1,
                List.of(
                        numericAssessment(1, AssessmentKind.REGULAR, "9.0", 1),
                        numericAssessment(2, AssessmentKind.REGULAR, "8.5", 2),
                        numericAssessment(3, AssessmentKind.REGULAR, "9.0", 3),
                        numericAssessment(4, AssessmentKind.REGULAR, "8.0", 4),
                        numericAssessment(5, AssessmentKind.MIDTERM, "8.5", 5),
                        numericAssessment(6, AssessmentKind.FINAL, "9.0", 6)));
        GradesService service = service(List.of(term), Map.of(term.id(), List.of(mathematics)), Clock.systemUTC());

        SemesterGrades grades = service.getSemesterGrades(USER_ID.toString(), term.id());

        assertThat(grades.subjects().getFirst().termAverage())
                .isEqualByComparingTo(new BigDecimal("8.7"));
        assertThat(grades.subjects().getFirst().termResult()).isNull();
    }

    @Test
    void roundsAThirtySixLessonSubjectAverageAtTheHalfUpTie() {
        GradeTerm term = term(1, "HK1", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30));
        GradeSubjectEnrollment subject = numericEnrollment(
                1,
                36,
                1,
                List.of(
                        numericAssessment(1, AssessmentKind.REGULAR, "8.0", 1),
                        numericAssessment(2, AssessmentKind.REGULAR, "8.0", 2),
                        numericAssessment(3, AssessmentKind.REGULAR, "8.5", 3),
                        numericAssessment(4, AssessmentKind.MIDTERM, "8.0", 4),
                        numericAssessment(5, AssessmentKind.FINAL, "8.5", 5)));
        GradesService service = service(List.of(term), Map.of(term.id(), List.of(subject)), Clock.systemUTC());

        SemesterGrades grades = service.getSemesterGrades(USER_ID.toString(), term.id());

        assertThat(grades.subjects().getFirst().requiredRegularAssessments()).isEqualTo(3);
        assertThat(grades.subjects().getFirst().termAverage())
                .isEqualByComparingTo(new BigDecimal("8.3"));
    }

    @Test
    void derivesRequiredRegularAssessmentsAtEveryCircularTwentyTwoLessonThreshold() {
        GradeTerm term = term(1, "HK1", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30));
        GradesService service = service(
                List.of(term),
                Map.of(term.id(), List.of(
                        numericEnrollment(1, 35, 1, List.of()),
                        numericEnrollment(2, 36, 2, List.of()),
                        numericEnrollment(3, 70, 3, List.of()),
                        numericEnrollment(4, 71, 4, List.of()))),
                Clock.systemUTC());

        SemesterGrades grades = service.getSemesterGrades(USER_ID.toString(), term.id());

        assertThat(grades.subjects())
                .extracting(subject -> subject.annualLessonCount()
                        + ":" + subject.requiredRegularAssessments())
                .containsExactly("35:2", "36:3", "70:3", "71:4");
    }

    @Test
    void keepsANumericAveragePendingWhenThereAreMoreRegularAssessmentsThanRequired() {
        GradeTerm term = term(1, "HK1", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30));
        GradeSubjectEnrollment mathematics = numericEnrollment(
                1,
                List.of(
                        numericAssessment(1, AssessmentKind.REGULAR, "8.0", 1),
                        numericAssessment(2, AssessmentKind.REGULAR, "8.0", 2),
                        numericAssessment(3, AssessmentKind.REGULAR, "8.0", 3),
                        numericAssessment(4, AssessmentKind.REGULAR, "8.0", 4),
                        numericAssessment(5, AssessmentKind.REGULAR, "8.0", 5),
                        numericAssessment(6, AssessmentKind.MIDTERM, "8.0", 6),
                        numericAssessment(7, AssessmentKind.FINAL, "8.0", 7)));
        GradesService service = service(List.of(term), Map.of(term.id(), List.of(mathematics)), Clock.systemUTC());

        SemesterGrades grades = service.getSemesterGrades(USER_ID.toString(), term.id());

        assertThat(grades.subjects().getFirst().termAverage()).isNull();
    }

    @Test
    void keepsARemarkResultPendingWhenThereAreMoreRegularAssessmentsThanRequired() {
        GradeTerm term = term(1, "HK1", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30));
        GradeSubjectEnrollment physicalEducation = new GradeSubjectEnrollment(
                identifier(10),
                "GIAO_DUC_THE_CHAT",
                "Giáo dục thể chất",
                AssessmentMode.REMARK,
                null,
                1,
                List.of(
                        remarkAssessment(1, AssessmentKind.REGULAR, AssessmentOutcome.ACHIEVED, 1),
                        remarkAssessment(2, AssessmentKind.REGULAR, AssessmentOutcome.ACHIEVED, 2),
                        remarkAssessment(3, AssessmentKind.REGULAR, AssessmentOutcome.ACHIEVED, 3),
                        remarkAssessment(4, AssessmentKind.MIDTERM, AssessmentOutcome.ACHIEVED, 4),
                        remarkAssessment(5, AssessmentKind.FINAL, AssessmentOutcome.ACHIEVED, 5)));
        GradesService service = service(
                List.of(term),
                Map.of(term.id(), List.of(physicalEducation)),
                Clock.systemUTC());

        SemesterGrades grades = service.getSemesterGrades(USER_ID.toString(), term.id());

        assertThat(grades.subjects().getFirst().termResult()).isEqualTo(TermResult.PENDING);
    }

    private static GradesService service(
            List<GradeTerm> terms,
            Map<UUID, List<GradeSubjectEnrollment>> subjectsByTerm,
            Clock clock) {
        StudentProfileStore students = ignored -> Optional.of(new StudentProfile(
                STUDENT_ID,
                "SE1913001",
                "Nguyễn Minh Anh",
                10,
                "10A1"));
        GradesStore gradesStore = new GradesStore() {
            @Override
            public List<GradeTerm> findAvailableTerms(UUID studentId) {
                return terms;
            }

            @Override
            public List<GradeSubjectEnrollment> findSubjectEnrollments(
                    UUID studentId,
                    UUID academicTermId) {
                return subjectsByTerm.getOrDefault(academicTermId, List.of());
            }
        };
        return new GradesService(students, gradesStore, clock);
    }

    private static GradeTerm term(int identifier, String code, LocalDate startsOn, LocalDate endsOn) {
        return new GradeTerm(
                identifier(identifier),
                "2025-2026",
                code,
                "Học kỳ " + code.substring(2),
                startsOn,
                endsOn);
    }

    private static GradeSubjectEnrollment numericEnrollment(
            int identifier,
            List<GradeAssessment> assessments) {
        return numericEnrollment(identifier, 105, 1, assessments);
    }

    private static GradeSubjectEnrollment numericEnrollment(
            int identifier,
            int annualLessonCount,
            int displayOrder,
            List<GradeAssessment> assessments) {
        return new GradeSubjectEnrollment(
                identifier(identifier + 100),
                "TOAN_" + identifier,
                "Toán",
                AssessmentMode.NUMERIC,
                annualLessonCount,
                displayOrder,
                assessments);
    }

    private static GradeAssessment numericAssessment(
            int identifier,
            AssessmentKind kind,
            String score,
            int displayOrder) {
        return new GradeAssessment(
                identifier(identifier + 1_000),
                AssessmentMode.NUMERIC,
                kind,
                AssessmentForm.WRITTEN,
                "Bài đánh giá " + identifier,
                45,
                AssessmentStatus.RECORDED,
                new BigDecimal(score),
                null,
                LocalDate.of(2026, 1, displayOrder),
                displayOrder);
    }

    private static GradeAssessment remarkAssessment(
            int identifier,
            AssessmentKind kind,
            AssessmentOutcome outcome,
            int displayOrder) {
        return new GradeAssessment(
                identifier(identifier + 2_000),
                AssessmentMode.REMARK,
                kind,
                AssessmentForm.PRACTICAL,
                "Đánh giá " + identifier,
                null,
                AssessmentStatus.RECORDED,
                null,
                outcome,
                LocalDate.of(2026, 1, displayOrder),
                displayOrder);
    }

    private static UUID identifier(int value) {
        return new UUID(0, value);
    }
}
