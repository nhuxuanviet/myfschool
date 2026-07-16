package vn.edu.fpt.myschool.admin.ai.api;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.fpt.myschool.admin.ai.application.AdminAiSettingsService;
import vn.edu.fpt.myschool.admin.ai.domain.AdminAiConfiguration;

@RestController
@RequestMapping("/api/v1/admin/ai/settings")
@Tag(name = "Admin AI settings", description = "Safe runtime settings and provider readiness")
@SecurityRequirement(name = "bearerAuth")
public class AdminAiSettingsController {

    private final AdminAiSettingsService service;

    public AdminAiSettingsController(AdminAiSettingsService service) {
        this.service = service;
    }

    @GetMapping
    public AdminAiConfiguration getConfiguration() {
        return service.getConfiguration();
    }

    @PutMapping
    public AdminAiConfiguration updateConfiguration(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateAiSettingsRequest request) {
        return service.update(
                UUID.fromString(jwt.getSubject()),
                request.model(),
                request.temperature(),
                request.maxCompletionTokens(),
                request.memoryMaxMessages(),
                request.version());
    }

    public record UpdateAiSettingsRequest(
            @NotBlank
            @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._:-]{2,79}")
            String model,
            @NotNull @DecimalMin("0.0") @DecimalMax("2.0") BigDecimal temperature,
            @Min(100) @Max(4_000) int maxCompletionTokens,
            @Min(2) @Max(30) int memoryMaxMessages,
            @Min(0) long version) {
    }
}
