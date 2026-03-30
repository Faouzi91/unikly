-- Outbox pattern table for notification-service

CREATE TABLE outbox_events (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type      VARCHAR(100)    NOT NULL,
    aggregate_id    UUID            NOT NULL,
    aggregate_type  VARCHAR(50)     NOT NULL,
    payload         JSONB           NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    sent_at         TIMESTAMPTZ,
    retry_count     INT             NOT NULL DEFAULT 0
);

CREATE INDEX idx_outbox_events_status ON outbox_events (status);
