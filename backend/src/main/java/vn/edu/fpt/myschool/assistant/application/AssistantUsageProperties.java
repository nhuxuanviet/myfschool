package vn.edu.fpt.myschool.assistant.application;

import java.time.Duration;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.assistant.usage")
public record AssistantUsageProperties(
        @NotNull Duration window,
        @Min(1) int maxRequests) {

    public AssistantUsageProperties {
        if (window == null || window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("Assistant usage window must be positive");
        }
    }
}
