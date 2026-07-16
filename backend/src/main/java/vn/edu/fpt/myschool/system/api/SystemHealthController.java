package vn.edu.fpt.myschool.system.api;

import java.time.Clock;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
@Tag(name = "System", description = "System-level API operations")
public class SystemHealthController {

    private final HealthEndpoint healthEndpoint;
    private final Clock clock;

    public SystemHealthController(HealthEndpoint healthEndpoint, Clock clock) {
        this.healthEndpoint = healthEndpoint;
        this.clock = clock;
    }

    @GetMapping("/health")
    @Operation(summary = "Get the aggregate application health")
    public ResponseEntity<SystemHealthResponse> health() {
        Status aggregateStatus = healthEndpoint.health().getStatus();
        HttpStatus httpStatus = Status.UP.equals(aggregateStatus)
                ? HttpStatus.OK
                : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(httpStatus)
                .body(new SystemHealthResponse(aggregateStatus.getCode(), clock.instant()));
    }
}
