package vn.edu.fpt.myschool.assistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.time.Clock;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import vn.edu.fpt.myschool.assistant.application.AssistantEngine;
import vn.edu.fpt.myschool.assistant.application.AssistantConversationMemory;
import vn.edu.fpt.myschool.assistant.application.AssistantConversationMessage;
import vn.edu.fpt.myschool.assistant.application.AssistantReply;
import vn.edu.fpt.myschool.assistant.application.AssistantReplyMode;
import vn.edu.fpt.myschool.assistant.application.AssistantUsageProperties;
import vn.edu.fpt.myschool.assistant.application.AssistantService;

class AssistantServiceTest {

    @Test
    void preservesWhitespaceOnlyStreamingDeltas() {
        AssistantConversationMemory memory = mock(AssistantConversationMemory.class);
        when(memory.history(anyString(), anyString())).thenReturn(List.of());
        AssistantService service = new AssistantService(
                new WhitespaceStreamingEngine(),
                memory,
                (userId, requestedAt, window, maxRequests) -> true,
                new AssistantUsageProperties(Duration.ofMinutes(1), 20),
                Clock.systemUTC());

        List<String> deltas = service.stream(
                "00000000-0000-0000-0000-000000000001",
                " question ").collectList().block();

        assertThat(deltas).containsExactly("Câu", " ", "trả lời");
    }

    private static final class WhitespaceStreamingEngine implements AssistantEngine {

        @Override
        public AssistantReply answer(
                String authenticatedUserId,
                String message,
                List<AssistantConversationMessage> history) {
            return new AssistantReply("Câu trả lời", mode());
        }

        @Override
        public Flux<String> stream(
                String authenticatedUserId,
                String message,
                List<AssistantConversationMessage> history) {
            return Flux.just("Câu", " ", "trả lời");
        }

        @Override
        public AssistantReplyMode mode() {
            return AssistantReplyMode.OPENAI;
        }
    }
}
