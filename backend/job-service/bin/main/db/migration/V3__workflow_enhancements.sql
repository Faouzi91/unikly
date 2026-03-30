-- Flyway: non-transactional
-- ALTER TYPE ... ADD VALUE requires a non-transactional context in PostgreSQL.

-- ─── JOB STATUS: add IN_REVIEW between OPEN and IN_PROGRESS ──────────────────
-- New lifecycle: DRAFT → OPEN → IN_REVIEW → IN_PROGRESS → COMPLETED → CLOSED

ALTER TYPE job_status ADD VALUE IF NOT EXISTS 'IN_REVIEW' AFTER 'OPEN';

-- ─── PROPOSAL STATUS: richer workflow values ───────────────────────────────────
-- New states:
--   SUBMITTED   – replaces PENDING for newly created proposals
--   VIEWED      – client has opened the proposal
--   SHORTLISTED – client has shortlisted the proposal
--   OUTDATED    – proposal was made on a previous job version
--   NEEDS_REVIEW – proposal requires client re-evaluation after a job edit
-- PENDING is kept for backward compatibility.

ALTER TYPE proposal_status ADD VALUE IF NOT EXISTS 'SUBMITTED'    BEFORE 'PENDING';
ALTER TYPE proposal_status ADD VALUE IF NOT EXISTS 'VIEWED'       AFTER  'SUBMITTED';
ALTER TYPE proposal_status ADD VALUE IF NOT EXISTS 'SHORTLISTED'  AFTER  'VIEWED';
ALTER TYPE proposal_status ADD VALUE IF NOT EXISTS 'OUTDATED'     AFTER  'WITHDRAWN';
ALTER TYPE proposal_status ADD VALUE IF NOT EXISTS 'NEEDS_REVIEW' AFTER  'OUTDATED';

-- ─── PROPOSALS: job version tracking ──────────────────────────────────────────
-- Tracks which version of the job a proposal was submitted against.
-- Allows the service to mark existing proposals as OUTDATED when a job is edited.

ALTER TABLE proposals
    ADD COLUMN IF NOT EXISTS job_version INT NOT NULL DEFAULT 1;

CREATE INDEX IF NOT EXISTS idx_proposal_job_version
    ON proposals (job_id, job_version);
