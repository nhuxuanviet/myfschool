package vn.edu.fpt.myschool.auth.api;

import vn.edu.fpt.myschool.auth.application.AuthenticationResult;

public record AuthTokenResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        StudentResponse student) {

    static AuthTokenResponse from(AuthenticationResult result) {
        return new AuthTokenResponse(
                result.accessToken(),
                result.refreshToken(),
                result.expiresIn(),
                StudentResponse.from(result.student()));
    }
}
