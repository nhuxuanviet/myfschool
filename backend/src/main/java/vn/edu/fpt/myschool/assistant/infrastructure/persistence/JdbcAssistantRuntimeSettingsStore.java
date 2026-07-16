package vn.edu.fpt.myschool.assistant.infrastructure.persistence;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import vn.edu.fpt.myschool.assistant.application.AssistantRuntimeSettings;
import vn.edu.fpt.myschool.assistant.application.AssistantRuntimeSettingsStore;

@Repository
public class JdbcAssistantRuntimeSettingsStore implements AssistantRuntimeSettingsStore {

    private final JdbcClient jdbcClient;

    public JdbcAssistantRuntimeSettingsStore(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public AssistantRuntimeSettings load() {
        return jdbcClient.sql("""
                        SELECT model, temperature, max_completion_tokens,
                               memory_max_messages, version, updated_at
                        FROM assistant_runtime_settings
                        WHERE id = 1
                        """)
                .query((resultSet, rowNumber) -> new AssistantRuntimeSettings(
                        resultSet.getString("model"),
                        resultSet.getBigDecimal("temperature"),
                        resultSet.getInt("max_completion_tokens"),
                        resultSet.getInt("memory_max_messages"),
                        resultSet.getLong("version"),
                        resultSet.getObject("updated_at", OffsetDateTime.class).toInstant()))
                .single();
    }

    @Override
    public boolean update(
            String model,
            BigDecimal temperature,
            int maxCompletionTokens,
            int memoryMaxMessages,
            long version,
            Instant updatedAt) {
        return jdbcClient.sql("""
                        UPDATE assistant_runtime_settings
                        SET model = :model,
                            temperature = :temperature,
                            max_completion_tokens = :maxCompletionTokens,
                            memory_max_messages = :memoryMaxMessages,
                            version = version + 1,
                            updated_at = :updatedAt
                        WHERE id = 1 AND version = :version
                        """)
                .param("model", model)
                .param("temperature", temperature)
                .param("maxCompletionTokens", maxCompletionTokens)
                .param("memoryMaxMessages", memoryMaxMessages)
                .param("updatedAt", Timestamp.from(updatedAt))
                .param("version", version)
                .update() == 1;
    }
}
