package vn.edu.fpt.myschool.grades.domain;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Student-specific subject configuration and all assessments in one term. */
public record GradeSubjectEnrollment(
        UUID id,
        String code,
        String name,
        AssessmentMode assessmentMode,
        Integer annualLessonCount,
        int displayOrder,
        List<GradeAssessment> assessments) {

    public GradeSubjectEnrollment {
        Objects.requireNonNull(id, "id must not be null");
        requireText(code, "code");
        requireText(name, "name");
        Objects.requireNonNull(assessmentMode, "assessmentMode must not be null");
        if (displayOrder <= 0) {
            throw new IllegalArgumentException("displayOrder must be positive");
        }
        validateAnnualLessonCount(assessmentMode, annualLessonCount);
        assessments = List.copyOf(Objects.requireNonNull(assessments, "assessments must not be null"));
        if (assessments.stream().anyMatch(assessment -> assessment.assessmentMode() != assessmentMode)) {
            throw new IllegalArgumentException("assessment mode must match its enrolled subject");
        }
    }

    private static void validateAnnualLessonCount(
            AssessmentMode assessmentMode,
            Integer annualLessonCount) {
        if (assessmentMode == AssessmentMode.NUMERIC) {
            if (annualLessonCount == null || annualLessonCount < 35) {
                throw new IllegalArgumentException(
                        "numeric subjects require annualLessonCount of at least 35");
            }
            return;
        }
        if (annualLessonCount != null) {
            throw new IllegalArgumentException("remark subjects must not have annualLessonCount");
        }
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
