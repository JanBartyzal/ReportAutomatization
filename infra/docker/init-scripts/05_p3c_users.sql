-- P3c Service Users Initialization
-- Creates database role for MS-FORM service
-- MS-FORM uses the shared 'reportplatform' database with Flyway migrations

-- MS-FORM: Full CRUD on forms, form_versions, form_fields, form_responses, form_field_values, form_field_comments, form_assignments
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'form_user') THEN
        CREATE ROLE form_user WITH LOGIN PASSWORD 'form_pass';
    END IF;
END
$$;

GRANT CONNECT ON DATABASE reportplatform TO form_user;
GRANT USAGE ON SCHEMA public TO form_user;
GRANT CREATE ON SCHEMA public TO form_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO form_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO form_user;
GRANT USAGE ON SCHEMA rls TO form_user;
GRANT EXECUTE ON FUNCTION rls.set_org_context(UUID) TO form_user;
GRANT EXECUTE ON FUNCTION rls.get_current_org_id() TO form_user;

RAISE NOTICE 'P3c service users created successfully';
