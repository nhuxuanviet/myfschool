package vn.edu.fpt.myschool.teacher.api;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
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

import vn.edu.fpt.myschool.teacher.application.HomeroomFormService;
import vn.edu.fpt.myschool.teacher.domain.HomeroomForm;

@RestController
@RequestMapping("/api/v1/teacher/homeroom-forms")
@Tag(name = "Homeroom forms", description = "Leave requests from the homeroom class")
@SecurityRequirement(name = "bearerAuth")
public class HomeroomFormsController {

    private final HomeroomFormService service;

    public HomeroomFormsController(HomeroomFormService service) {
        this.service = service;
    }

    public record DecisionRequest(@Size(max = 500) String note) {
    }

    /** Empty for a teacher who is nobody's homeroom teacher; there is no queue to show them. */
    @GetMapping
    public List<HomeroomForm> getLeaveForms(
            @AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) String status) {
        return service.getLeaveForms(userId(jwt), status);
    }

    @PostMapping("/{formId}/approve")
    public ResponseEntity<Void> approve(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID formId,
            @Valid @RequestBody DecisionRequest request) {
        service.approve(userId(jwt), formId, request.note());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{formId}/reject")
    public ResponseEntity<Void> reject(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID formId,
            @Valid @RequestBody DecisionRequest request) {
        service.reject(userId(jwt), formId, request.note());
        return ResponseEntity.noContent().build();
    }

    private static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
