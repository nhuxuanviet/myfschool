CREATE TABLE assistant_conversation_messages (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id UUID NOT NULL,
    conversation_id VARCHAR(64) NOT NULL,
    role VARCHAR(16) NOT NULL,
    content VARCHAR(20000) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_assistant_conversation_messages_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_assistant_conversation_messages_role
        CHECK (role IN ('USER', 'ASSISTANT')),
    CONSTRAINT ck_assistant_conversation_messages_content
        CHECK (btrim(content) <> '')
);

CREATE INDEX idx_assistant_conversation_messages_lookup
    ON assistant_conversation_messages (user_id, conversation_id, id DESC);
