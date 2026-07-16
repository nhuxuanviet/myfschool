package vn.edu.fpt.myschool.grades.domain;

import java.util.List;
import java.util.Objects;

public record SemesterGrades(
        GradeTerm selectedTerm,
        List<GradeTerm> availableTerms,
        List<SemesterGradeSubject> subjects) {

    public SemesterGrades {
        Objects.requireNonNull(selectedTerm, "selectedTerm must not be null");
        availableTerms = List.copyOf(Objects.requireNonNull(
                availableTerms, "availableTerms must not be null"));
        subjects = List.copyOf(Objects.requireNonNull(subjects, "subjects must not be null"));
        if (availableTerms.stream().noneMatch(term -> term.id().equals(selectedTerm.id()))) {
            throw new IllegalArgumentException("selectedTerm must be included in availableTerms");
        }
    }
}
