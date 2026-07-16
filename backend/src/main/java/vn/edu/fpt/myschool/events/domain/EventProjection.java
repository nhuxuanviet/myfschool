package vn.edu.fpt.myschool.events.domain;

import java.util.Objects;

/** Read-model data before time-sensitive action flags are calculated. */
public record EventProjection(
        SchoolEvent event,
        int registeredCount,
        EventRegistrationStatus registrationStatus) {

    public EventProjection {
        Objects.requireNonNull(event, "event must not be null");
        Objects.requireNonNull(registrationStatus, "registrationStatus must not be null");
        if (registeredCount < 0) {
            throw new IllegalArgumentException("registeredCount must not be negative");
        }
    }
}
