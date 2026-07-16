package vn.edu.fpt.myschool.grades.api;

import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.fpt.myschool.grades.application.GradesService;

@RestController
@RequestMapping("/api/v1/grades")
@Tag(name = "Grades", description = "Authenticated student semester grades")
@SecurityRequirement(name = "bearerAuth")
public class GradesController {

    private final GradesService gradesService;

    public GradesController(GradesService gradesService) {
        this.gradesService = gradesService;
    }

    @GetMapping
    @Operation(summary = "Get the authenticated student's semester grades")
    public GradesResponse getGrades(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(
                            in = ParameterIn.QUERY,
                            description = "Semester UUID available to the authenticated student")
                    @RequestParam(required = false)
                    UUID termId) {
        return GradesResponse.from(gradesService.getSemesterGrades(jwt.getSubject(), termId));
    }
}
