package vn.edu.fpt.myschool.assistant.application;

import java.time.Clock;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import vn.edu.fpt.myschool.shared.error.ApiException;

@Service
public class AssistantService {

    private final AssistantEngine engine;
    private final AssistantConversationMemory memory;
    private final AssistantRateLimitStore rateLimitStore;
    private final AssistantUsageProperties usageProperties;
    private final Clock clock;

    public AssistantService(
            AssistantEngine engine,
            AssistantConversationMemory memory,
            AssistantRateLimitStore rateLimitStore,
            AssistantUsageProperties usageProperties,
            Clock clock) {
        this.engine = engine;
        this.memory = memory;
        this.rateLimitStore = rateLimitStore;
        this.usageProperties = usageProperties;
        this.clock = clock;
    }

    public AssistantReply chat(String authenticatedUserId, String message) {
        return chat(authenticatedUserId, null, message);
    }

    public AssistantReply chat(String authenticatedUserId, String conversationId, String message) {
        acquirePermit(authenticatedUserId);
        String normalizedMessage = message.trim();
        AssistantReply reply = engine.answer(
                authenticatedUserId,
                normalizedMessage,
                memory.history(authenticatedUserId, conversationId));
        memory.record(authenticatedUserId, conversationId, normalizedMessage, reply.answer());
        return reply;
    }

    public Flux<String> stream(String authenticatedUserId, String message) {
        return stream(authenticatedUserId, null, message);
    }

    public Flux<String> stream(String authenticatedUserId, String conversationId, String message) {
        acquirePermit(authenticatedUserId);
        String normalizedMessage = message.trim();
        StringBuilder answer = new StringBuilder();
        return engine.stream(
                        authenticatedUserId,
                        normalizedMessage,
                        memory.history(authenticatedUserId, conversationId))
                .doOnNext(answer::append)
                .doOnComplete(() -> memory.record(
                        authenticatedUserId,
                        conversationId,
                        normalizedMessage,
                        answer.toString()));
    }

    public AssistantReplyMode mode() {
        return engine.mode();
    }

    private void acquirePermit(String authenticatedUserId) {
        boolean acquired = rateLimitStore.tryAcquire(
                UUID.fromString(authenticatedUserId),
                clock.instant(),
                usageProperties.window(),
                usageProperties.maxRequests());
        if (!acquired) {
            throw new ApiException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "ASSISTANT_RATE_LIMITED",
                    "Bạn đã gửi quá nhiều câu hỏi. Vui lòng thử lại sau ít phút");
        }
    }
}
