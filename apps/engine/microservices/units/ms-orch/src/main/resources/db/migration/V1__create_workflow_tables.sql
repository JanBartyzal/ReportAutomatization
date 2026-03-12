-- V1: Create workflow orchestration tables

CREATE TABLE IF NOT EXISTS workflow_history (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    file_id         VARCHAR(255) NOT NULL,
    workflow_id     VARCHAR(255) NOT NULL UNIQUE,
    status          VARCHAR(50)  NOT NULL,
    steps_json      JSONB,
    started_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    completed_at    TIMESTAMPTZ,
    org_id          VARCHAR(255) NOT NULL
);

CREATE INDEX idx_workflow_history_file_id     ON workflow_history (file_id);
CREATE INDEX idx_workflow_history_workflow_id  ON workflow_history (workflow_id);
CREATE INDEX idx_workflow_history_org_id       ON workflow_history (org_id);
CREATE INDEX idx_workflow_history_status       ON workflow_history (status);

CREATE TABLE IF NOT EXISTS failed_jobs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    file_id         VARCHAR(255) NOT NULL,
    workflow_id     VARCHAR(255) NOT NULL,
    error_type      VARCHAR(255) NOT NULL,
    error_detail    TEXT,
    failed_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    retry_count     INTEGER      NOT NULL DEFAULT 0,
    org_id          VARCHAR(255) NOT NULL
);

CREATE INDEX idx_failed_jobs_file_id     ON failed_jobs (file_id);
CREATE INDEX idx_failed_jobs_workflow_id ON failed_jobs (workflow_id);
CREATE INDEX idx_failed_jobs_org_id      ON failed_jobs (org_id);
CREATE INDEX idx_failed_jobs_error_type  ON failed_jobs (error_type);
