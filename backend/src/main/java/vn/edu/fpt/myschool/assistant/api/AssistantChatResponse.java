package vn.edu.fpt.myschool.assistant.api;

import vn.edu.fpt.myschool.assistant.application.AssistantReply;
import vn.edu.fpt.myschool.assistant.application.AssistantReplyMode;

public record AssistantChatResponse(String answer, AssistantReplyMode mode) {

    static AssistantChatResponse from(AssistantReply reply) {
        return new AssistantChatResponse(reply.answer(), reply.mode());
    }
}
