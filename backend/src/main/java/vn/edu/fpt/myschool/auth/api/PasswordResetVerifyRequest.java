package vn.edu.fpt.myschool.auth.api;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PasswordResetVerifyRequest(
        @NotNull UUID challengeId,
        @NotBlank @Pattern(regexp = "^[0-9]{6}$") String otp) {
}
