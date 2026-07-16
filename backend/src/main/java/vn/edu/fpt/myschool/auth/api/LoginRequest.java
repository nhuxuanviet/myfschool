package vn.edu.fpt.myschool.auth.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Size(max = 32) String phoneNumber,
        @NotBlank @Size(max = 72) String password) {
}
