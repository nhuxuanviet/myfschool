package vn.edu.fpt.myschool.teacher.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * A leave request awaiting the homeroom teacher.
 *
 * @param submittedByRole STUDENT or PARENT — the homeroom teacher should see whether the child or
 *     their guardian asked, because it changes how the request reads
 */
public record HomeroomForm(
        UUID id,
        UUID studentId,
        String studentCode,
        String studentFullName,
        String classCode,
        String reason,
        LocalDate startsOn,
        LocalDate endsOn,
        String status,
        String submittedByRole,
        Instant submittedAt) {

    public HomeroomForm {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(studentId, "studentId must not be null");
        Objects.requireNonNull(status, "status must not be null");
    }

    public boolean isOpen() {
        return "SUBMITTED".equals(status) || "IN_REVIEW".equals(status);
    }
}
