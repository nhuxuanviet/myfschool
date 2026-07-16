package vn.edu.fpt.myschool.admin.academics.api;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAcademicTermRequest(
        @NotNull UUID academicYearId,
        @NotBlank @Size(max = 16) String code,
        @NotBlank @Size(max = 80) String name,
        @NotNull LocalDate startsOn,
        @NotNull LocalDate endsOn) {
}
