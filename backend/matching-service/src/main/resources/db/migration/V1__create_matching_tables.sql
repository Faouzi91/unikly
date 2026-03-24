-- Matching Service schema

CREATE TABLE freelancer_skill_cache (
    user_id         UUID            PRIMARY KEY,
    skills          TEXT[]          NOT NULL DEFAULT '{}',
    hourly_rate     NUMERIC(10, 2),
    average_rating  NUMERIC(3, 2),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX idx_fsc_skills ON freelancer_skill_cache USING GIN (skills);

CREATE TABLE match_results (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id          UUID            NOT NULL,
    freelancer_id   UUID            NOT NULL,
    score           NUMERIC(5, 4)   NOT NULL,
    matched_skills  TEXT[]          NOT NULL DEFAULT '{}',
    strategy        VARCHAR(20)     NOT NULL DEFAULT 'RULE_BASED',
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX idx_match_job_id        ON match_results (job_id);
CREATE INDEX idx_match_freelancer_id ON match_results (freelancer_id);
CREATE INDEX idx_match_score         ON match_results (score DESC);

CREATE TABLE processed_events (
    event_id        UUID            PRIMARY KEY,
    processed_at    TIMESTAMPTZ     NOT NULL DEFAULT now()
);

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
