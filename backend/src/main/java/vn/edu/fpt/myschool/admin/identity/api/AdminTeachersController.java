package vn.edu.fpt.myschool.admin.identity.api;

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

import vn.edu.fpt.myschool.admin.identity.application.AdminIdentityService;

@RestController
@RequestMapping("/api/v1/admin/teachers")
@Tag(name = "Admin teachers", description = "Teacher roster management")
@SecurityRequirement(name = "bearerAuth")
public class AdminTeachersController {

    private final AdminIdentityService service;

    public AdminTeachersController(AdminIdentityService service) {
        this.service = service;
    }

    @GetMapping
    public AdminTeachersResponse getTeachers(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) Boolean hasAccount,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "fullName,asc") String sort) {
        return AdminTeachersResponse.from(
                service.getTeachers(query, enabled, hasAccount, page, size, sort));
    }

    @PostMapping
    public ResponseEntity<AdminTeacherMutationResponse> createTeacher(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateTeacherRequest request) {
        UUID id = service.createTeacher(
                actorId(jwt), request.teacherCode(), request.fullName(), request.email());
        return ResponseEntity.created(URI.create("/api/v1/admin/teachers/" + id))
                .body(new AdminTeacherMutationResponse(id));
    }

    @PutMapping("/{teacherId}")
    public AdminTeacherMutationResponse updateTeacher(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID teacherId,
            @Valid @RequestBody UpdateTeacherRequest request) {
        service.updateTeacher(
                actorId(jwt), teacherId, request.teacherCode(), request.fullName(),
                request.email(), request.enabled(), request.version());
        return new AdminTeacherMutationResponse(teacherId);
    }

    private static UUID actorId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
