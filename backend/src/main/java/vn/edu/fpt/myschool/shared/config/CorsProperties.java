package vn.edu.fpt.myschool.shared.config;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.cors")
public record CorsProperties(
        @NotEmpty List<@NotBlank String> pathPatterns,
        @NotEmpty List<@NotBlank String> allowedOrigins,
        @NotEmpty List<@NotBlank String> allowedMethods,
        @NotEmpty List<@NotBlank String> allowedHeaders,
        List<@NotBlank String> exposedHeaders,
        boolean allowCredentials,
        @NotNull Duration maxAge) {

    public CorsProperties {
        pathPatterns = immutableCopy(pathPatterns, "pathPatterns");
        allowedOrigins = immutableCopy(allowedOrigins, "allowedOrigins");
        allowedMethods = immutableCopy(allowedMethods, "allowedMethods");
        allowedHeaders = immutableCopy(allowedHeaders, "allowedHeaders");
        exposedHeaders = immutableCopy(exposedHeaders, "exposedHeaders");
        Objects.requireNonNull(maxAge, "maxAge must not be null");

        if (maxAge.isNegative()) {
            throw new IllegalArgumentException("maxAge must not be negative");
        }
        if (allowCredentials && allowedOrigins.contains("*")) {
            throw new IllegalArgumentException(
                    "Wildcard CORS origins cannot be used when credentials are enabled");
        }
    }

    private static List<String> immutableCopy(List<String> values, String name) {
        Objects.requireNonNull(values, name + " must not be null");
        return List.copyOf(values);
    }
}
