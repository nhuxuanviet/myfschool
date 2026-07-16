package vn.edu.fpt.myschool.auth.api;

import java.util.UUID;

import vn.edu.fpt.myschool.auth.application.ResetChallengeResult;

public record PasswordResetChallengeResponse(UUID challengeId, long expiresIn) {

    static PasswordResetChallengeResponse from(ResetChallengeResult result) {
        return new PasswordResetChallengeResponse(result.challengeId(), result.expiresIn());
    }
}
