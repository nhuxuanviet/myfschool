package vn.edu.fpt.myschool.events.api;

import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.fpt.myschool.events.application.EventsService;
import vn.edu.fpt.myschool.events.domain.EventCategory;
import vn.edu.fpt.myschool.events.domain.EventRegistrationResult;

@RestController
@RequestMapping("/api/v1/events")
@Tag(name = "Events", description = "Authenticated student school events")
@SecurityRequirement(name = "bearerAuth")
public class EventsController {

    private final EventsService eventsService;

    public EventsController(EventsService eventsService) {
        this.eventsService = eventsService;
    }

    @GetMapping
    @Operation(summary = "List school events visible to the authenticated student")
    public EventsResponse getEvents(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(
                            in = ParameterIn.QUERY,
                            description = "Optional event category")
                    @RequestParam(required = false)
                    EventCategory category,
            @Parameter(
                            in = ParameterIn.QUERY,
                            description = "Include completed events after active and upcoming events")
                    @RequestParam(defaultValue = "false")
                    boolean includePast) {
        return EventsResponse.from(eventsService.getEvents(
                jwt.getSubject(), category, includePast));
    }

    @GetMapping("/{eventId}")
    @Operation(summary = "Get a school event visible to the authenticated student")
    public EventResponse getEvent(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID eventId) {
        return EventResponse.from(eventsService.getEvent(jwt.getSubject(), eventId));
    }

    @PostMapping("/{eventId}/registrations")
    @Operation(summary = "Register the authenticated student for an event")
    public ResponseEntity<EventResponse> register(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID eventId) {
        EventRegistrationResult result = eventsService.register(jwt.getSubject(), eventId);
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(EventResponse.from(result.event()));
    }

    @DeleteMapping("/{eventId}/registrations")
    @Operation(summary = "Cancel the authenticated student's event registration")
    public EventResponse cancelRegistration(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID eventId) {
        return EventResponse.from(eventsService.cancelRegistration(jwt.getSubject(), eventId));
    }
}
