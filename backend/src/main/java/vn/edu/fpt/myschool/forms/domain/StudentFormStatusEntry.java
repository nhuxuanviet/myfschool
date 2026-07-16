package vn.edu.fpt.myschool.forms.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record StudentFormStatusEntry(
        UUID id,
        StudentFormStatus status,
        Instant occurredAt,
        String note) {

    public StudentFormStatusEntry {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        if (note != null && note.isBlank()) {
            throw new IllegalArgumentException("note must be null or non-blank");
        }
    }
}
