package vn.edu.fpt.myschool.timetable.domain;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public record TimetableDay(LocalDate date, DayOfWeek dayOfWeek, List<TimetableLesson> lessons) {

    public TimetableDay {
        Objects.requireNonNull(date, "date must not be null");
        Objects.requireNonNull(dayOfWeek, "dayOfWeek must not be null");
        if (date.getDayOfWeek() != dayOfWeek) {
            throw new IllegalArgumentException("dayOfWeek must match date");
        }
        lessons = List.copyOf(Objects.requireNonNull(lessons, "lessons must not be null"));
    }
}
