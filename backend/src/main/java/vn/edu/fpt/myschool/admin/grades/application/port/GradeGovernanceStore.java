package vn.edu.fpt.myschool.admin.grades.application.port;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import vn.edu.fpt.myschool.admin.grades.domain.GradeGovernance;

public interface GradeGovernanceStore {

    List<GradeGovernance.BookSummary> findBooks(UUID academicTermId, Boolean locked);

    boolean lock(UUID bookId, UUID lockedBy, long expectedVersion, Instant now);

    boolean unlock(UUID bookId, long expectedVersion, Instant now);

    List<GradeGovernance.ChangeRequest> findChangeRequests(String status);

    Optional<GradeGovernance.ChangeRequest> findChangeRequest(UUID requestId);

    UUID createChangeRequest(
            UUID assessmentId,
            UUID requestedBy,
            BigDecimal newScore,
            String newOutcome,
            String reason,
            Instant now);

    boolean decide(
            UUID requestId, String status, UUID decidedBy, String decisionNote, Instant now);

    /** Applies the approved value to the mark itself. */
    boolean applyChange(UUID assessmentId, BigDecimal score, String outcome, Instant now);

    void recordAudit(
            UUID actorUserId,
            String action,
            String entityType,
            UUID entityId,
            String changedFieldsJson,
            Instant occurredAt);
}
