package vn.edu.fpt.myschool.forms.api;

import java.util.UUID;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.fpt.myschool.forms.application.StudentFormsService;
import vn.edu.fpt.myschool.forms.domain.StudentFormStatus;

@RestController
@RequestMapping("/api/v1/forms")
@Tag(name = "Student Forms", description = "Authenticated student request forms")
@SecurityRequirement(name = "bearerAuth")
public class StudentFormsController {

    private final StudentFormsService formsService;

    public StudentFormsController(StudentFormsService formsService) {
        this.formsService = formsService;
    }

    @GetMapping
    @Operation(summary = "List forms owned by the authenticated student")
    public StudentFormsResponse getForms(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) StudentFormStatus status) {
        return StudentFormsResponse.from(formsService.getForms(jwt.getSubject(), status));
    }

    @GetMapping("/{formId}")
    @Operation(summary = "Get an owned form and its status timeline")
    public StudentFormDetailsResponse getForm(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID formId) {
        return StudentFormDetailsResponse.from(formsService.getForm(jwt.getSubject(), formId));
    }

    @PostMapping
    @Operation(summary = "Submit a new student form")
    public ResponseEntity<StudentFormDetailsResponse> createForm(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateStudentFormRequest request) {
        StudentFormDetailsResponse response = StudentFormDetailsResponse.from(
                formsService.createForm(
                        jwt.getSubject(),
                        request.type(),
                        request.reason(),
                        request.startsOn(),
                        request.endsOn()));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{formId}")
    @Operation(summary = "Cancel an owned pending student form")
    public StudentFormDetailsResponse cancelForm(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID formId) {
        return StudentFormDetailsResponse.from(formsService.cancelForm(jwt.getSubject(), formId));
    }
}
