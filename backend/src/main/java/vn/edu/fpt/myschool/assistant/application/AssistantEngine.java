package vn.edu.fpt.myschool.assistant.application;

import reactor.core.publisher.Flux;

import java.util.List;

public interface AssistantEngine {
    AssistantReply answer(
            String authenticatedUserId,
            String message,
            List<AssistantConversationMessage> history);

    Flux<String> stream(
            String authenticatedUserId,
            String message,
            List<AssistantConversationMessage> history);

    AssistantReplyMode mode();
}
