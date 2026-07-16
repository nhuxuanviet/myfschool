package vn.edu.fpt.myschool.auth.api;

import vn.edu.fpt.myschool.auth.application.ResetVerificationResult;

public record PasswordResetVerificationResponse(String resetToken) {

    static PasswordResetVerificationResponse from(ResetVerificationResult result) {
        return new PasswordResetVerificationResponse(result.resetToken());
    }
}
