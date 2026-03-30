CREATE TABLE invitations (
    id              UUID PRIMARY KEY,
    job_id          UUID        NOT NULL,
    client_id       UUID        NOT NULL,
    freelancer_id   UUID        NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    message         TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_invitation_job_freelancer UNIQUE (job_id, freelancer_id)
);

CREATE INDEX idx_invitations_freelancer ON invitations (freelancer_id);
CREATE INDEX idx_invitations_job        ON invitations (job_id);
