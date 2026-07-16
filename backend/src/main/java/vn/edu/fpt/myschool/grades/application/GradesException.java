package vn.edu.fpt.myschool.grades.application;

import org.springframework.http.HttpStatus;

import vn.edu.fpt.myschool.shared.error.ApiException;

final class GradesException extends ApiException {

    private GradesException(HttpStatus status, String code, String message) {
        super(status, code, message);
    }

    static GradesException invalidAuthenticatedSubject() {
        return new GradesException(
                HttpStatus.UNAUTHORIZED,
                "UNAUTHORIZED",
                "Authentication token is invalid");
    }

    static GradesException studentProfileNotFound() {
        return new GradesException(
                HttpStatus.NOT_FOUND,
                "STUDENT_PROFILE_NOT_FOUND",
                "Student profile was not found");
    }

    static GradesException requestedTermNotFound() {
        return new GradesException(
                HttpStatus.NOT_FOUND,
                "GRADE_TERM_NOT_FOUND",
                "The requested semester grades are unavailable");
    }
}
