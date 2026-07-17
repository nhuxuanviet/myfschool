package vn.edu.fpt.myschool.parent.api;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.fpt.myschool.grades.api.GradesResponse;
import vn.edu.fpt.myschool.parent.application.ParentService;
import vn.edu.fpt.myschool.parent.domain.ParentChild;

@RestController
@RequestMapping("/api/v1/parent")
@Tag(name = "Parent", description = "A guardian's own children")
@SecurityRequirement(name = "bearerAuth")
public class ParentController {

    private final ParentService service;

    public ParentController(ParentService service) {
        this.service = service;
    }

    public record ChildResponse(
            UUID studentId,
            String studentCode,
            String fullName,
            int gradeLevel,
            String className,
            String relationship,
            int contactOrder) {

        static ChildResponse from(ParentChild child) {
            return new ChildResponse(
                    child.studentId(), child.studentCode(), child.fullName(), child.gradeLevel(),
                    child.className(), child.relationship(), child.contactOrder());
        }
    }

    /** The guardian picks a child from here; every later call re-checks the link anyway. */
    @GetMapping("/children")
    public List<ChildResponse> getChildren(@AuthenticationPrincipal Jwt jwt) {
        return service.getChildren(userId(jwt)).stream().map(ChildResponse::from).toList();
    }

    @GetMapping("/children/{studentId}/grades")
    public GradesResponse getChildGrades(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID studentId,
            @RequestParam(required = false) UUID termId) {
        return GradesResponse.from(service.getChildGrades(userId(jwt), studentId, termId));
    }

    private static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
