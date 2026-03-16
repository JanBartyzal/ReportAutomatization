-- P7 Service Users Initialization
-- Creates database role for MS-EXT-SNOW service
-- MS-GEN-XLS does not need a database role (uses Blob Storage only)

-- MS-EXT-SNOW: Full CRUD on servicenow_connections, sync_schedules, sync_job_history,
-- distribution_rules, distribution_history
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'snow_user') THEN
        CREATE ROLE snow_user WITH LOGIN PASSWORD 'snow_pass';
    END IF;
END
$$;

GRANT CONNECT ON DATABASE reportplatform TO snow_user;
GRANT USAGE ON SCHEMA public TO snow_user;
GRANT CREATE ON SCHEMA public TO snow_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO snow_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO snow_user;
GRANT USAGE ON SCHEMA rls TO snow_user;
GRANT EXECUTE ON FUNCTION rls.set_org_context(UUID) TO snow_user;
GRANT EXECUTE ON FUNCTION rls.get_current_org_id() TO snow_user;

DO $$ BEGIN RAISE NOTICE 'P7 service users created successfully'; END $$;
