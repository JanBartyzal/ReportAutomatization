-- P3b Service Users Initialization
-- Creates database roles for MS-LIFECYCLE and MS-PERIOD services
-- Both services use the shared 'reportplatform' database with Flyway migrations

-- MS-LIFECYCLE: Full CRUD on reports, report_status_history, submission_checklists
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'lifecycle_user') THEN
        CREATE ROLE lifecycle_user WITH LOGIN PASSWORD 'lifecycle_pass';
    END IF;
END
$$;

GRANT CONNECT ON DATABASE reportplatform TO lifecycle_user;
GRANT USAGE ON SCHEMA public TO lifecycle_user;
GRANT CREATE ON SCHEMA public TO lifecycle_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO lifecycle_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO lifecycle_user;
GRANT USAGE ON SCHEMA rls TO lifecycle_user;
GRANT EXECUTE ON FUNCTION rls.set_org_context(UUID) TO lifecycle_user;
GRANT EXECUTE ON FUNCTION rls.get_current_org_id() TO lifecycle_user;

-- MS-PERIOD: Full CRUD on periods, period_org_assignments
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'period_user') THEN
        CREATE ROLE period_user WITH LOGIN PASSWORD 'period_pass';
    END IF;
END
$$;

GRANT CONNECT ON DATABASE reportplatform TO period_user;
GRANT USAGE ON SCHEMA public TO period_user;
GRANT CREATE ON SCHEMA public TO period_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO period_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO period_user;
GRANT USAGE ON SCHEMA rls TO period_user;
GRANT EXECUTE ON FUNCTION rls.set_org_context(UUID) TO period_user;
GRANT EXECUTE ON FUNCTION rls.get_current_org_id() TO period_user;

DO $$ BEGIN RAISE NOTICE 'P3b service users created successfully'; END $$;
