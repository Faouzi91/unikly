-- Job Service schema

CREATE TABLE jobs (
    id          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id   UUID            NOT NULL,
    title       VARCHAR(200)    NOT NULL,
    description TEXT            NOT NULL,
    budget      NUMERIC(10, 2)  NOT NULL,
    currency    VARCHAR(3)      NOT NULL,
    skills      TEXT[]          NOT NULL,
    status      VARCHAR(20)     NOT NULL DEFAULT 'DRAFT',
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT now(),
    version     INT             NOT NULL DEFAULT 0
);

CREATE INDEX idx_job_status ON jobs (status);
CREATE INDEX idx_job_client ON jobs (client_id);
CREATE INDEX idx_job_created ON jobs (created_at DESC);
CREATE INDEX idx_job_budget ON jobs (budget);
CREATE INDEX idx_job_skills ON jobs USING GIN (skills);

CREATE TABLE proposals (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id          UUID            NOT NULL REFERENCES jobs(id),
    freelancer_id   UUID            NOT NULL,
    proposed_budget NUMERIC(10, 2)  NOT NULL,
    cover_letter    VARCHAR(5000),
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    version         INT             NOT NULL DEFAULT 0
);

CREATE INDEX idx_proposal_job_id ON proposals (job_id);

CREATE TABLE contracts (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id          UUID            NOT NULL UNIQUE REFERENCES jobs(id),
    client_id       UUID            NOT NULL,
    freelancer_id   UUID            NOT NULL,
    agreed_budget   NUMERIC(10, 2)  NOT NULL,
    terms           TEXT,
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    started_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    completed_at    TIMESTAMPTZ
);

-- Outbox table for guaranteed event delivery
CREATE TABLE outbox_events (
    id          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type  VARCHAR(100)    NOT NULL,
    payload     JSONB           NOT NULL,
    status      VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT now(),
    sent_at     TIMESTAMPTZ,
    retry_count INT             NOT NULL DEFAULT 0
);

CREATE INDEX idx_outbox_events_status ON outbox_events (status);
