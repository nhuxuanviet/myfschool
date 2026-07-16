package vn.edu.fpt.myschool.events.application;

import org.springframework.http.HttpStatus;

import vn.edu.fpt.myschool.shared.error.ApiException;

final class EventsException extends ApiException {

    private EventsException(HttpStatus status, String code, String message) {
        super(status, code, message);
    }

    static EventsException invalidAuthenticatedSubject() {
        return new EventsException(
                HttpStatus.UNAUTHORIZED,
                "UNAUTHORIZED",
                "Authentication token is invalid");
    }

    static EventsException studentProfileNotFound() {
        return new EventsException(
                HttpStatus.NOT_FOUND,
                "STUDENT_PROFILE_NOT_FOUND",
                "Student profile was not found");
    }

    static EventsException eventNotFound() {
        return new EventsException(
                HttpStatus.NOT_FOUND,
                "EVENT_NOT_FOUND",
                "The requested event is unavailable");
    }

    static EventsException alreadyRegistered() {
        return new EventsException(
                HttpStatus.CONFLICT,
                "EVENT_ALREADY_REGISTERED",
                "The student is already registered for this event");
    }

    static EventsException registrationClosed() {
        return new EventsException(
                HttpStatus.CONFLICT,
                "EVENT_REGISTRATION_CLOSED",
                "Registration is no longer available for this event");
    }

    static EventsException capacityReached() {
        return new EventsException(
                HttpStatus.CONFLICT,
                "EVENT_CAPACITY_REACHED",
                "This event has reached its registration capacity");
    }

    static EventsException activeRegistrationNotFound() {
        return new EventsException(
                HttpStatus.NOT_FOUND,
                "EVENT_REGISTRATION_NOT_FOUND",
                "An active registration was not found for this event");
    }

    static EventsException cancellationClosed() {
        return new EventsException(
                HttpStatus.CONFLICT,
                "EVENT_CANCELLATION_CLOSED",
                "Cancellation is no longer available for this event");
    }
}
