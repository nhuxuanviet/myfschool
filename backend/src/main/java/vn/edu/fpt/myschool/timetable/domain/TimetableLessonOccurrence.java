package vn.edu.fpt.myschool.timetable.domain;

import java.time.LocalDate;
import java.util.Objects;

public record TimetableLessonOccurrence(LocalDate date, TimetableLesson lesson) {

    public TimetableLessonOccurrence {
        Objects.requireNonNull(date, "date must not be null");
        Objects.requireNonNull(lesson, "lesson must not be null");
    }
}
