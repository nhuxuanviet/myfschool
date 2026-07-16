package vn.edu.fpt.myschool.admin.academics.api;

import java.net.URI;
import java.util.UUID;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.fpt.myschool.admin.academics.application.AdminAcademicsService;

@RestController
@RequestMapping("/api/v1/admin/academics")
@Tag(name = "Admin academics", description = "Academic catalog and school class management")
@SecurityRequirement(name = "bearerAuth")
public class AdminAcademicsController {

    private final AdminAcademicsService service;

    public AdminAcademicsController(AdminAcademicsService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Get the academic catalog")
    public AdminAcademicsResponse getCatalog() {
        return AdminAcademicsResponse.from(service.getCatalog());
    }

    @PostMapping("/years")
    public ResponseEntity<AdminMutationResponse> createAcademicYear(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateAcademicYearRequest request) {
        UUID id = service.createAcademicYear(
                actorId(jwt), request.code(), request.startsOn(), request.endsOn());
        return created("/api/v1/admin/academics/years/" + id, id);
    }

    @PostMapping("/terms")
    public ResponseEntity<AdminMutationResponse> createAcademicTerm(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateAcademicTermRequest request) {
        UUID id = service.createAcademicTerm(
                actorId(jwt), request.academicYearId(), request.code(), request.name(),
                request.startsOn(), request.endsOn());
        return created("/api/v1/admin/academics/terms/" + id, id);
    }

    @PutMapping("/years/{yearId}")
    public ResponseEntity<Void> updateAcademicYear(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID yearId,
            @Valid @RequestBody UpdateAcademicYearRequest request) {
        service.updateAcademicYear(
                actorId(jwt), yearId, request.code(), request.startsOn(), request.endsOn(),
                request.version());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/terms/{termId}")
    public ResponseEntity<Void> updateAcademicTerm(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID termId,
            @Valid @RequestBody UpdateAcademicTermRequest request) {
        service.updateAcademicTerm(
                actorId(jwt), termId, request.academicYearId(), request.code(), request.name(),
                request.startsOn(), request.endsOn(), request.version());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/subjects")
    public ResponseEntity<AdminMutationResponse> createSubject(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateSubjectRequest request) {
        UUID id = service.createSubject(actorId(jwt), request.code(), request.name());
        return created("/api/v1/admin/academics/subjects/" + id, id);
    }

    @PostMapping("/classes")
    public ResponseEntity<AdminMutationResponse> createClass(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpsertSchoolClassRequest request) {
        UUID id = service.createClass(
                actorId(jwt), request.academicYearId(), request.code(), request.name(),
                request.gradeLevel());
        return created("/api/v1/admin/academics/classes/" + id, id);
    }

    @PutMapping("/subjects/{subjectId}")
    public ResponseEntity<Void> updateSubject(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID subjectId,
            @Valid @RequestBody UpdateSubjectRequest request) {
        service.updateSubject(
                actorId(jwt), subjectId, request.code(), request.name(), request.enabled(),
                request.version());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/classes/{classId}")
    public ResponseEntity<Void> updateClass(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID classId,
            @Valid @RequestBody UpsertSchoolClassRequest request) {
        service.updateClass(
                actorId(jwt), classId, request.academicYearId(), request.code(), request.name(),
                request.gradeLevel(), request.enabled(), request.version());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/years/{yearId}")
    public ResponseEntity<Void> deleteAcademicYear(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID yearId,
            @RequestParam long version) {
        service.deleteAcademicYear(actorId(jwt), yearId, version);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/terms/{termId}")
    public ResponseEntity<Void> deleteAcademicTerm(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID termId,
            @RequestParam long version) {
        service.deleteAcademicTerm(actorId(jwt), termId, version);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/subjects/{subjectId}")
    public ResponseEntity<Void> deleteSubject(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID subjectId,
            @RequestParam long version) {
        service.deleteSubject(actorId(jwt), subjectId, version);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/classes/{classId}")
    public ResponseEntity<Void> deleteClass(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID classId,
            @RequestParam long version) {
        service.deleteClass(actorId(jwt), classId, version);
        return ResponseEntity.noContent().build();
    }

    private static UUID actorId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    private static ResponseEntity<AdminMutationResponse> created(String path, UUID id) {
        return ResponseEntity.created(URI.create(path)).body(new AdminMutationResponse(id));
    }
}
