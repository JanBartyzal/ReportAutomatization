-- V1__create_admin_tables.sql
-- Admin service database schema

-- Organizations table with hierarchy support (extends auth service)
CREATE TABLE IF NOT EXISTS organizations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(20) NOT NULL CHECK (type IN ('HOLDING', 'COMPANY', 'DIVISION')),
    parent_id UUID REFERENCES organizations(id) ON DELETE RESTRICT,
    tenant_id VARCHAR(64),
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_org_parent ON organizations(parent_id);
CREATE INDEX idx_org_tenant ON organizations(tenant_id);
CREATE INDEX idx_org_type ON organizations(type);

-- API Keys table
CREATE TABLE IF NOT EXISTS api_keys (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    key_hash VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    scopes TEXT[] NOT NULL DEFAULT '{}',
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP WITH TIME ZONE,
    last_used_at TIMESTAMP WITH TIME ZONE,
    revoked_at TIMESTAMP WITH TIME ZONE,
    revoked BOOLEAN NOT NULL DEFAULT false,
    org_id UUID REFERENCES organizations(id) ON DELETE SET NULL
);

CREATE INDEX idx_api_keys_org ON api_keys(org_id);
CREATE INDEX idx_api_keys_created_by ON api_keys(created_by);

-- Audit log for role changes
CREATE TABLE IF NOT EXISTS role_audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(255) NOT NULL,
    target_user_id VARCHAR(255),
    action VARCHAR(20) NOT NULL CHECK (action IN ('ASSIGN', 'REMOVE')),
    role VARCHAR(50) NOT NULL,
    org_id UUID REFERENCES organizations(id) ON DELETE SET NULL,
    performed_by VARCHAR(255) NOT NULL,
    performed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    ip_address VARCHAR(45)
);

CREATE INDEX idx_role_audit_user ON role_audit_log(user_id);
CREATE INDEX idx_role_audit_performed_by ON role_audit_log(performed_by);
CREATE INDEX idx_role_audit_performed_at ON role_audit_log(performed_at);

-- Add organization type column to existing auth service organizations if not exists
-- This is handled by checking if the column exists
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'organizations' AND column_name = 'type'
    ) THEN
        ALTER TABLE organizations ADD COLUMN type VARCHAR(20) NOT NULL DEFAULT 'COMPANY';
    END IF;
    
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'organizations' AND column_name = 'parent_id'
    ) THEN
        ALTER TABLE organizations ADD COLUMN parent_id UUID REFERENCES organizations(id) ON DELETE RESTRICT;
    END IF;
END $$;
