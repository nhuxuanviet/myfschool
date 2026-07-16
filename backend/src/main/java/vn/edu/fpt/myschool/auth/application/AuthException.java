package vn.edu.fpt.myschool.auth.application;

import org.springframework.http.HttpStatus;

import vn.edu.fpt.myschool.shared.error.ApiException;

public final class AuthException extends ApiException {

    private AuthException(HttpStatus status, String code, String message) {
        super(status, code, message);
    }

    static AuthException invalidCredentials() {
        return new AuthException(
                HttpStatus.UNAUTHORIZED,
                "INVALID_CREDENTIALS",
                "Phone number or password is incorrect");
    }

    /**
     * The account holds several mobile roles and the caller did not say which one to open.
     *
     * <p>Deliberately not an authentication failure: the credentials were correct. The client
     * shows a role chooser and repeats the request with a role.
     */
    static AuthException roleSelectionRequired() {
        return new AuthException(
                HttpStatus.CONFLICT,
                "ROLE_SELECTION_REQUIRED",
                "Account holds several roles. Repeat the request with the role to open");
    }

    static AuthException invalidRefreshToken() {
        return new AuthException(
                HttpStatus.UNAUTHORIZED,
                "INVALID_REFRESH_TOKEN",
                "Refresh token is invalid or expired");
    }

    static AuthException adminLoginRateLimited() {
        return new AuthException(
                HttpStatus.TOO_MANY_REQUESTS,
                "ADMIN_LOGIN_RATE_LIMITED",
                "Too many login attempts. Please try again later");
    }

    static AuthException invalidChallenge() {
        return new AuthException(
                HttpStatus.BAD_REQUEST,
                "RESET_CHALLENGE_INVALID",
                "Password reset challenge is invalid or expired");
    }

    static AuthException invalidOtp() {
        return new AuthException(
                HttpStatus.BAD_REQUEST,
                "INVALID_OTP",
                "OTP is invalid or expired");
    }

    static AuthException resetRequestRateLimited() {
        return new AuthException(
                HttpStatus.TOO_MANY_REQUESTS,
                "PASSWORD_RESET_RATE_LIMITED",
                "Too many password reset requests. Please try again later");
    }

    static AuthException invalidResetToken() {
        return new AuthException(
                HttpStatus.BAD_REQUEST,
                "RESET_TOKEN_INVALID",
                "Reset token is invalid or expired");
    }

    static AuthException weakPassword() {
        return new AuthException(
                HttpStatus.BAD_REQUEST,
                "WEAK_PASSWORD",
                "Password must contain uppercase, lowercase, number, and special characters");
    }
}
