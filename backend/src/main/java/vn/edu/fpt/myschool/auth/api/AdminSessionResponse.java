package vn.edu.fpt.myschool.auth.api;

import vn.edu.fpt.myschool.auth.application.AuthenticationResult;

public record AdminSessionResponse(
        String accessToken,
        long expiresIn,
        String csrfToken,
        AdminAccountResponse account) {

    static AdminSessionResponse from(AuthenticationResult result, String csrfToken) {
        return new AdminSessionResponse(
                result.accessToken(),
                result.expiresIn(),
                csrfToken,
                AdminAccountResponse.from(result.admin()));
    }
}
