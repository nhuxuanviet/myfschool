package vn.edu.fpt.myschool.assistant.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AssistantChatRequest(
        @NotBlank(message = "Message is required")
        @Size(max = 500, message = "Message must not exceed 500 characters")
        String message,
        @Size(max = 64, message = "Conversation ID must not exceed 64 characters")
        @Pattern(
                regexp = "[A-Za-z0-9_-]+",
                message = "Conversation ID contains unsupported characters")
        String conversationId) {}
