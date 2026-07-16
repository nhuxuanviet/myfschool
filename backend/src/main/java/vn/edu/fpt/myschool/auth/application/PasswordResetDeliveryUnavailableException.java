package vn.edu.fpt.myschool.auth.application;

import java.util.Objects;

public final class PasswordResetDeliveryUnavailableException extends RuntimeException {

    private final ResetChallengeResult challenge;

    PasswordResetDeliveryUnavailableException(
            ResetChallengeResult challenge,
            Throwable cause) {
        super("Password-reset OTP delivery is temporarily unavailable", cause);
        this.challenge = Objects.requireNonNull(challenge, "challenge must not be null");
    }

    public ResetChallengeResult challenge() {
        return challenge;
    }
}
