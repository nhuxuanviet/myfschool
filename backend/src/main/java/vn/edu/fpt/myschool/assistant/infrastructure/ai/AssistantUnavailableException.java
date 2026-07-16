package vn.edu.fpt.myschool.assistant.infrastructure.ai;

import org.springframework.http.HttpStatus;

import vn.edu.fpt.myschool.shared.error.ApiException;

final class AssistantUnavailableException extends ApiException {

    private AssistantUnavailableException() {
        super(
                HttpStatus.SERVICE_UNAVAILABLE,
                "ASSISTANT_UNAVAILABLE",
                "The assistant is temporarily unavailable");
    }

    static AssistantUnavailableException create() {
        return new AssistantUnavailableException();
    }
}
