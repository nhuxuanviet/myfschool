package vn.edu.fpt.myschool.assistant.application;

import org.springframework.stereotype.Component;

@Component
public class LocalAssistantEngineFactory {

    private final StudentAssistantData data;

    LocalAssistantEngineFactory(StudentAssistantData data) {
        this.data = data;
    }

    public AssistantEngine create() {
        return new LocalAssistantEngine(data);
    }
}
