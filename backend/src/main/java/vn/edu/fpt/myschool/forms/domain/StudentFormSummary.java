package vn.edu.fpt.myschool.forms.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record StudentFormSummary(
        UUID id,
        StudentFormType type,
        LocalDate startsOn,
        LocalDate endsOn,
        StudentFormStatus status,
        Instant submittedAt,
        Instant updatedAt) {

    public StudentFormSummary {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(submittedAt, "submittedAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (updatedAt.isBefore(submittedAt)) {
            throw new IllegalArgumentException("updatedAt must not precede submittedAt");
        }
        validateDates(type, startsOn, endsOn);
    }

    public boolean canCancel() {
        return status.canBeCancelledByStudent();
    }

    public static void validateDates(StudentFormType type, LocalDate startsOn, LocalDate endsOn) {
        if (type == StudentFormType.LEAVE_OF_ABSENCE) {
            if (startsOn == null || endsOn == null || endsOn.isBefore(startsOn)) {
                throw new IllegalArgumentException("Leave forms require a valid date range");
            }
        } else if (startsOn != null || endsOn != null) {
            throw new IllegalArgumentException("Only leave forms may contain a date range");
        }
    }
}
