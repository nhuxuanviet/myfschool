package vn.edu.fpt.myschool.auth.application;

import org.springframework.http.HttpStatus;

import vn.edu.fpt.myschool.shared.error.ApiException;

public final class AdminSessionException extends ApiException {

    private AdminSessionException(HttpStatus status, String code, String message) {
        super(status, code, message);
    }

    public static AdminSessionException missingRefreshToken() {
        return new AdminSessionException(
                HttpStatus.UNAUTHORIZED,
                "INVALID_ADMIN_SESSION",
                "Admin session is invalid or expired");
    }

    public static AdminSessionException invalidCsrfToken() {
        return new AdminSessionException(
                HttpStatus.FORBIDDEN,
                "INVALID_CSRF_TOKEN",
                "CSRF token is missing or invalid");
    }
}
