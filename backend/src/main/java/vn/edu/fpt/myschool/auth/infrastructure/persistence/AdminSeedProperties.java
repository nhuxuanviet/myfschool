package vn.edu.fpt.myschool.auth.infrastructure.persistence;

import jakarta.validation.constraints.NotBlank;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.security.admin-seed")
record AdminSeedProperties(
        @NotBlank String phoneNumber,
        @NotBlank String password,
        @NotBlank String fullName) {
}
