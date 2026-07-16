package vn.edu.fpt.myschool.grades.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/** A single regular, midterm, or final assessment for one enrolled subject. */
public record GradeAssessment(
        UUID id,
        AssessmentMode assessmentMode,
        AssessmentKind kind,
        AssessmentForm form,
        String displayLabel,
        Integer durationMinutes,
        AssessmentStatus status,
        BigDecimal score,
        AssessmentOutcome outcome,
        LocalDate assessedOn,
        int displayOrder) {

    private static final BigDecimal MINIMUM_SCORE = BigDecimal.ZERO;
    private static final BigDecimal MAXIMUM_SCORE = BigDecimal.TEN;

    public GradeAssessment {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(assessmentMode, "assessmentMode must not be null");
        Objects.requireNonNull(kind, "kind must not be null");
        Objects.requireNonNull(form, "form must not be null");
        Objects.requireNonNull(status, "status must not be null");
        requireText(displayLabel, "displayLabel");
        if (durationMinutes != null && (durationMinutes <= 0 || durationMinutes > 180)) {
            throw new IllegalArgumentException(
                    "durationMinutes must be between 1 and 180 when present");
        }
        if (displayOrder <= 0) {
            throw new IllegalArgumentException("displayOrder must be positive");
        }
        validateScore(score);
        validateResult(assessmentMode, status, score, outcome);
    }

    public boolean isFinalized() {
        return status.isFinalized();
    }

    private static void validateScore(BigDecimal score) {
        if (score == null) {
            return;
        }
        if (score.compareTo(MINIMUM_SCORE) < 0 || score.compareTo(MAXIMUM_SCORE) > 0) {
            throw new IllegalArgumentException("score must be between 0.0 and 10.0");
        }
        if (score.scale() > 1) {
            throw new IllegalArgumentException("score must have at most one decimal place");
        }
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }

    private static void validateResult(
            AssessmentMode assessmentMode,
            AssessmentStatus status,
            BigDecimal score,
            AssessmentOutcome outcome) {
        if (!status.isFinalized()) {
            if (score != null || outcome != null) {
                throw new IllegalArgumentException(
                        "non-finalized assessments must not contain a result");
            }
            return;
        }
        if (assessmentMode == AssessmentMode.NUMERIC) {
            if (score == null || outcome != null) {
                throw new IllegalArgumentException(
                        "finalized numeric assessments require a score only");
            }
            return;
        }
        if (score != null || outcome == null) {
            throw new IllegalArgumentException(
                    "finalized remark assessments require an outcome only");
        }
    }
}
