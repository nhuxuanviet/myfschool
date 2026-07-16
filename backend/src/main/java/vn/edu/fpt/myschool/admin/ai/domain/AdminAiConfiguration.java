package vn.edu.fpt.myschool.admin.ai.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record AdminAiConfiguration(
        String provider,
        String status,
        boolean apiKeyConfigured,
        String model,
        BigDecimal temperature,
        int maxCompletionTokens,
        int memoryMaxMessages,
        long version,
        Instant updatedAt) {
}
