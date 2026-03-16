-- V2_0_1: Admin service schema extensions
-- Extends auth tables (organizations, api_keys) and adds role_audit_log

-- Add missing columns to organizations (created in V1_0_1)
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

-- Add indexes on new columns (IF NOT EXISTS requires PG 9.5+)
CREATE INDEX IF NOT EXISTS idx_org_parent ON organizations(parent_id);
CREATE INDEX IF NOT EXISTS idx_org_type ON organizations(type);

-- Add missing columns to api_keys (created in V1_0_1)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'api_keys' AND column_name = 'scopes'
    ) THEN
        ALTER TABLE api_keys ADD COLUMN scopes TEXT[] NOT NULL DEFAULT '{}';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'api_keys' AND column_name = 'created_by'
    ) THEN
        ALTER TABLE api_keys ADD COLUMN created_by VARCHAR(255);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'api_keys' AND column_name = 'revoked'
    ) THEN
        ALTER TABLE api_keys ADD COLUMN revoked BOOLEAN NOT NULL DEFAULT false;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'api_keys' AND column_name = 'revoked_at'
    ) THEN
        ALTER TABLE api_keys ADD COLUMN revoked_at TIMESTAMP WITH TIME ZONE;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'api_keys' AND column_name = 'org_id'
    ) THEN
        ALTER TABLE api_keys ADD COLUMN org_id UUID REFERENCES organizations(id) ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_api_keys_org ON api_keys(org_id);
CREATE INDEX IF NOT EXISTS idx_api_keys_created_by ON api_keys(created_by);

-- Audit log for role changes (new table)
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

CREATE INDEX IF NOT EXISTS idx_role_audit_user ON role_audit_log(user_id);
CREATE INDEX IF NOT EXISTS idx_role_audit_performed_by ON role_audit_log(performed_by);
CREATE INDEX IF NOT EXISTS idx_role_audit_performed_at ON role_audit_log(performed_at);
