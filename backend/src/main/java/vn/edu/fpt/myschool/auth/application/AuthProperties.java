package vn.edu.fpt.myschool.auth.application;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.security.auth")
public record AuthProperties(
        @NotBlank String issuer,
        @NotBlank String jwtSecret,
        @NotNull Duration accessTokenTtl,
        @NotNull Duration refreshTokenTtl,
        @NotNull Duration resetChallengeTtl,
        @NotNull Duration resetTokenTtl,
        @Min(1) @Max(10) int maxOtpAttempts,
        @NotNull Duration resetRequestRateLimitWindow,
        @Min(1) @Max(100) int maxResetRequestsPerWindow,
        @NotBlank String otpPepper,
        @Min(4) @Max(16) int passwordStrength) {

    private static final int MINIMUM_SECRET_BYTES = 32;

    public AuthProperties {
        requirePositive(accessTokenTtl, "accessTokenTtl");
        requirePositive(refreshTokenTtl, "refreshTokenTtl");
        requirePositive(resetChallengeTtl, "resetChallengeTtl");
        requirePositive(resetTokenTtl, "resetTokenTtl");
        requirePositive(resetRequestRateLimitWindow, "resetRequestRateLimitWindow");
        requireSecretLength(jwtSecret, "jwtSecret");
        requireSecretLength(otpPepper, "otpPepper");
    }

    private static void requirePositive(Duration duration, String name) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }

    private static void requireSecretLength(String secret, String name) {
        if (secret == null
                || secret.getBytes(StandardCharsets.UTF_8).length < MINIMUM_SECRET_BYTES) {
            throw new IllegalArgumentException(name + " must contain at least 32 bytes");
        }
    }
}
