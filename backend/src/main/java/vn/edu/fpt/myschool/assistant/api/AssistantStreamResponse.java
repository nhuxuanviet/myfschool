package vn.edu.fpt.myschool.assistant.api;

import vn.edu.fpt.myschool.assistant.application.AssistantReplyMode;

public record AssistantStreamResponse(
        String type,
        String content,
        AssistantReplyMode mode) {

    static AssistantStreamResponse delta(String content, AssistantReplyMode mode) {
        return new AssistantStreamResponse("delta", content, mode);
    }

    static AssistantStreamResponse done(AssistantReplyMode mode) {
        return new AssistantStreamResponse("done", "", mode);
    }

    static AssistantStreamResponse error(AssistantReplyMode mode) {
        return new AssistantStreamResponse(
                "error",
                "Không thể nhận câu trả lời lúc này. Vui lòng thử lại.",
                mode);
    }
}
