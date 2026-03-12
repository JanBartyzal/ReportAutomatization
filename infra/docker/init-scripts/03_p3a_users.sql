-- P3a Service Users Initialization
-- Creates database roles for MS-TMPL service

-- MS-TMPL: Full CRUD on mapping tables (mapping_templates, mapping_rules, mapping_history)
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'ms_tmpl') THEN
        CREATE ROLE ms_tmpl WITH LOGIN PASSWORD 'ms_tmpl_pass';
    END IF;
END
$$;

GRANT CONNECT ON DATABASE reportplatform TO ms_tmpl;
GRANT USAGE ON SCHEMA public TO ms_tmpl;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO ms_tmpl;
GRANT USAGE ON SCHEMA rls TO ms_tmpl;
GRANT EXECUTE ON FUNCTION rls.set_org_context(UUID) TO ms_tmpl;
GRANT EXECUTE ON FUNCTION rls.get_current_org_id() TO ms_tmpl;

RAISE NOTICE 'P3a service users created successfully';
