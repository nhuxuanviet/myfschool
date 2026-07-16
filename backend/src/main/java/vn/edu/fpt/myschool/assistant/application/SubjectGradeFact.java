package vn.edu.fpt.myschool.assistant.application;

import java.math.BigDecimal;
import java.util.Objects;

/** Structured grade data for model tool use; it deliberately contains no response wording. */
public record SubjectGradeFact(
        String termName,
        String academicYear,
        String subjectName,
        boolean found,
        boolean resultAvailable,
        BigDecimal termAverage,
        String qualitativeResult) {

    public SubjectGradeFact {
        requireText(termName, "termName");
        requireText(academicYear, "academicYear");
        requireText(subjectName, "subjectName");
        if (!found && resultAvailable) {
            throw new IllegalArgumentException("a missing subject cannot have an available result");
        }
        if (termAverage != null && qualitativeResult != null) {
            throw new IllegalArgumentException("a subject cannot have both numeric and qualitative results");
        }
        if (resultAvailable != (termAverage != null || qualitativeResult != null)) {
            throw new IllegalArgumentException("resultAvailable must match the supplied result");
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
