-- Payment Service schema

CREATE TABLE payments (
    id                          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id                      UUID            NOT NULL,
    client_id                   UUID            NOT NULL,
    freelancer_id               UUID            NOT NULL,
    amount                      NUMERIC(12, 2)  NOT NULL CHECK (amount > 0),
    currency                    CHAR(3)         NOT NULL,
    status                      VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    stripe_payment_intent_id    VARCHAR(255)    UNIQUE,
    stripe_transfer_id          VARCHAR(255),
    idempotency_key             VARCHAR(255)    NOT NULL UNIQUE,
    created_at                  TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMPTZ     NOT NULL DEFAULT now(),
    version                     INT             NOT NULL DEFAULT 0
);

CREATE INDEX idx_payments_job_id   ON payments (job_id);
CREATE INDEX idx_payments_status   ON payments (status);
CREATE INDEX idx_payments_client   ON payments (client_id);

CREATE TABLE ledger_entries (
    id          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id  UUID            NOT NULL REFERENCES payments (id),
    entry_type  VARCHAR(30)     NOT NULL,
    amount      NUMERIC(12, 2)  NOT NULL,
    currency    CHAR(3)         NOT NULL,
    description TEXT            NOT NULL,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX idx_ledger_payment_id ON ledger_entries (payment_id);

CREATE TABLE webhook_events (
    id              VARCHAR(255)    PRIMARY KEY,
    event_type      VARCHAR(100)    NOT NULL,
    processed_at    TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE TABLE outbox_events (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type  VARCHAR(100) NOT NULL,
    payload     JSONB       NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    sent_at     TIMESTAMPTZ,
    retry_count INT         NOT NULL DEFAULT 0
);

CREATE INDEX idx_outbox_status ON outbox_events (status) WHERE status = 'PENDING';
