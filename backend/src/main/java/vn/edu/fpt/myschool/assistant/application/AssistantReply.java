package vn.edu.fpt.myschool.assistant.application;

import java.util.Objects;

public record AssistantReply(String answer, AssistantReplyMode mode) {

    public AssistantReply {
        if (answer == null || answer.isBlank()) {
            throw new IllegalArgumentException("answer must not be blank");
        }
        answer = answer.trim();
        Objects.requireNonNull(mode, "mode must not be null");
    }
}
