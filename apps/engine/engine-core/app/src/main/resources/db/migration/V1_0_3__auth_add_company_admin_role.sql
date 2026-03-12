-- V3: Add COMPANY_ADMIN role for local scope support (P6-W1)
-- COMPANY_ADMIN sits between ADMIN and EDITOR in the hierarchy.
-- Scoped to own organization: can create/manage LOCAL forms, templates, and local users.

INSERT INTO roles (id, name, description, hierarchy_level) VALUES
    (gen_random_uuid(), 'COMPANY_ADMIN', 'Company administrator – manages local forms, templates, and users within own org', 2);

-- Update existing roles hierarchy levels to accommodate COMPANY_ADMIN
UPDATE roles SET hierarchy_level = 3 WHERE name = 'EDITOR';
UPDATE roles SET hierarchy_level = 4 WHERE name = 'VIEWER';

-- Update RLS policies to recognize COMPANY_ADMIN for api_keys visibility
DROP POLICY IF EXISTS api_keys_rls_policy ON api_keys;
CREATE POLICY api_keys_rls_policy ON api_keys
    USING (
        current_setting('app.current_role', TRUE) IN ('HOLDING_ADMIN', 'ADMIN', 'COMPANY_ADMIN')
    );

-- Update user_roles RLS to allow COMPANY_ADMIN to see roles within own org
DROP POLICY IF EXISTS user_roles_rls_policy ON user_roles;
CREATE POLICY user_roles_rls_policy ON user_roles
    USING (
        user_oid = current_setting('app.current_user_oid', TRUE)
        OR current_setting('app.current_role', TRUE) = 'HOLDING_ADMIN'
        OR (
            current_setting('app.current_role', TRUE) = 'COMPANY_ADMIN'
            AND organization_id = current_setting('app.current_org_id', TRUE)::UUID
        )
    );
