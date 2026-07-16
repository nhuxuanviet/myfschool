package vn.edu.fpt.myschool.home.domain;

import java.time.LocalDate;
import java.util.Objects;

public record AcademicTerm(
        String academicYear,
        String code,
        String name,
        LocalDate startsOn,
        LocalDate endsOn) {

    public AcademicTerm {
        requireText(academicYear, "academicYear");
        requireText(code, "code");
        requireText(name, "name");
        Objects.requireNonNull(startsOn, "startsOn must not be null");
        Objects.requireNonNull(endsOn, "endsOn must not be null");
        if (!endsOn.isAfter(startsOn)) {
            throw new IllegalArgumentException("endsOn must be after startsOn");
        }
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
