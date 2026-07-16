package vn.edu.fpt.myschool.grades.domain;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record GradeTerm(
        UUID id,
        String academicYear,
        String code,
        String name,
        LocalDate startsOn,
        LocalDate endsOn) {

    public GradeTerm {
        Objects.requireNonNull(id, "id must not be null");
        requireText(academicYear, "academicYear");
        requireText(code, "code");
        requireText(name, "name");
        Objects.requireNonNull(startsOn, "startsOn must not be null");
        Objects.requireNonNull(endsOn, "endsOn must not be null");
        if (!endsOn.isAfter(startsOn)) {
            throw new IllegalArgumentException("endsOn must be after startsOn");
        }
    }

    public boolean includes(LocalDate date) {
        Objects.requireNonNull(date, "date must not be null");
        return !date.isBefore(startsOn) && !date.isAfter(endsOn);
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
