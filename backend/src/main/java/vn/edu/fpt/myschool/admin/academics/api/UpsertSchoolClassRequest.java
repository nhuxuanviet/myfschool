package vn.edu.fpt.myschool.admin.academics.api;

import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record UpsertSchoolClassRequest(
        @NotNull UUID academicYearId,
        @NotBlank @Size(max = 32) String code,
        @NotBlank @Size(max = 80) String name,
        @Min(6) @Max(12) int gradeLevel,
        boolean enabled,
        @PositiveOrZero long version) {
}
