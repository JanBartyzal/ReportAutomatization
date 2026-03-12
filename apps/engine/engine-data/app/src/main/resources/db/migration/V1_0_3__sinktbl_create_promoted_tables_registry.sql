-- V3__create_promoted_tables_registry.sql
-- Phase 6: Admin Approval & Table Creation
-- Registry for promoted tables created from high-usage JSONB mapping templates.

-- Promoted tables registry
CREATE TABLE IF NOT EXISTS promoted_tables_registry (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    mapping_template_id UUID NOT NULL,
    table_name VARCHAR(255) NOT NULL UNIQUE,
    ddl_applied TEXT NOT NULL,
    dual_write_until TIMESTAMP WITH TIME ZONE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('CREATING', 'ACTIVE', 'MIGRATING', 'DISABLED')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_promoted_mapping ON promoted_tables_registry(mapping_template_id);
CREATE INDEX idx_promoted_status ON promoted_tables_registry(status);
