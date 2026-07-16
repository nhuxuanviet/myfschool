package vn.edu.fpt.myschool.home.domain;

import java.util.Objects;

public record HomeSummary(
        LessonSummary lessons,
        EventSummary events,
        FormSummary forms,
        ClubSummary clubs) {

    public HomeSummary {
        Objects.requireNonNull(lessons, "lessons must not be null");
        Objects.requireNonNull(events, "events must not be null");
        Objects.requireNonNull(forms, "forms must not be null");
        Objects.requireNonNull(clubs, "clubs must not be null");
    }

    public static HomeSummary empty() {
        return new HomeSummary(
                new LessonSummary(0),
                new EventSummary(0),
                new FormSummary(0),
                new ClubSummary(0));
    }

    public record LessonSummary(int today) {

        public LessonSummary {
            requireNonNegative(today, "today");
        }
    }

    public record EventSummary(int upcoming) {

        public EventSummary {
            requireNonNegative(upcoming, "upcoming");
        }
    }

    public record FormSummary(int pending) {

        public FormSummary {
            requireNonNegative(pending, "pending");
        }
    }

    public record ClubSummary(int active) {

        public ClubSummary {
            requireNonNegative(active, "active");
        }
    }

    private static void requireNonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must not be negative");
        }
    }
}
