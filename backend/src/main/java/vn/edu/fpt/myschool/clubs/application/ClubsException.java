package vn.edu.fpt.myschool.clubs.application;

import org.springframework.http.HttpStatus;

import vn.edu.fpt.myschool.shared.error.ApiException;

final class ClubsException extends ApiException {
    private ClubsException(HttpStatus status, String code, String message) {
        super(status, code, message);
    }

    static ClubsException invalidAuthenticatedSubject() {
        return new ClubsException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication token is invalid");
    }

    static ClubsException studentNotFound() {
        return new ClubsException(HttpStatus.NOT_FOUND, "STUDENT_PROFILE_NOT_FOUND", "Student profile was not found");
    }

    static ClubsException clubNotFound() {
        return new ClubsException(HttpStatus.NOT_FOUND, "CLUB_NOT_FOUND", "The requested club is unavailable");
    }

    static ClubsException alreadyApplied() {
        return new ClubsException(HttpStatus.CONFLICT, "CLUB_ALREADY_APPLIED", "The student already has an active club application");
    }

    static ClubsException applicationsClosed() {
        return new ClubsException(HttpStatus.CONFLICT, "CLUB_APPLICATIONS_CLOSED", "Applications are closed for this club");
    }

    static ClubsException capacityReached() {
        return new ClubsException(HttpStatus.CONFLICT, "CLUB_CAPACITY_REACHED", "This club has reached its active member capacity");
    }

    static ClubsException pendingApplicationNotFound() {
        return new ClubsException(HttpStatus.NOT_FOUND, "CLUB_APPLICATION_NOT_FOUND", "A pending club application was not found");
    }
}
