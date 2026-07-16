package vn.edu.fpt.myschool.assistant.application;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AssistantConversationMemory {

    private static final String DEFAULT_CONVERSATION = "default";
    private final JdbcClient jdbcClient;
    private final AssistantRuntimeSettingsStore settingsStore;

    public AssistantConversationMemory(
            JdbcClient jdbcClient,
            AssistantRuntimeSettingsStore settingsStore) {
        this.jdbcClient = jdbcClient;
        this.settingsStore = settingsStore;
    }

    @Transactional(readOnly = true)
    public List<AssistantConversationMessage> history(String authenticatedUserId, String conversationId) {
        int limit = settingsStore.load().memoryMaxMessages();
        return jdbcClient.sql("""
                        SELECT role, content
                        FROM (
                            SELECT id, role, content
                            FROM assistant_conversation_messages
                            WHERE user_id = :userId AND conversation_id = :conversationId
                            ORDER BY id DESC
                            LIMIT :limit
                        ) recent
                        ORDER BY id
                        """)
                .param("userId", userId(authenticatedUserId))
                .param("conversationId", normalizeConversationId(conversationId))
                .param("limit", limit)
                .query((resultSet, rowNumber) -> new AssistantConversationMessage(
                        AssistantConversationMessage.Role.valueOf(resultSet.getString("role")),
                        resultSet.getString("content")))
                .list();
    }

    @Transactional
    public void record(
            String authenticatedUserId,
            String conversationId,
            String userMessage,
            String assistantMessage) {
        UUID userId = userId(authenticatedUserId);
        String normalizedConversationId = normalizeConversationId(conversationId);
        insert(userId, normalizedConversationId, AssistantConversationMessage.Role.USER, userMessage);
        insert(userId, normalizedConversationId, AssistantConversationMessage.Role.ASSISTANT, assistantMessage);

        jdbcClient.sql("""
                        DELETE FROM assistant_conversation_messages
                        WHERE id IN (
                            SELECT id
                            FROM assistant_conversation_messages
                            WHERE user_id = :userId AND conversation_id = :conversationId
                            ORDER BY id DESC
                            OFFSET :maxMessages
                        )
                        """)
                .param("userId", userId)
                .param("conversationId", normalizedConversationId)
                .param("maxMessages", settingsStore.load().memoryMaxMessages())
                .update();
    }

    @Transactional
    public void clear(String authenticatedUserId, String conversationId) {
        jdbcClient.sql("""
                        DELETE FROM assistant_conversation_messages
                        WHERE user_id = :userId AND conversation_id = :conversationId
                        """)
                .param("userId", userId(authenticatedUserId))
                .param("conversationId", normalizeConversationId(conversationId))
                .update();
    }

    private void insert(
            UUID userId,
            String conversationId,
            AssistantConversationMessage.Role role,
            String content) {
        jdbcClient.sql("""
                        INSERT INTO assistant_conversation_messages (
                            user_id, conversation_id, role, content, created_at
                        ) VALUES (
                            :userId, :conversationId, :role, :content, CURRENT_TIMESTAMP
                        )
                        """)
                .param("userId", userId)
                .param("conversationId", conversationId)
                .param("role", role.name())
                .param("content", content)
                .update();
    }

    private static UUID userId(String authenticatedUserId) {
        return UUID.fromString(authenticatedUserId);
    }

    private static String normalizeConversationId(String conversationId) {
        return conversationId == null || conversationId.isBlank()
                ? DEFAULT_CONVERSATION
                : conversationId.trim();
    }
}
