package vn.edu.fpt.myschool.admin.grades.api;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.fpt.myschool.admin.grades.application.GradeGovernanceService;
import vn.edu.fpt.myschool.admin.grades.domain.GradeGovernance;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin grade governance", description = "Locking grade books and ruling on corrections")
@SecurityRequirement(name = "bearerAuth")
public class AdminGradeBooksController {

    private final GradeGovernanceService service;

    public AdminGradeBooksController(GradeGovernanceService service) {
        this.service = service;
    }

    public record VersionedRequest(@NotNull @PositiveOrZero Long version) {
    }

    public record DecisionRequest(@Size(max = 500) String decisionNote) {
    }

    @GetMapping("/gradebooks")
    public List<GradeGovernance.BookSummary> getBooks(
            @RequestParam(required = false) UUID academicTermId,
            @RequestParam(required = false) Boolean locked) {
        return service.getBooks(academicTermId, locked);
    }

    @PostMapping("/gradebooks/{bookId}/lock")
    public ResponseEntity<Void> lock(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID bookId,
            @Valid @RequestBody VersionedRequest request) {
        service.lock(actorId(jwt), bookId, request.version());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/gradebooks/{bookId}/unlock")
    public ResponseEntity<Void> unlock(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID bookId,
            @Valid @RequestBody VersionedRequest request) {
        service.unlock(actorId(jwt), bookId, request.version());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/grade-change-requests")
    public List<GradeGovernance.ChangeRequest> getChangeRequests(
            @RequestParam(required = false) String status) {
        return service.getChangeRequests(status);
    }

    @PostMapping("/grade-change-requests/{requestId}/approve")
    public ResponseEntity<Void> approve(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID requestId,
            @Valid @RequestBody DecisionRequest request) {
        service.approve(actorId(jwt), requestId, request.decisionNote());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/grade-change-requests/{requestId}/reject")
    public ResponseEntity<Void> reject(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID requestId,
            @Valid @RequestBody DecisionRequest request) {
        service.reject(actorId(jwt), requestId, request.decisionNote());
        return ResponseEntity.noContent().build();
    }

    private static UUID actorId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
