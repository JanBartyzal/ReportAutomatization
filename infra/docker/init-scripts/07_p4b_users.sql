-- P4b Service Users Initialization
-- Creates database role for MS-TMPL-PPTX service
-- MS-GEN-PPTX does not need a database role (uses Blob Storage only)

-- MS-TMPL-PPTX: Full CRUD on pptx_templates, template_versions, template_placeholders, placeholder_mappings
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'tmpl_pptx_user') THEN
        CREATE ROLE tmpl_pptx_user WITH LOGIN PASSWORD 'tmpl_pptx_pass';
    END IF;
END
$$;

GRANT CONNECT ON DATABASE reportplatform TO tmpl_pptx_user;
GRANT USAGE ON SCHEMA public TO tmpl_pptx_user;
GRANT CREATE ON SCHEMA public TO tmpl_pptx_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO tmpl_pptx_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO tmpl_pptx_user;
GRANT USAGE ON SCHEMA rls TO tmpl_pptx_user;
GRANT EXECUTE ON FUNCTION rls.set_org_context(UUID) TO tmpl_pptx_user;
GRANT EXECUTE ON FUNCTION rls.get_current_org_id() TO tmpl_pptx_user;

DO $$ BEGIN RAISE NOTICE 'P4b service users created successfully'; END $$;
