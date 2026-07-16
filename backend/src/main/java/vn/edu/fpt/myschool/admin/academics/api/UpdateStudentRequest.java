package vn.edu.fpt.myschool.admin.academics.api;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record UpdateStudentRequest(
        @NotBlank @Size(max = 20) String phoneNumber,
        @NotBlank @Size(max = 32) String studentCode,
        @NotBlank @Size(max = 120) String fullName,
        @NotNull UUID classId,
        boolean enabled,
        @PositiveOrZero long version) {
}
