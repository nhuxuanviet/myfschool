package vn.edu.fpt.myschool.events.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Event configuration as visible to students after audience authorization. */
public record SchoolEvent(
        UUID id,
        EventCategory category,
        String title,
        String description,
        String location,
        Instant startsAt,
        Instant endsAt,
        EventAudience audience,
        Integer audienceGradeLevel,
        Integer capacity,
        Instant registrationDeadline,
        Instant cancellationDeadline,
        boolean registrationEnabled) {

    public SchoolEvent {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(category, "category must not be null");
        requireText(title, "title");
        requireText(description, "description");
        requireText(location, "location");
        Objects.requireNonNull(startsAt, "startsAt must not be null");
        Objects.requireNonNull(endsAt, "endsAt must not be null");
        Objects.requireNonNull(audience, "audience must not be null");
        if (!endsAt.isAfter(startsAt)) {
            throw new IllegalArgumentException("endsAt must be after startsAt");
        }
        validateAudience(audience, audienceGradeLevel);
        if (capacity != null && capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive when present");
        }
        validateDeadline(registrationDeadline, startsAt, "registrationDeadline");
        validateDeadline(cancellationDeadline, startsAt, "cancellationDeadline");
    }

    private static void validateAudience(EventAudience audience, Integer audienceGradeLevel) {
        if (audience == EventAudience.ALL && audienceGradeLevel != null) {
            throw new IllegalArgumentException("ALL audience must not have audienceGradeLevel");
        }
        if (audience == EventAudience.GRADE
                && (audienceGradeLevel == null
                || audienceGradeLevel < 6
                || audienceGradeLevel > 12)) {
            throw new IllegalArgumentException("GRADE audience requires a grade level between 6 and 12");
        }
    }

    private static void validateDeadline(Instant deadline, Instant startsAt, String name) {
        if (deadline != null && deadline.isAfter(startsAt)) {
            throw new IllegalArgumentException(name + " must not be after startsAt");
        }
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
