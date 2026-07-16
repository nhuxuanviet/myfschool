package vn.edu.fpt.myschool.forms.api;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import vn.edu.fpt.myschool.forms.domain.StudentFormSummary;

public record StudentFormSummaryResponse(
        UUID id,
        String type,
        LocalDate startsOn,
        LocalDate endsOn,
        String status,
        Instant submittedAt,
        Instant updatedAt,
        boolean canCancel) {

    static StudentFormSummaryResponse from(StudentFormSummary form) {
        return new StudentFormSummaryResponse(
                form.id(),
                form.type().name(),
                form.startsOn(),
                form.endsOn(),
                form.status().name(),
                form.submittedAt(),
                form.updatedAt(),
                form.canCancel());
    }
}
