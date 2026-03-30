-- User Profile Service schema

CREATE TABLE user_profiles (
    id              UUID            PRIMARY KEY,
    display_name    VARCHAR(100)    NOT NULL,
    bio             TEXT,
    avatar_url      VARCHAR(500),
    role            VARCHAR(20)     NOT NULL,
    skills          TEXT[],
    hourly_rate     NUMERIC(10, 2),
    currency        VARCHAR(3),
    location        VARCHAR(255),
    portfolio_links TEXT[],
    average_rating  NUMERIC(3, 2)   NOT NULL DEFAULT 0,
    total_reviews   INT             NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    version         INT             NOT NULL DEFAULT 0
);

CREATE TABLE reviews (
    id          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    reviewer_id UUID            NOT NULL,
    reviewee_id UUID            NOT NULL,
    job_id      UUID            NOT NULL,
    rating      INT             NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment     VARCHAR(2000),
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX idx_reviews_reviewee_id ON reviews (reviewee_id);
CREATE INDEX idx_user_profiles_role ON user_profiles (role);

-- Outbox table for guaranteed event delivery
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
