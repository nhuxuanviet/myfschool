package vn.edu.fpt.myschool.auth.application;

import java.time.Duration;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.security.admin-session")
public record AdminSessionProperties(
        @NotBlank String refreshCookieName,
        @NotBlank String csrfCookieName,
        @NotBlank String csrfHeaderName,
        boolean secureCookies,
        @NotNull Duration loginAttemptWindow,
        @Min(1) @Max(20) int maxLoginAttempts,
        @NotNull Duration loginBlockDuration) {

    public AdminSessionProperties {
        requirePositive(loginAttemptWindow, "loginAttemptWindow");
        requirePositive(loginBlockDuration, "loginBlockDuration");
    }

    private static void requirePositive(Duration duration, String name) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }
}
