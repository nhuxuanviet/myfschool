package vn.edu.fpt.myschool.timetable.api;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.fpt.myschool.timetable.application.TimetableService;

@RestController
@RequestMapping("/api/v1/timetable")
@Tag(name = "Timetable", description = "Authenticated student weekly timetable")
@SecurityRequirement(name = "bearerAuth")
public class TimetableController {

    private final TimetableService timetableService;

    public TimetableController(TimetableService timetableService) {
        this.timetableService = timetableService;
    }

    @GetMapping
    @Operation(summary = "Get the authenticated student's weekly timetable")
    public TimetableResponse getTimetable(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(
                            in = ParameterIn.QUERY,
                            description = "ISO-8601 Monday that starts the requested week",
                            example = "2026-07-06")
                    @RequestParam(required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate weekStart) {
        return TimetableResponse.from(timetableService.getTimetable(jwt.getSubject(), weekStart));
    }
}
