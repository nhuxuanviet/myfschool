package vn.edu.fpt.myschool.assistant.application;

public record AssistantConversationMessage(Role role, String content) {

    public enum Role {
        USER,
        ASSISTANT
    }
}
