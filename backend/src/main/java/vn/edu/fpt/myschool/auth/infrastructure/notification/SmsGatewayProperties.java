package vn.edu.fpt.myschool.auth.infrastructure.notification;

import java.net.URI;
import java.time.Duration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.security.sms")
record SmsGatewayProperties(
        @NotNull URI endpoint,
        @NotBlank String apiKey,
        @NotBlank String senderId,
        @NotNull Duration connectTimeout,
        @NotNull Duration readTimeout) {

    SmsGatewayProperties {
        if (endpoint == null
                || !"https".equalsIgnoreCase(endpoint.getScheme())
                || endpoint.getHost() == null) {
            throw new IllegalArgumentException("SMS gateway endpoint must be an HTTPS URL");
        }
        requirePositive(connectTimeout, "connectTimeout");
        requirePositive(readTimeout, "readTimeout");
    }

    private static void requirePositive(Duration duration, String name) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }
}
