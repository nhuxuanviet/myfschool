package vn.edu.fpt.myschool.grades.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.fpt.myschool.auth.application.port.StudentProfileStore;
import vn.edu.fpt.myschool.auth.domain.StudentProfile;
import vn.edu.fpt.myschool.grades.application.port.GradesStore;
import vn.edu.fpt.myschool.grades.domain.AssessmentKind;
import vn.edu.fpt.myschool.grades.domain.AssessmentMode;
import vn.edu.fpt.myschool.grades.domain.AssessmentOutcome;
import vn.edu.fpt.myschool.grades.domain.GradeAssessment;
import vn.edu.fpt.myschool.grades.domain.GradeSubjectEnrollment;
import vn.edu.fpt.myschool.grades.domain.GradeTerm;
import vn.edu.fpt.myschool.grades.domain.SemesterGradeSubject;
import vn.edu.fpt.myschool.grades.domain.SemesterGrades;
import vn.edu.fpt.myschool.grades.domain.TermResult;
import vn.edu.fpt.myschool.shared.time.SchoolTimeZone;

@Service
public class GradesService {

    private static final BigDecimal MIDTERM_WEIGHT = BigDecimal.valueOf(2);
    private static final BigDecimal FINAL_WEIGHT = BigDecimal.valueOf(3);
    private static final Comparator<GradeTerm> TERM_NEWEST_FIRST = Comparator
            .comparing(GradeTerm::startsOn, Comparator.reverseOrder())
            .thenComparing(GradeTerm::endsOn, Comparator.reverseOrder())
            .thenComparing(GradeTerm::academicYear, Comparator.reverseOrder())
            .thenComparing(GradeTerm::code, Comparator.reverseOrder())
            .thenComparing(GradeTerm::id, Comparator.reverseOrder());

    private final StudentProfileStore studentProfileStore;
    private final GradesStore gradesStore;
    private final Clock clock;

    public GradesService(
            StudentProfileStore studentProfileStore,
            GradesStore gradesStore,
            Clock clock) {
        this.studentProfileStore = studentProfileStore;
        this.gradesStore = gradesStore;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public SemesterGrades getSemesterGrades(String authenticatedUserId, UUID requestedTermId) {
        UUID userId = parseAuthenticatedUserId(authenticatedUserId);
        StudentProfile student = studentProfileStore.findByUserId(userId)
                .orElseThrow(GradesException::studentProfileNotFound);
        return getSemesterGradesOf(student.id(), requestedTermId);
    }

    /**
     * The same report card, for a student already established as the caller's to see.
     *
     * <p>Takes a student id because a guardian is not the student. The check that the caller may
     * see this student belongs to whoever calls this, and the publication rule stays inside the
     * one query both paths share rather than being restated per caller.
     */
    @Transactional(readOnly = true)
    public SemesterGrades getSemesterGradesOf(UUID studentId, UUID requestedTermId) {
        List<GradeTerm> availableTerms = gradesStore.findAvailableTerms(studentId).stream()
                .sorted(TERM_NEWEST_FIRST)
                .toList();
        GradeTerm selectedTerm = selectTerm(availableTerms, requestedTermId);
        List<SemesterGradeSubject> subjects = gradesStore
                .findSubjectEnrollments(studentId, selectedTerm.id())
                .stream()
                .sorted(Comparator
                        .comparingInt(GradeSubjectEnrollment::displayOrder)
                        .thenComparing(GradeSubjectEnrollment::code)
                        .thenComparing(GradeSubjectEnrollment::id))
                .map(this::calculateSubject)
                .toList();
        return new SemesterGrades(selectedTerm, availableTerms, subjects);
    }

    private GradeTerm selectTerm(List<GradeTerm> availableTerms, UUID requestedTermId) {
        if (availableTerms.isEmpty()) {
            throw GradesException.requestedTermNotFound();
        }
        if (requestedTermId != null) {
            return availableTerms.stream()
                    .filter(term -> term.id().equals(requestedTermId))
                    .findFirst()
                    .orElseThrow(GradesException::requestedTermNotFound);
        }
        LocalDate schoolDate = LocalDate.ofInstant(clock.instant(), SchoolTimeZone.ZONE);
        return availableTerms.stream()
                .filter(term -> term.includes(schoolDate))
                .findFirst()
                .orElse(availableTerms.getFirst());
    }

    private SemesterGradeSubject calculateSubject(GradeSubjectEnrollment enrollment) {
        List<GradeAssessment> assessments = enrollment.assessments().stream()
                .sorted(Comparator.comparingInt(GradeAssessment::displayOrder)
                        .thenComparing(GradeAssessment::id))
                .toList();
        int requiredRegularAssessments = requiredRegularAssessments(enrollment);
        if (enrollment.assessmentMode() == AssessmentMode.NUMERIC) {
            return new SemesterGradeSubject(
                    enrollment.code(),
                    enrollment.name(),
                    enrollment.assessmentMode(),
                    enrollment.annualLessonCount(),
                    requiredRegularAssessments,
                    calculateNumericAverage(assessments, requiredRegularAssessments),
                    null,
                    assessments);
        }
        return new SemesterGradeSubject(
                enrollment.code(),
                enrollment.name(),
                enrollment.assessmentMode(),
                null,
                requiredRegularAssessments,
                null,
                calculateRemarkResult(assessments, requiredRegularAssessments),
                assessments);
    }

    private static int requiredRegularAssessments(GradeSubjectEnrollment enrollment) {
        if (enrollment.assessmentMode() == AssessmentMode.REMARK) {
            return 2;
        }
        int annualLessonCount = enrollment.annualLessonCount();
        if (annualLessonCount == 35) {
            return 2;
        }
        if (annualLessonCount <= 70) {
            return 3;
        }
        return 4;
    }

    private static BigDecimal calculateNumericAverage(
            List<GradeAssessment> assessments,
            int requiredRegularAssessments) {
        AssessmentSet assessmentSet = AssessmentSet.from(assessments);
        if (!assessmentSet.isComplete(requiredRegularAssessments)) {
            return null;
        }
        BigDecimal regularTotal = assessmentSet.regularAssessments().stream()
                .map(GradeAssessment::score)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal weightedTotal = regularTotal
                .add(assessmentSet.midterm().score().multiply(MIDTERM_WEIGHT))
                .add(assessmentSet.finalAssessment().score().multiply(FINAL_WEIGHT));
        int divisor = assessmentSet.regularAssessments().size() + 5;
        return weightedTotal.divide(BigDecimal.valueOf(divisor), 1, RoundingMode.HALF_UP);
    }

    private static TermResult calculateRemarkResult(
            List<GradeAssessment> assessments,
            int requiredRegularAssessments) {
        AssessmentSet assessmentSet = AssessmentSet.from(assessments);
        if (!assessmentSet.isComplete(requiredRegularAssessments)) {
            return TermResult.PENDING;
        }
        return assessmentSet.all().stream()
                .map(GradeAssessment::outcome)
                .allMatch(AssessmentOutcome.ACHIEVED::equals)
                ? TermResult.ACHIEVED
                : TermResult.NOT_ACHIEVED;
    }

    private UUID parseAuthenticatedUserId(String authenticatedUserId) {
        if (authenticatedUserId == null) {
            throw GradesException.invalidAuthenticatedSubject();
        }
        try {
            return UUID.fromString(authenticatedUserId);
        } catch (IllegalArgumentException exception) {
            throw GradesException.invalidAuthenticatedSubject();
        }
    }

    private record AssessmentSet(
            List<GradeAssessment> regularAssessments,
            List<GradeAssessment> midterms,
            List<GradeAssessment> finals) {

        private static AssessmentSet from(List<GradeAssessment> assessments) {
            return new AssessmentSet(
                    assessments.stream()
                            .filter(assessment -> assessment.kind() == AssessmentKind.REGULAR)
                            .toList(),
                    assessments.stream()
                            .filter(assessment -> assessment.kind() == AssessmentKind.MIDTERM)
                            .toList(),
                    assessments.stream()
                            .filter(assessment -> assessment.kind() == AssessmentKind.FINAL)
                            .toList());
        }

        private boolean isComplete(int requiredRegularAssessments) {
            return regularAssessments.size() == requiredRegularAssessments
                    && regularAssessments.stream().allMatch(GradeAssessment::isFinalized)
                    && midterms.size() == 1
                    && midterms.getFirst().isFinalized()
                    && finals.size() == 1
                    && finals.getFirst().isFinalized();
        }

        private GradeAssessment midterm() {
            return midterms.getFirst();
        }

        private GradeAssessment finalAssessment() {
            return finals.getFirst();
        }

        private List<GradeAssessment> all() {
            return java.util.stream.Stream.of(regularAssessments, midterms, finals)
                    .flatMap(List::stream)
                    .toList();
        }
    }
}
