-- PostgreSQL Initialization Script
-- Creates extensions and base configuration for ReportAutomatization

-- Enable pgVector extension for document embeddings
CREATE EXTENSION IF NOT EXISTS vector;

-- Enable UUID generation
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Enable hstore for key-value storage (if needed)
CREATE EXTENSION IF NOT EXISTS hstore;

-- Set timezone
SET timezone = 'UTC';

-- Create application user with limited privileges
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'app_user') THEN
        CREATE ROLE app_user WITH LOGIN PASSWORD 'app_user_pass';
    END IF;
END
$$;

-- Grant connect permission
GRANT CONNECT ON DATABASE reportplatform TO app_user;

-- Grant schema usage
GRANT USAGE ON SCHEMA public TO app_user;

-- Grant default privileges for new tables
ALTER DEFAULT PRIVILEGES IN SCHEMA public 
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO app_user;

ALTER DEFAULT PRIVILEGES IN SCHEMA public 
    GRANT USAGE, SELECT ON ALL SEQUENCES TO app_user;

-- Create service-specific schemas (optional - services can use public schema)
-- Uncomment if you want to isolate services into separate schemas:
-- CREATE SCHEMA IF NOT EXISTS auth;
-- CREATE SCHEMA IF NOT EXISTS ingestion;
-- CREATE SCHEMA IF NOT EXISTS orchestrator;
-- CREATE SCHEMA IF NOT EXISTS sinks;

-- Grant schema permissions to app_user
-- GRANT USAGE ON SCHEMA auth TO app_user;
-- GRANT ALL PRIVILEGES ON SCHEMA auth TO app_user;

-- Create RLS policies schema (for RLS management)
CREATE SCHEMA IF NOT EXISTS rls;

-- Function to set session context for org_id
CREATE OR REPLACE FUNCTION rls.set_org_context(p_org_id UUID)
RETURNS void
LANGUAGE plpgsql
AS $$
BEGIN
    PERFORM set_config('app.current_org_id', p_org_id::text, false);
END;
$$;

-- Function to get current org_id from session
CREATE OR REPLACE FUNCTION rls.get_current_org_id()
RETURNS UUID
LANGUAGE plpgsql
AS $$
DECLARE
    v_org_id text;
BEGIN
    v_org_id := current_setting('app.current_org_id', true);
    IF v_org_id IS NULL OR v_org_id = '' THEN
        RETURN NULL;
    END IF;
    RETURN v_org_id::UUID;
END;
$$;

-- Function for audit trail (template)
CREATE OR REPLACE FUNCTION rls.set_audit_context(
    p_user_id UUID DEFAULT NULL,
    p_session_id UUID DEFAULT NULL
)
RETURNS void
LANGUAGE plpgsql
AS $$
BEGIN
    IF p_user_id IS NOT NULL THEN
        PERFORM set_config('app.current_user_id', p_user_id::text, false);
    END IF;
    IF p_session_id IS NOT NULL THEN
        PERFORM set_config('app.current_session_id', p_session_id::text, false);
    END IF;
END;
$$;

-- Grant execute permissions to app_user
GRANT EXECUTE ON FUNCTION rls.set_org_context(UUID) TO app_user;
GRANT EXECUTE ON FUNCTION rls.get_current_org_id() TO app_user;
GRANT EXECUTE ON FUNCTION rls.set_audit_context(UUID, UUID) TO app_user;

-- Log successful initialization
RAISE NOTICE 'PostgreSQL initialization completed successfully';
