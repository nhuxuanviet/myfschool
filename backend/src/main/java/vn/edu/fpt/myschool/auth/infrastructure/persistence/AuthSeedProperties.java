package vn.edu.fpt.myschool.auth.infrastructure.persistence;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.security.seed")
record AuthSeedProperties(
        @NotBlank String phoneNumber,
        @NotBlank String password,
        @NotBlank String studentCode,
        @NotBlank String fullName,
        @Min(6) @Max(12) int gradeLevel,
        @NotBlank String className) {
}
