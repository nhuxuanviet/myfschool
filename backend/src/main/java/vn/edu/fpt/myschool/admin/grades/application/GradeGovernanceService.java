package vn.edu.fpt.myschool.admin.grades.application;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.fpt.myschool.admin.grades.application.port.GradeGovernanceStore;
import vn.edu.fpt.myschool.admin.grades.domain.GradeGovernance;
import vn.edu.fpt.myschool.shared.error.ApiException;

/**
 * Locking grade books and deciding grade-change requests.
 *
 * <p>Both are the administration's call, not the teacher's: the teacher publishes, the
 * administration closes the book and rules on corrections afterwards (spec §5.5, §7.1).
 */
@Service
public class GradeGovernanceService {

    private static final String BOOK_ENTITY = "GRADE_BOOK";
    private static final String REQUEST_ENTITY = "GRADE_CHANGE_REQUEST";

    private final GradeGovernanceStore store;
    private final Clock clock;

    public GradeGovernanceService(GradeGovernanceStore store, Clock clock) {
        this.store = store;
        this.clock = clock;
    }

    public static final class GovernanceException extends ApiException {
        private GovernanceException(HttpStatus status, String code, String message) {
            super(status, code, message);
        }
    }

    private static GovernanceException notFound(String what) {
        return new GovernanceException(
                HttpStatus.NOT_FOUND, what + "_NOT_FOUND", what + " does not exist");
    }

    private static GovernanceException conflict(String code, String message) {
        return new GovernanceException(HttpStatus.CONFLICT, code, message);
    }

    @Transactional(readOnly = true)
    public List<GradeGovernance.BookSummary> getBooks(UUID academicTermId, Boolean locked) {
        return store.findBooks(academicTermId, locked);
    }

    @Transactional
    public void lock(UUID actorUserId, UUID bookId, long expectedVersion) {
        if (!store.lock(bookId, actorUserId, expectedVersion, clock.instant())) {
            throw conflict(
                    "GRADE_BOOK_NOT_LOCKABLE",
                    "Grade book is already locked or was changed by someone else");
        }
        store.recordAudit(actorUserId, "LOCK", BOOK_ENTITY, bookId,
                "{\"fields\":[\"lockedAt\"]}", clock.instant());
    }

    @Transactional
    public void unlock(UUID actorUserId, UUID bookId, long expectedVersion) {
        if (!store.unlock(bookId, expectedVersion, clock.instant())) {
            throw conflict(
                    "GRADE_BOOK_NOT_UNLOCKABLE",
                    "Grade book is not locked or was changed by someone else");
        }
        store.recordAudit(actorUserId, "UNLOCK", BOOK_ENTITY, bookId,
                "{\"fields\":[\"lockedAt\"]}", clock.instant());
    }

    @Transactional(readOnly = true)
    public List<GradeGovernance.ChangeRequest> getChangeRequests(String status) {
        return store.findChangeRequests(status);
    }

    /**
     * Approving both decides the request and applies the value, in one transaction.
     *
     * <p>Splitting them would allow an approved request whose mark never changed, which is the
     * worst of both: the trail says corrected, the report card says otherwise.
     */
    @Transactional
    public void approve(UUID actorUserId, UUID requestId, String decisionNote) {
        GradeGovernance.ChangeRequest request = store.findChangeRequest(requestId)
                .orElseThrow(() -> notFound("GRADE_CHANGE_REQUEST"));
        if (!"PENDING".equals(request.status())) {
            throw conflict("GRADE_CHANGE_REQUEST_DECIDED", "This request has already been decided");
        }
        if (!store.decide(requestId, "APPROVED", actorUserId, decisionNote, clock.instant())) {
            throw conflict("GRADE_CHANGE_REQUEST_DECIDED", "This request has already been decided");
        }
        if (!store.applyChange(
                request.assessmentId(), request.newScore(), request.newOutcome(), clock.instant())) {
            throw notFound("GRADE_ASSESSMENT");
        }
        store.recordAudit(actorUserId, "APPROVE", REQUEST_ENTITY, requestId,
                "{\"fields\":[\"status\",\"score\",\"outcome\"]}", clock.instant());
    }

    /** Rejecting leaves the mark untouched and records why. */
    @Transactional
    public void reject(UUID actorUserId, UUID requestId, String decisionNote) {
        if (!store.decide(requestId, "REJECTED", actorUserId, decisionNote, clock.instant())) {
            throw conflict(
                    "GRADE_CHANGE_REQUEST_DECIDED",
                    "This request does not exist or has already been decided");
        }
        store.recordAudit(actorUserId, "REJECT", REQUEST_ENTITY, requestId,
                "{\"fields\":[\"status\"]}", clock.instant());
    }

    /** Raised by the responsible teacher; the caller has already checked the assignment. */
    @Transactional
    public UUID requestChange(
            UUID requesterUserId,
            UUID assessmentId,
            BigDecimal newScore,
            String newOutcome,
            String reason) {
        UUID id = store.createChangeRequest(
                assessmentId, requesterUserId, newScore, newOutcome, reason.strip(), clock.instant());
        store.recordAudit(requesterUserId, "CREATE", REQUEST_ENTITY, id,
                "{\"fields\":[\"newScore\",\"newOutcome\",\"reason\"]}", clock.instant());
        return id;
    }
}
