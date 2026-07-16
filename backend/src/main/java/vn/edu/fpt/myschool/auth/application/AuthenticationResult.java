package vn.edu.fpt.myschool.auth.application;

import vn.edu.fpt.myschool.auth.domain.StudentProfile;
import vn.edu.fpt.myschool.auth.domain.UserAccount;

public record AuthenticationResult(
        String accessToken,
        String refreshToken,
        long expiresIn,
        UserAccount account) {

    public StudentProfile student() {
        return account.student();
    }
}
