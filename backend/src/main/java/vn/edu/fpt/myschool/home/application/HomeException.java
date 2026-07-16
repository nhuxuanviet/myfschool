package vn.edu.fpt.myschool.home.application;

import org.springframework.http.HttpStatus;

import vn.edu.fpt.myschool.shared.error.ApiException;

final class HomeException extends ApiException {

    private HomeException(HttpStatus status, String code, String message) {
        super(status, code, message);
    }

    static HomeException invalidAuthenticatedSubject() {
        return new HomeException(
                HttpStatus.UNAUTHORIZED,
                "UNAUTHORIZED",
                "Authentication token is invalid");
    }

    static HomeException studentProfileNotFound() {
        return new HomeException(
                HttpStatus.NOT_FOUND,
                "STUDENT_PROFILE_NOT_FOUND",
                "Student profile was not found");
    }
}
