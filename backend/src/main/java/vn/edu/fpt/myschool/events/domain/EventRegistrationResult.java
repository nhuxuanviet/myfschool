package vn.edu.fpt.myschool.events.domain;

import java.util.Objects;

/** Distinguishes a new registration from a permitted reactivation. */
public record EventRegistrationResult(EventDetails event, boolean created) {

    public EventRegistrationResult {
        Objects.requireNonNull(event, "event must not be null");
    }
}
