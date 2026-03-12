-- P2 Service Users Initialization
-- Creates database roles for MS-QRY and MS-DASH services

-- MS-QRY: Read-only access to sink tables + materialized views
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'ms_qry') THEN
        CREATE ROLE ms_qry WITH LOGIN PASSWORD 'ms_qry_pass';
    END IF;
END
$$;

GRANT CONNECT ON DATABASE reportplatform TO ms_qry;
GRANT USAGE ON SCHEMA public TO ms_qry;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO ms_qry;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO ms_qry;
GRANT USAGE ON SCHEMA rls TO ms_qry;
GRANT EXECUTE ON FUNCTION rls.set_org_context(UUID) TO ms_qry;
GRANT EXECUTE ON FUNCTION rls.get_current_org_id() TO ms_qry;

-- MS-DASH: Read access to sink tables + CRUD on dashboards table
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'ms_dash') THEN
        CREATE ROLE ms_dash WITH LOGIN PASSWORD 'ms_dash_pass';
    END IF;
END
$$;

GRANT CONNECT ON DATABASE reportplatform TO ms_dash;
GRANT USAGE ON SCHEMA public TO ms_dash;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO ms_dash;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO ms_dash;
-- ms_dash will also get INSERT/UPDATE/DELETE on dashboards table via Flyway migration
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO ms_dash;
GRANT USAGE ON SCHEMA rls TO ms_dash;
GRANT EXECUTE ON FUNCTION rls.set_org_context(UUID) TO ms_dash;
GRANT EXECUTE ON FUNCTION rls.get_current_org_id() TO ms_dash;

RAISE NOTICE 'P2 service users created successfully';
