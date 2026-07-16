package vn.edu.fpt.myschool.assistant.api;

import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;

import vn.edu.fpt.myschool.assistant.application.AssistantService;

@RestController
@RequestMapping(path = "/api/v1/assistant", produces = MediaType.APPLICATION_JSON_VALUE)
public class AssistantController {

    private final AssistantService assistantService;

    public AssistantController(AssistantService assistantService) {
        this.assistantService = assistantService;
    }

    @PostMapping(path = "/chat", consumes = MediaType.APPLICATION_JSON_VALUE)
    public AssistantChatResponse chat(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AssistantChatRequest request) {
        return AssistantChatResponse.from(
                assistantService.chat(jwt.getSubject(), request.conversationId(), request.message()));
    }

    @PostMapping(
            path = "/chat/stream",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<AssistantStreamResponse> stream(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AssistantChatRequest request) {
        var mode = assistantService.mode();
        return assistantService.stream(jwt.getSubject(), request.conversationId(), request.message())
                .map(content -> AssistantStreamResponse.delta(content, mode))
                .concatWithValues(AssistantStreamResponse.done(mode))
                .onErrorResume(exception -> Flux.just(AssistantStreamResponse.error(mode)));
    }
}
