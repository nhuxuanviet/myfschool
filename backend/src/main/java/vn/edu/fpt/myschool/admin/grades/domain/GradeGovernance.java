package vn.edu.fpt.myschool.admin.grades.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** What the administration governs about marks: who may still edit, and which corrections stand. */
public final class GradeGovernance {

    private GradeGovernance() {
    }

    public record BookSummary(
            UUID id,
            String classCode,
            String subjectName,
            String academicTermCode,
            Instant publishedAt,
            Instant lockedAt,
            String teacherFullName,
            int columnCount,
            int pendingMarkCount,
            long version) {

        public BookSummary {
            Objects.requireNonNull(id, "id must not be null");
        }
    }

    /**
     * A correction a teacher asked for after the book was locked.
     *
     * @param oldScore what the mark held when the request was raised, kept so the decision can be
     *     read later without replaying history
     */
    public record ChangeRequest(
            UUID id,
            UUID assessmentId,
            String studentFullName,
            String studentCode,
            String classCode,
            String subjectName,
            String displayLabel,
            BigDecimal oldScore,
            String oldOutcome,
            BigDecimal newScore,
            String newOutcome,
            String reason,
            String status,
            String requestedByFullName,
            Instant createdAt,
            Instant decidedAt,
            String decisionNote) {

        public ChangeRequest {
            Objects.requireNonNull(id, "id must not be null");
            Objects.requireNonNull(assessmentId, "assessmentId must not be null");
        }
    }
}
