-- V1: Create auth tables with Row Level Security support

-- Organizations table
CREATE TABLE organizations (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code        VARCHAR(50)  NOT NULL UNIQUE,
    name        VARCHAR(255) NOT NULL,
    tenant_id   VARCHAR(64),
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_organizations_tenant_id ON organizations (tenant_id);
CREATE INDEX idx_organizations_code ON organizations (code);

-- Enable RLS on organizations
ALTER TABLE organizations ENABLE ROW LEVEL SECURITY;

CREATE POLICY organizations_rls_policy ON organizations
    USING (
        active = TRUE
        OR current_setting('app.current_role', TRUE) = 'HOLDING_ADMIN'
    );

-- Roles table
CREATE TABLE roles (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(30)  NOT NULL UNIQUE,
    description     VARCHAR(255),
    hierarchy_level INT          NOT NULL
);

-- User roles (org-scoped)
CREATE TABLE user_roles (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_oid        VARCHAR(64)  NOT NULL,
    organization_id UUID         NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    role            VARCHAR(30)  NOT NULL,
    is_active_org   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (user_oid, organization_id, role)
);

CREATE INDEX idx_user_roles_user_oid ON user_roles (user_oid);
CREATE INDEX idx_user_roles_org_id ON user_roles (organization_id);
CREATE INDEX idx_user_roles_active_org ON user_roles (user_oid, is_active_org) WHERE is_active_org = TRUE;

-- Enable RLS on user_roles
ALTER TABLE user_roles ENABLE ROW LEVEL SECURITY;

CREATE POLICY user_roles_rls_policy ON user_roles
    USING (
        user_oid = current_setting('app.current_user_oid', TRUE)
        OR current_setting('app.current_role', TRUE) = 'HOLDING_ADMIN'
    );

-- API keys for service accounts
CREATE TABLE api_keys (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    key_name        VARCHAR(100) NOT NULL,
    key_hash        VARCHAR(255) NOT NULL,
    key_prefix      VARCHAR(10)  NOT NULL,
    organization_id UUID         NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    role            VARCHAR(30)  NOT NULL DEFAULT 'VIEWER',
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    expires_at      TIMESTAMPTZ,
    last_used_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_api_keys_prefix ON api_keys (key_prefix) WHERE active = TRUE;
CREATE INDEX idx_api_keys_org_id ON api_keys (organization_id) WHERE active = TRUE;

-- Enable RLS on api_keys
ALTER TABLE api_keys ENABLE ROW LEVEL SECURITY;

CREATE POLICY api_keys_rls_policy ON api_keys
    USING (
        current_setting('app.current_role', TRUE) IN ('HOLDING_ADMIN', 'ADMIN')
    );

-- Updated_at trigger function
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_organizations_updated_at
    BEFORE UPDATE ON organizations
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
