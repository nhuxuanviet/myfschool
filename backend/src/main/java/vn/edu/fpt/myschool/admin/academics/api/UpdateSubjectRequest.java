package vn.edu.fpt.myschool.admin.academics.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record UpdateSubjectRequest(
        @NotBlank @Size(max = 32) String code,
        @NotBlank @Size(max = 120) String name,
        boolean enabled,
        @PositiveOrZero long version) {
}
