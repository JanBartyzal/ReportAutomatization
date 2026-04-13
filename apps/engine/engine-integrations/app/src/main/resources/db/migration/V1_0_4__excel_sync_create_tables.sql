-- V1_0_4__excel_sync_create_tables.sql
-- Excel Sync integration module database schema

-- Export Flow Definitions table
CREATE TABLE IF NOT EXISTS export_flow_definitions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    sql_query TEXT NOT NULL,
    target_type VARCHAR(20) NOT NULL CHECK (target_type IN ('SHAREPOINT', 'LOCAL_PATH')),
    target_path VARCHAR(500) NOT NULL,
    target_sheet VARCHAR(31) NOT NULL,
    file_naming VARCHAR(20) NOT NULL DEFAULT 'CUSTOM' CHECK (file_naming IN ('CUSTOM', 'BATCH_NAME')),
    custom_file_name VARCHAR(255),
    trigger_type VARCHAR(20) NOT NULL DEFAULT 'MANUAL' CHECK (trigger_type IN ('AUTO', 'MANUAL')),
    trigger_filter JSONB,
    sharepoint_config JSONB,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_export_flow_def_org ON export_flow_definitions(org_id);
CREATE INDEX idx_export_flow_def_active ON export_flow_definitions(is_active);
CREATE INDEX idx_export_flow_def_org_active ON export_flow_definitions(org_id, is_active);

-- Row Level Security
ALTER TABLE export_flow_definitions ENABLE ROW LEVEL SECURITY;

CREATE POLICY export_flow_definitions_org_isolation ON export_flow_definitions
    USING (org_id = rls.get_current_org_id());

-- Export Flow Executions table
CREATE TABLE IF NOT EXISTS export_flow_executions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    flow_id UUID NOT NULL REFERENCES export_flow_definitions(id),
    org_id UUID NOT NULL,
    trigger_source VARCHAR(50),
    trigger_event_id VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'RUNNING', 'SUCCESS', 'FAILED')),
    rows_exported INTEGER,
    target_path_used VARCHAR(500),
    error_message TEXT,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_export_flow_exec_flow ON export_flow_executions(flow_id);
CREATE INDEX idx_export_flow_exec_org ON export_flow_executions(org_id);
CREATE INDEX idx_export_flow_exec_started ON export_flow_executions(started_at DESC);

-- Row Level Security
ALTER TABLE export_flow_executions ENABLE ROW LEVEL SECURITY;

CREATE POLICY export_flow_executions_org_isolation ON export_flow_executions
    USING (org_id = rls.get_current_org_id());
