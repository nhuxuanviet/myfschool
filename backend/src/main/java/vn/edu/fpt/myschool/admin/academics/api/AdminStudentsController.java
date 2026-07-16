package vn.edu.fpt.myschool.admin.academics.api;

import java.net.URI;
import java.util.UUID;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.fpt.myschool.admin.academics.application.AdminAcademicsService;

@RestController
@RequestMapping("/api/v1/admin/students")
@Tag(name = "Admin students", description = "Paginated student directory management")
@SecurityRequirement(name = "bearerAuth")
public class AdminStudentsController {

    private final AdminAcademicsService service;

    public AdminStudentsController(AdminAcademicsService service) {
        this.service = service;
    }

    @GetMapping
    public AdminStudentsResponse getStudents(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Integer gradeLevel,
            @RequestParam(required = false) UUID classId,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "fullName,asc") String sort) {
        return AdminStudentsResponse.from(service.getStudents(
                query, gradeLevel, classId, enabled, page, size, sort));
    }

    @PostMapping
    public ResponseEntity<AdminMutationResponse> createStudent(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateStudentRequest request) {
        UUID id = service.createStudent(
                actorId(jwt), request.phoneNumber(), request.initialPassword(),
                request.studentCode(), request.fullName(), request.classId());
        return ResponseEntity.created(URI.create("/api/v1/admin/students/" + id))
                .body(new AdminMutationResponse(id));
    }

    @PutMapping("/{studentId}")
    public ResponseEntity<Void> updateStudent(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID studentId,
            @Valid @RequestBody UpdateStudentRequest request) {
        service.updateStudent(
                actorId(jwt), studentId, request.phoneNumber(), request.studentCode(),
                request.fullName(), request.classId(), request.enabled(), request.version());
        return ResponseEntity.noContent().build();
    }

    private static UUID actorId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
