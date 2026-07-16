package vn.edu.fpt.myschool.admin.identity.api;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.fpt.myschool.admin.identity.application.AdminIdentityService;
import vn.edu.fpt.myschool.admin.identity.domain.AdminIdentity;

@RestController
@RequestMapping("/api/v1/admin/parents")
@Tag(name = "Admin guardians", description = "Guardian directory and guardian-student links")
@SecurityRequirement(name = "bearerAuth")
public class AdminParentsController {

    private final AdminIdentityService service;

    public AdminParentsController(AdminIdentityService service) {
        this.service = service;
    }

    /**
     * @param phoneNumber optional. Supplying it also issues a sign-in account for the guardian;
     *     leaving it out records the guardian without one.
     */
    public record CreateParentRequest(
            @NotBlank @Size(max = 120) String fullName,
            @Email @Size(max = 190) String email,
            @Size(max = 32) String phoneNumber,
            @Size(max = 72) String initialPassword) {
    }

    public record UpdateParentRequest(
            @NotBlank @Size(max = 120) String fullName,
            @Email @Size(max = 190) String email,
            @NotNull Boolean enabled,
            @NotNull @PositiveOrZero Long version) {
    }

    public record LinkStudentRequest(
            @NotNull UUID studentId,
            @NotNull AdminIdentity.Relationship relationship,
            @NotNull @Min(1) Integer contactOrder) {
    }

    @GetMapping
    public AdminParentsResponse getParents(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "fullName,asc") String sort) {
        return AdminParentsResponse.from(service.getParents(query, enabled, page, size, sort));
    }

    @PostMapping
    public ResponseEntity<AdminTeacherMutationResponse> createParent(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateParentRequest request) {
        UUID id = service.createParent(
                actorId(jwt), request.fullName(), request.email(),
                request.phoneNumber(), request.initialPassword());
        return ResponseEntity.created(URI.create("/api/v1/admin/parents/" + id))
                .body(new AdminTeacherMutationResponse(id));
    }

    @PutMapping("/{parentId}")
    public AdminTeacherMutationResponse updateParent(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID parentId,
            @Valid @RequestBody UpdateParentRequest request) {
        service.updateParent(
                actorId(jwt), parentId, request.fullName(), request.email(),
                request.enabled(), request.version());
        return new AdminTeacherMutationResponse(parentId);
    }

    @GetMapping("/links")
    public List<AdminGuardianLinkResponse> getLinks(
            @RequestParam(required = false) UUID parentId,
            @RequestParam(required = false) UUID studentId,
            @RequestParam(defaultValue = "true") boolean inForceOnly) {
        return service.getLinks(parentId, studentId, inForceOnly).stream()
                .map(AdminGuardianLinkResponse::from)
                .toList();
    }

    @PostMapping("/{parentId}/links")
    public ResponseEntity<AdminTeacherMutationResponse> linkStudent(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID parentId,
            @Valid @RequestBody LinkStudentRequest request) {
        UUID id = service.linkGuardian(
                actorId(jwt), parentId, request.studentId(),
                request.relationship(), request.contactOrder());
        return ResponseEntity.created(URI.create("/api/v1/admin/parents/links/" + id))
                .body(new AdminTeacherMutationResponse(id));
    }

    /** Ends the link. The row survives as a record of who could see the child's data, and until when. */
    @DeleteMapping("/links/{linkId}")
    public ResponseEntity<Void> unlinkStudent(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID linkId) {
        service.unlinkGuardian(actorId(jwt), linkId);
        return ResponseEntity.noContent().build();
    }

    private static UUID actorId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
