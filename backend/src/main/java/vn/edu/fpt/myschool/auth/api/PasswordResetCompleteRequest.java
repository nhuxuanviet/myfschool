package vn.edu.fpt.myschool.auth.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetCompleteRequest(
        @NotBlank @Size(max = 512) String resetToken,
        @NotBlank @Size(min = 8, max = 72) String newPassword) {
}
