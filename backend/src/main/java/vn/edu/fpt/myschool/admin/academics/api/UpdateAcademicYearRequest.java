package vn.edu.fpt.myschool.admin.academics.api;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record UpdateAcademicYearRequest(
        @NotBlank @Size(max = 16) String code,
        @NotNull LocalDate startsOn,
        @NotNull LocalDate endsOn,
        @PositiveOrZero long version) {
}
