package vn.edu.fpt.myschool.assistant.application;

import java.math.BigDecimal;
import java.time.Instant;

public interface AssistantRuntimeSettingsStore {

    AssistantRuntimeSettings load();

    boolean update(
            String model,
            BigDecimal temperature,
            int maxCompletionTokens,
            int memoryMaxMessages,
            long version,
            Instant updatedAt);
}
