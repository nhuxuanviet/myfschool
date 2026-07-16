package vn.edu.fpt.myschool.events.domain;

import java.util.Objects;
import java.util.UUID;

/** The authenticated student's registration state for a locked event. */
public record EventRegistration(UUID id, EventRegistrationStatus status) {

    public EventRegistration {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(status, "status must not be null");
        if (status == EventRegistrationStatus.NOT_REGISTERED) {
            throw new IllegalArgumentException("a persisted registration cannot be NOT_REGISTERED");
        }
    }
}
