CREATE TABLE assistant_runtime_settings (
    id SMALLINT PRIMARY KEY,
    model VARCHAR(80) NOT NULL,
    temperature NUMERIC(2, 1) NOT NULL,
    max_completion_tokens INTEGER NOT NULL,
    memory_max_messages SMALLINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_assistant_runtime_settings_singleton CHECK (id = 1),
    CONSTRAINT ck_assistant_runtime_settings_model CHECK (btrim(model) <> ''),
    CONSTRAINT ck_assistant_runtime_settings_temperature CHECK (temperature BETWEEN 0.0 AND 2.0),
    CONSTRAINT ck_assistant_runtime_settings_max_tokens CHECK (max_completion_tokens BETWEEN 100 AND 4000),
    CONSTRAINT ck_assistant_runtime_settings_memory CHECK (memory_max_messages BETWEEN 2 AND 30),
    CONSTRAINT ck_assistant_runtime_settings_version CHECK (version >= 0)
);

INSERT INTO assistant_runtime_settings (
    id,
    model,
    temperature,
    max_completion_tokens,
    memory_max_messages,
    updated_at
) VALUES (
    1,
    'gpt-5.6-luna',
    0.6,
    800,
    12,
    CURRENT_TIMESTAMP
);
