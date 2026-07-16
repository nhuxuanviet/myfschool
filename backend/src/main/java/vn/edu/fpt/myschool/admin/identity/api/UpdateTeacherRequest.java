package vn.edu.fpt.myschool.admin.identity.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record UpdateTeacherRequest(
        @NotBlank @Size(max = 32) String teacherCode,
        @NotBlank @Size(max = 120) String fullName,
        @Email @Size(max = 190) String email,
        @NotNull Boolean enabled,
        @NotNull @PositiveOrZero Long version) {
}
