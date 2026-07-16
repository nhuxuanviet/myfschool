package vn.edu.fpt.myschool.home.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record HomeAnnouncement(UUID id, String title, String body, Instant publishedAt) {

    public HomeAnnouncement {
        Objects.requireNonNull(id, "id must not be null");
        requireText(title, "title");
        requireText(body, "body");
        Objects.requireNonNull(publishedAt, "publishedAt must not be null");
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
