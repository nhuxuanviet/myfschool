package vn.edu.fpt.myschool.grades.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/** Calculated student-facing result for one subject in a semester. */
public record SemesterGradeSubject(
        String code,
        String name,
        AssessmentMode assessmentMode,
        Integer annualLessonCount,
        int requiredRegularAssessments,
        BigDecimal termAverage,
        TermResult termResult,
        List<GradeAssessment> assessments) {

    public SemesterGradeSubject {
        requireText(code, "code");
        requireText(name, "name");
        Objects.requireNonNull(assessmentMode, "assessmentMode must not be null");
        if (requiredRegularAssessments <= 0) {
            throw new IllegalArgumentException("requiredRegularAssessments must be positive");
        }
        assessments = List.copyOf(Objects.requireNonNull(assessments, "assessments must not be null"));
        if (assessments.stream().anyMatch(assessment -> assessment.assessmentMode() != assessmentMode)) {
            throw new IllegalArgumentException("assessment mode must match subject mode");
        }
        validateCalculatedResult(assessmentMode, annualLessonCount, termAverage, termResult);
    }

    private static void validateCalculatedResult(
            AssessmentMode assessmentMode,
            Integer annualLessonCount,
            BigDecimal termAverage,
            TermResult termResult) {
        if (assessmentMode == AssessmentMode.NUMERIC) {
            if (annualLessonCount == null || annualLessonCount < 35) {
                throw new IllegalArgumentException(
                        "numeric subjects require annualLessonCount of at least 35");
            }
            if (termResult != null) {
                throw new IllegalArgumentException("numeric subjects must not expose a termResult");
            }
            validateTermAverage(termAverage);
            return;
        }
        if (annualLessonCount != null || termAverage != null || termResult == null) {
            throw new IllegalArgumentException(
                    "remark subjects require a termResult and do not expose numeric values");
        }
    }

    private static void validateTermAverage(BigDecimal termAverage) {
        if (termAverage == null) {
            return;
        }
        if (termAverage.compareTo(BigDecimal.ZERO) < 0
                || termAverage.compareTo(BigDecimal.TEN) > 0
                || termAverage.scale() > 1) {
            throw new IllegalArgumentException(
                    "termAverage must be between 0.0 and 10.0 with at most one decimal place");
        }
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
