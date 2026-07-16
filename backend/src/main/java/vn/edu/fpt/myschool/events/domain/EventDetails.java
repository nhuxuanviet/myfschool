package vn.edu.fpt.myschool.events.domain;

import java.util.Objects;

/** Student-facing event data and the permitted actions at the current instant. */
public record EventDetails(
        SchoolEvent event,
        int registeredCount,
        EventRegistrationStatus registrationStatus,
        boolean canRegister,
        boolean canCancel) {

    public EventDetails {
        Objects.requireNonNull(event, "event must not be null");
        Objects.requireNonNull(registrationStatus, "registrationStatus must not be null");
        if (registeredCount < 0) {
            throw new IllegalArgumentException("registeredCount must not be negative");
        }
    }
}
