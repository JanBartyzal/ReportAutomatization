-- V2: Seed default roles and test organizations

-- Default roles
INSERT INTO roles (id, name, description, hierarchy_level) VALUES
    (gen_random_uuid(), 'HOLDING_ADMIN', 'Super administrator with cross-organization access', 0),
    (gen_random_uuid(), 'ADMIN',         'Organization administrator',                        1),
    (gen_random_uuid(), 'EDITOR',        'Can create and modify reports',                     2),
    (gen_random_uuid(), 'VIEWER',        'Read-only access to reports',                       3);

-- Test organizations (for development/testing only)
INSERT INTO organizations (id, code, name, tenant_id, active) VALUES
    ('a0000000-0000-0000-0000-000000000001', 'TEST-ORG-1', 'Test Organization Alpha', NULL, TRUE),
    ('a0000000-0000-0000-0000-000000000002', 'TEST-ORG-2', 'Test Organization Beta',  NULL, TRUE);

-- Dev admin user: jan.bartyzal@wizardhub.cz
INSERT INTO user_roles (id, user_oid, organization_id, role, is_active_org) VALUES
    (gen_random_uuid(), '6bbc3213-00ac-4d30-bf27-7477b207c515', 'a0000000-0000-0000-0000-000000000001', 'HOLDING_ADMIN', TRUE),
    (gen_random_uuid(), '6bbc3213-00ac-4d30-bf27-7477b207c515', 'a0000000-0000-0000-0000-000000000002', 'HOLDING_ADMIN', FALSE);
