-- V2_0_2: ServiceNow Project Sync Configuration
-- Stores sync configuration (RAG thresholds, scope filter) per connection.

CREATE TABLE snow_project_sync_config (
    id                          UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    connection_id               UUID         NOT NULL REFERENCES servicenow_connections(id) ON DELETE CASCADE,
    org_id                      UUID         NOT NULL,
    -- Scope: ALL | ACTIVE_ONLY | BY_MANAGER
    sync_scope                  VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE_ONLY',
    -- Comma-separated manager email filter (used when sync_scope=BY_MANAGER)
    filter_manager_emails       TEXT,
    budget_currency             CHAR(3)      NOT NULL DEFAULT 'CZK',
    -- RAG thresholds (budget utilization %)
    rag_amber_budget_threshold  NUMERIC(5,2) NOT NULL DEFAULT 80.00,
    rag_red_budget_threshold    NUMERIC(5,2) NOT NULL DEFAULT 95.00,
    -- RAG thresholds (schedule variance in days)
    rag_amber_schedule_days     INTEGER      NOT NULL DEFAULT 1,
    rag_red_schedule_days       INTEGER      NOT NULL DEFAULT 14,
    sync_enabled                BOOLEAN      NOT NULL DEFAULT TRUE,
    last_synced_at              TIMESTAMPTZ,
    created_at                  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    -- Only one config per connection
    CONSTRAINT uq_project_sync_connection UNIQUE (connection_id)
);

CREATE INDEX idx_project_sync_connection ON snow_project_sync_config (connection_id);
CREATE INDEX idx_project_sync_org        ON snow_project_sync_config (org_id);

GRANT SELECT, INSERT, UPDATE, DELETE ON snow_project_sync_config TO engine_integrations_user;
