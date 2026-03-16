-- P4a Service Users Initialization
-- Creates database roles for MS-VER and MS-AUDIT services
-- Both services use the shared 'reportplatform' database with Flyway migrations

-- MS-VER: Full CRUD on versions, version_diffs
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'ver_user') THEN
        CREATE ROLE ver_user WITH LOGIN PASSWORD 'ver_pass';
    END IF;
END
$$;

GRANT CONNECT ON DATABASE reportplatform TO ver_user;
GRANT USAGE ON SCHEMA public TO ver_user;
GRANT CREATE ON SCHEMA public TO ver_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO ver_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO ver_user;
GRANT USAGE ON SCHEMA rls TO ver_user;
GRANT EXECUTE ON FUNCTION rls.set_org_context(UUID) TO ver_user;
GRANT EXECUTE ON FUNCTION rls.get_current_org_id() TO ver_user;

-- MS-AUDIT: SELECT + INSERT only (NO UPDATE/DELETE for immutability)
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'audit_user') THEN
        CREATE ROLE audit_user WITH LOGIN PASSWORD 'audit_pass';
    END IF;
END
$$;

GRANT CONNECT ON DATABASE reportplatform TO audit_user;
GRANT USAGE ON SCHEMA public TO audit_user;
GRANT CREATE ON SCHEMA public TO audit_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT, INSERT ON TABLES TO audit_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO audit_user;
GRANT USAGE ON SCHEMA rls TO audit_user;
GRANT EXECUTE ON FUNCTION rls.set_org_context(UUID) TO audit_user;
GRANT EXECUTE ON FUNCTION rls.get_current_org_id() TO audit_user;

DO $$ BEGIN RAISE NOTICE 'P4a service users created successfully'; END $$;
