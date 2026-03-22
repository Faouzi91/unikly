-- Notification Service schema

CREATE TABLE notifications (
    id          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID            NOT NULL,
    type        VARCHAR(30)     NOT NULL,
    title       VARCHAR(200)    NOT NULL,
    body        TEXT            NOT NULL,
    read        BOOLEAN         NOT NULL DEFAULT FALSE,
    action_url  VARCHAR(500),
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX idx_notification_user_id   ON notifications (user_id);
CREATE INDEX idx_notification_unread    ON notifications (user_id, read) WHERE read = FALSE;
CREATE INDEX idx_notification_created   ON notifications (created_at DESC);

CREATE TABLE notification_preferences (
    user_id             UUID        PRIMARY KEY,
    email_enabled       BOOLEAN     NOT NULL DEFAULT TRUE,
    push_enabled        BOOLEAN     NOT NULL DEFAULT TRUE,
    realtime_enabled    BOOLEAN     NOT NULL DEFAULT TRUE,
    quiet_hours_start   TIME,
    quiet_hours_end     TIME
);

CREATE TABLE job_client_cache (
    job_id          UUID            PRIMARY KEY,
    client_id       UUID            NOT NULL,
    title           VARCHAR(200)    NOT NULL,
    freelancer_id   UUID
);

CREATE TABLE processed_events (
    event_id        UUID        PRIMARY KEY,
    processed_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
