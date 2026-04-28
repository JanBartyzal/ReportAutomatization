-- V2_0_1: ServiceNow ITSM – Resolver Group Configuration
-- Stores which SN assignment_groups to monitor per connection (Incidents, Requests, Tasks).

CREATE TABLE snow_resolver_groups (
    id              UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    connection_id   UUID         NOT NULL REFERENCES servicenow_connections(id) ON DELETE CASCADE,
    org_id          UUID         NOT NULL,
    group_sys_id    VARCHAR(32)  NOT NULL,
    group_name      VARCHAR(255) NOT NULL,
    -- JSONB array: ["INCIDENT","REQUEST","TASK"]
    data_types      JSONB        NOT NULL DEFAULT '["INCIDENT"]',
    sync_enabled    BOOLEAN      NOT NULL DEFAULT TRUE,
    last_synced_at  TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_resolver_group_conn_sys UNIQUE (connection_id, group_sys_id)
);

CREATE INDEX idx_resolver_groups_connection ON snow_resolver_groups (connection_id);
CREATE INDEX idx_resolver_groups_org        ON snow_resolver_groups (org_id);
CREATE INDEX idx_resolver_groups_enabled    ON snow_resolver_groups (sync_enabled);

GRANT SELECT, INSERT, UPDATE, DELETE ON snow_resolver_groups TO engine_integrations_user;
