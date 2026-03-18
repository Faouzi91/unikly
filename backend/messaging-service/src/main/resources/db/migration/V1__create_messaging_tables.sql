-- Messaging Service schema

CREATE TABLE conversations (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id           UUID,
    participant_ids  UUID[]        NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    last_message_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_conversations_participant ON conversations USING GIN (participant_ids);
CREATE INDEX idx_conversations_job_id      ON conversations (job_id);
CREATE INDEX idx_conversations_last_msg    ON conversations (last_message_at DESC);

CREATE TABLE messages (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id  UUID         NOT NULL REFERENCES conversations (id) ON DELETE CASCADE,
    sender_id        UUID         NOT NULL,
    content          TEXT         NOT NULL,
    content_type     VARCHAR(20)  NOT NULL DEFAULT 'TEXT',
    read_at          TIMESTAMPTZ,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_messages_conversation_id ON messages (conversation_id);
CREATE INDEX idx_messages_created_at      ON messages (created_at);

CREATE TABLE outbox_events (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type  VARCHAR(100) NOT NULL,
    aggregate_id    UUID         NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    payload         TEXT         NOT NULL,
    published       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_outbox_published ON outbox_events (published) WHERE published = FALSE;
