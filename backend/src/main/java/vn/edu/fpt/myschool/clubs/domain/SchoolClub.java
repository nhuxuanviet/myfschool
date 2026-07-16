package vn.edu.fpt.myschool.clubs.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record SchoolClub(
        UUID id,
        ClubCategory category,
        String name,
        String description,
        String advisorName,
        String meetingSchedule,
        String location,
        ClubAudience audience,
        Integer audienceGradeLevel,
        Integer capacity,
        Instant applicationDeadline,
        boolean acceptingApplications) {

    public SchoolClub {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(category, "category must not be null");
        Objects.requireNonNull(audience, "audience must not be null");
        requireNonBlank(name, "name");
        requireNonBlank(description, "description");
        requireNonBlank(advisorName, "advisorName");
        requireNonBlank(meetingSchedule, "meetingSchedule");
        requireNonBlank(location, "location");
        if (audience == ClubAudience.ALL && audienceGradeLevel != null) {
            throw new IllegalArgumentException("ALL clubs must not target one grade");
        }
        if (audience == ClubAudience.GRADE
                && (audienceGradeLevel == null || audienceGradeLevel < 6 || audienceGradeLevel > 12)) {
            throw new IllegalArgumentException("GRADE clubs require grade 6 through 12");
        }
        if (capacity != null && capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
    }

    private static void requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
