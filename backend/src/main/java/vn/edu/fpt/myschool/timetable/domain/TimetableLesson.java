package vn.edu.fpt.myschool.timetable.domain;

import java.time.Duration;
import java.time.LocalTime;
import java.util.Objects;

public record TimetableLesson(
        SchoolSession session,
        int periodNumber,
        LocalTime startTime,
        LocalTime endTime,
        TimetableSubject subject,
        String teacherName,
        String room,
        TimetableLessonStatus status,
        String note) {

    private static final Duration LESSON_DURATION = Duration.ofMinutes(45);

    public TimetableLesson {
        Objects.requireNonNull(session, "session must not be null");
        if (periodNumber < 1 || periodNumber > 5) {
            throw new IllegalArgumentException("periodNumber must be between 1 and 5");
        }
        Objects.requireNonNull(startTime, "startTime must not be null");
        Objects.requireNonNull(endTime, "endTime must not be null");
        if (!Duration.between(startTime, endTime).equals(LESSON_DURATION)) {
            throw new IllegalArgumentException("A lesson must last exactly 45 minutes");
        }
        Objects.requireNonNull(subject, "subject must not be null");
        teacherName = optionalText(teacherName, "teacherName");
        room = optionalText(room, "room");
        Objects.requireNonNull(status, "status must not be null");
        note = optionalText(note, "note");
    }

    private static String optionalText(String value, String name) {
        if (value != null && value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank when provided");
        }
        return value;
    }
}
