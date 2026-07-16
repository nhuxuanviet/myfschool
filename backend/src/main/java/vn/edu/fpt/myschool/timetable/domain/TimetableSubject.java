package vn.edu.fpt.myschool.timetable.domain;

import java.util.Objects;

public record TimetableSubject(String code, String name) {

    public TimetableSubject {
        code = requireText(code, "code");
        name = requireText(name, "name");
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
