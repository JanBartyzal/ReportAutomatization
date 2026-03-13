-- V1_0_1__snow_create_tables.sql
-- Service-Now integration service database schema

-- Service-Now connections table
CREATE TABLE IF NOT EXISTS servicenow_connections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    instance_url VARCHAR(500) NOT NULL,
    auth_type VARCHAR(20) NOT NULL CHECK (auth_type IN ('OAUTH2', 'BASIC')),
    credentials_ref VARCHAR(255) NOT NULL,
    tables JSONB NOT NULL DEFAULT '[]',
    mapping_template_id UUID,
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_snow_conn_org ON servicenow_connections(org_id);
CREATE INDEX idx_snow_conn_enabled ON servicenow_connections(enabled);

-- Row Level Security
ALTER TABLE servicenow_connections ENABLE ROW LEVEL SECURITY;

CREATE POLICY servicenow_connections_org_isolation ON servicenow_connections
    USING (org_id = rls.get_current_org_id());
