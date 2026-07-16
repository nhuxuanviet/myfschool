package vn.edu.fpt.myschool.auth.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import vn.edu.fpt.myschool.auth.domain.UserRole;

/**
 * @param role which role to open. Optional: an account holding a single mobile role goes straight
 *     in. An account holding several must name one, and the server answers 409 until it does.
 */
public record LoginRequest(
        @NotBlank @Size(max = 32) String phoneNumber,
        @NotBlank @Size(max = 72) String password,
        UserRole role) {
}
