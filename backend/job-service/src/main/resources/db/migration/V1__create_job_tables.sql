-- Job Service schema: jobs, proposals, contracts, outbox_events

-- ─── ENUMs ────────────────────────────────────────────────────────────────────

CREATE TYPE job_status AS ENUM (
    'DRAFT',
    'OPEN',
    'IN_PROGRESS',
    'COMPLETED',
    'CLOSED',
    'CANCELLED',
    'DISPUTED',
    'REFUNDED'
);

CREATE TYPE proposal_status AS ENUM (
    'PENDING',
    'ACCEPTED',
    'REJECTED',
    'WITHDRAWN'
);

-- ─── JOBS ─────────────────────────────────────────────────────────────────────

CREATE TABLE jobs (
    id          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id   UUID            NOT NULL,
    title       VARCHAR(200)    NOT NULL,
    description TEXT            NOT NULL,
    budget      DECIMAL(12, 2)  NOT NULL,
    currency    VARCHAR(3)      NOT NULL DEFAULT 'USD',
    skills      TEXT[]          NOT NULL DEFAULT '{}',
    status      job_status      NOT NULL DEFAULT 'DRAFT',
    version     INT             NOT NULL DEFAULT 1,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_job_status  ON jobs (status);
CREATE INDEX idx_job_client  ON jobs (client_id);
CREATE INDEX idx_job_created ON jobs (created_at DESC);
CREATE INDEX idx_job_budget  ON jobs (budget);
CREATE INDEX idx_job_skills  ON jobs USING GIN (skills);

-- ─── PROPOSALS ────────────────────────────────────────────────────────────────

CREATE TABLE proposals (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id          UUID            NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    freelancer_id   UUID            NOT NULL,
    proposed_budget DECIMAL(12, 2)  NOT NULL,
    cover_letter    TEXT,
    status          proposal_status NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    version         INT             NOT NULL DEFAULT 1,

    CONSTRAINT uq_proposal_job_freelancer UNIQUE (job_id, freelancer_id)
);

CREATE INDEX idx_proposal_job        ON proposals (job_id);
CREATE INDEX idx_proposal_freelancer ON proposals (freelancer_id);
CREATE INDEX idx_proposal_status     ON proposals (status);

-- ─── CONTRACTS ────────────────────────────────────────────────────────────────

CREATE TABLE contracts (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id          UUID            NOT NULL UNIQUE REFERENCES jobs(id),
    client_id       UUID            NOT NULL,
    freelancer_id   UUID            NOT NULL,
    agreed_budget   DECIMAL(12, 2)  NOT NULL,
    terms           TEXT,
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    started_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    completed_at    TIMESTAMPTZ
);

-- ─── OUTBOX EVENTS ────────────────────────────────────────────────────────────

CREATE TABLE outbox_events (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type      VARCHAR(100)    NOT NULL,
    aggregate_id    UUID            NOT NULL,
    aggregate_type  VARCHAR(50)     NOT NULL,
    payload         JSONB           NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING'
                                    CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    sent_at         TIMESTAMPTZ,
    retry_count     INT             NOT NULL DEFAULT 0
);

CREATE INDEX idx_outbox_pending ON outbox_events (created_at) WHERE status = 'PENDING';
CREATE INDEX idx_outbox_created ON outbox_events (created_at);
