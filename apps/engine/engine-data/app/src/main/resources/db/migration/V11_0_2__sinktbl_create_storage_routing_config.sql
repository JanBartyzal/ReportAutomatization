-- V11_0_2: Storage routing configuration table
-- Enables per-org and per-source-type routing between POSTGRES and SPARK backends.
-- Rows are evaluated with specificity: org+source > org-only > source-only > global default.

CREATE TABLE storage_routing_config (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    org_id          UUID,                        -- NULL = applies to all orgs
    source_type     VARCHAR(50),                 -- NULL = applies to all source types
                                                 -- e.g. EXCEL, PPTX, CSV, SERVICE_NOW
    backend         VARCHAR(20) NOT NULL         -- POSTGRES | SPARK
                    CHECK (backend IN ('POSTGRES', 'SPARK')),
    effective_from  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(255),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_routing_org_source UNIQUE (org_id, source_type)
);

-- Most common lookup: resolve backend for a specific org + source combination
CREATE INDEX idx_routing_org_source ON storage_routing_config (org_id, source_type);

-- Allow querying all rules that apply to a given org
CREATE INDEX idx_routing_org_id ON storage_routing_config (org_id);

COMMENT ON TABLE  storage_routing_config IS
    'Determines which storage backend (POSTGRES or SPARK) receives table data for a given org/source-type combination';
COMMENT ON COLUMN storage_routing_config.org_id IS
    'NULL means the rule applies to all organisations';
COMMENT ON COLUMN storage_routing_config.source_type IS
    'NULL means the rule applies to all source types (EXCEL, PPTX, CSV, SERVICE_NOW, …)';
COMMENT ON COLUMN storage_routing_config.backend IS
    'Target backend: POSTGRES (JSONB sink) or SPARK (Delta Lake via external pipeline)';
COMMENT ON COLUMN storage_routing_config.effective_from IS
    'Routing rule activates at this timestamp; future-dating allows planned cutover';

-- Seed: default global rule → keep everything in Postgres until explicitly changed
INSERT INTO storage_routing_config (org_id, source_type, backend, created_by)
VALUES (NULL, NULL, 'POSTGRES', 'system-migration-init');

GRANT SELECT, INSERT, UPDATE, DELETE ON storage_routing_config TO engine_data_user;
