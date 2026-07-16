package vn.edu.fpt.myschool.timetable.application;

import org.springframework.http.HttpStatus;

import vn.edu.fpt.myschool.shared.error.ApiException;

final class TimetableException extends ApiException {

    private TimetableException(HttpStatus status, String code, String message) {
        super(status, code, message);
    }

    static TimetableException invalidAuthenticatedSubject() {
        return new TimetableException(
                HttpStatus.UNAUTHORIZED,
                "UNAUTHORIZED",
                "Authentication token is invalid");
    }

    static TimetableException invalidWeekStart() {
        return new TimetableException(
                HttpStatus.BAD_REQUEST,
                "INVALID_WEEK_START",
                "weekStart must be an ISO Monday date");
    }

    static TimetableException studentProfileNotFound() {
        return new TimetableException(
                HttpStatus.NOT_FOUND,
                "STUDENT_PROFILE_NOT_FOUND",
                "Student profile was not found");
    }
}
