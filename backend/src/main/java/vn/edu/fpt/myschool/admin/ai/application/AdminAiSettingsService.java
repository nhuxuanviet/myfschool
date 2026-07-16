package vn.edu.fpt.myschool.admin.ai.application;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.fpt.myschool.admin.ai.domain.AdminAiConfiguration;
import vn.edu.fpt.myschool.admin.operations.application.port.AdminOperationsStore;
import vn.edu.fpt.myschool.assistant.application.AssistantRuntimeSettings;
import vn.edu.fpt.myschool.assistant.application.AssistantRuntimeSettingsStore;
import vn.edu.fpt.myschool.shared.error.ApiErrorCode;
import vn.edu.fpt.myschool.shared.error.ApiException;

@Service
@PreAuthorize("hasRole('ADMIN')")
public class AdminAiSettingsService {

    private static final UUID SETTINGS_ENTITY_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final Pattern MODEL_PATTERN =
            Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:-]{2,79}");

    private final AssistantRuntimeSettingsStore settingsStore;
    private final AdminOperationsStore adminOperationsStore;
    private final Environment environment;
    private final Clock clock;

    public AdminAiSettingsService(
            AssistantRuntimeSettingsStore settingsStore,
            AdminOperationsStore adminOperationsStore,
            Environment environment,
            Clock clock) {
        this.settingsStore = settingsStore;
        this.adminOperationsStore = adminOperationsStore;
        this.environment = environment;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AdminAiConfiguration getConfiguration() {
        return toConfiguration(settingsStore.load());
    }

    @Transactional
    public AdminAiConfiguration update(
            UUID actorId,
            String model,
            BigDecimal temperature,
            int maxCompletionTokens,
            int memoryMaxMessages,
            long version) {
        String normalizedModel = model == null ? "" : model.trim();
        validate(normalizedModel, temperature, maxCompletionTokens, memoryMaxMessages);
        var now = clock.instant();
        boolean updated = settingsStore.update(
                normalizedModel,
                temperature,
                maxCompletionTokens,
                memoryMaxMessages,
                version,
                now);
        if (!updated) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    ApiErrorCode.CONFLICT.name(),
                    "Cấu hình AI đã được quản trị viên khác thay đổi");
        }
        adminOperationsStore.audit(
                actorId,
                "UPDATE",
                "ASSISTANT_SETTINGS",
                SETTINGS_ENTITY_ID,
                changedFields(normalizedModel, temperature, maxCompletionTokens, memoryMaxMessages),
                now);
        return toConfiguration(settingsStore.load());
    }

    private AdminAiConfiguration toConfiguration(AssistantRuntimeSettings settings) {
        String provider = environment.getProperty("app.assistant.provider", "local")
                .trim()
                .toUpperCase(java.util.Locale.ROOT);
        boolean apiKeyConfigured = !environment
                .getProperty("spring.ai.openai.api-key", "")
                .isBlank();
        String status = switch (provider) {
            case "OPENAI" -> apiKeyConfigured ? "READY" : "MISSING_API_KEY";
            case "LOCAL" -> "LOCAL_FALLBACK";
            default -> "UNSUPPORTED_PROVIDER";
        };
        return new AdminAiConfiguration(
                provider,
                status,
                apiKeyConfigured,
                settings.model(),
                settings.temperature(),
                settings.maxCompletionTokens(),
                settings.memoryMaxMessages(),
                settings.version(),
                settings.updatedAt());
    }

    private static void validate(
            String model,
            BigDecimal temperature,
            int maxCompletionTokens,
            int memoryMaxMessages) {
        if (!MODEL_PATTERN.matcher(model).matches()) {
            throw invalid("Model AI không hợp lệ");
        }
        if (temperature == null
                || temperature.compareTo(BigDecimal.ZERO) < 0
                || temperature.compareTo(BigDecimal.valueOf(2)) > 0) {
            throw invalid("Temperature phải nằm trong khoảng 0 đến 2");
        }
        if (maxCompletionTokens < 100 || maxCompletionTokens > 4_000) {
            throw invalid("Số token phản hồi phải nằm trong khoảng 100 đến 4000");
        }
        if (memoryMaxMessages < 2 || memoryMaxMessages > 30) {
            throw invalid("Bộ nhớ hội thoại phải nằm trong khoảng 2 đến 30 tin nhắn");
        }
    }

    private String changedFields(
            String model,
            BigDecimal temperature,
            int maxCompletionTokens,
            int memoryMaxMessages) {
        return """
                {"model":"%s","temperature":%s,"maxCompletionTokens":%d,"memoryMaxMessages":%d}
                """.formatted(
                        model,
                        temperature.toPlainString(),
                        maxCompletionTokens,
                        memoryMaxMessages).strip();
    }

    private static ApiException invalid(String message) {
        return new ApiException(
                HttpStatus.BAD_REQUEST,
                ApiErrorCode.VALIDATION_FAILED.name(),
                message);
    }
}
