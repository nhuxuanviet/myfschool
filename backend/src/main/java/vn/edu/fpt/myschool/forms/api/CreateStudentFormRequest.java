package vn.edu.fpt.myschool.forms.api;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import vn.edu.fpt.myschool.forms.domain.StudentFormType;

public record CreateStudentFormRequest(
        @NotNull StudentFormType type,
        @NotBlank @Size(max = 1000) String reason,
        LocalDate startsOn,
        LocalDate endsOn) {
}
