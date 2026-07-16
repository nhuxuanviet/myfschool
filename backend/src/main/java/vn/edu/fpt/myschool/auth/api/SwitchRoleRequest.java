package vn.edu.fpt.myschool.auth.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import vn.edu.fpt.myschool.auth.domain.UserRole;

public record SwitchRoleRequest(
        @NotBlank @Size(max = 512) String refreshToken,
        @NotNull UserRole role) {
}
