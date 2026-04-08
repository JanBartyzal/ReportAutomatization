-- P8 Consolidated Schema-Based Isolation
-- Single database (reportplatform) with per-service schemas
-- Replaces 8 separate databases with 6 schemas + shared rls schema
--
-- Schemas:
--   core         -> engine-core (Auth + Admin + Batch + Versioning + Audit)
--   data         -> engine-data (Sinks + Query + Dashboard + Search + Template)
--   reporting    -> engine-reporting (Lifecycle + Period + Form + PPTX Template + Notification)
--   ingestor     -> engine-ingestor (File ingestion + virus scanning)
--   orchestrator -> engine-orchestrator (Workflow orchestration)
--   integrations -> engine-integrations (ServiceNow integration)
--   rls          -> shared Row-Level Security functions

-- =============================================================================
-- EXTENSIONS (in reportplatform database, already connected)
-- =============================================================================
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS hstore;
SET timezone = 'UTC';

-- =============================================================================
-- SHARED RLS SCHEMA
-- =============================================================================
CREATE SCHEMA IF NOT EXISTS rls;

CREATE OR REPLACE FUNCTION rls.set_org_context(p_org_id UUID)
RETURNS void LANGUAGE plpgsql AS $$
BEGIN
    PERFORM set_config('app.current_org_id', p_org_id::text, false);
END;
$$;

CREATE OR REPLACE FUNCTION rls.get_current_org_id()
RETURNS UUID LANGUAGE plpgsql AS $$
DECLARE v_org_id text;
BEGIN
    v_org_id := current_setting('app.current_org_id', true);
    IF v_org_id IS NULL OR v_org_id = '' THEN RETURN NULL; END IF;
    RETURN v_org_id::UUID;
END;
$$;

CREATE OR REPLACE FUNCTION rls.set_audit_context(
    p_user_id UUID DEFAULT NULL,
    p_session_id UUID DEFAULT NULL
)
RETURNS void LANGUAGE plpgsql AS $$
BEGIN
    IF p_user_id IS NOT NULL THEN
        PERFORM set_config('app.current_user_id', p_user_id::text, false);
    END IF;
    IF p_session_id IS NOT NULL THEN
        PERFORM set_config('app.current_session_id', p_session_id::text, false);
    END IF;
END;
$$;

-- =============================================================================
-- SERVICE SCHEMAS
-- =============================================================================
CREATE SCHEMA IF NOT EXISTS core;
CREATE SCHEMA IF NOT EXISTS data;
CREATE SCHEMA IF NOT EXISTS reporting;
CREATE SCHEMA IF NOT EXISTS ingestor;
CREATE SCHEMA IF NOT EXISTS orchestrator;
CREATE SCHEMA IF NOT EXISTS integrations;

-- =============================================================================
-- SERVICE USERS
-- =============================================================================

-- Helper function to create role if not exists
DO $$
DECLARE
    role_rec RECORD;
    roles text[][] := ARRAY[
        ARRAY['engine_core_user',         'engine_core_pass'],
        ARRAY['engine_data_user',         'engine_data_pass'],
        ARRAY['engine_reporting_user',    'engine_reporting_pass'],
        ARRAY['engine_ingestor_user',     'engine_ingestor_pass'],
        ARRAY['engine_orchestrator_user', 'engine_orchestrator_pass'],
        ARRAY['engine_integrations_user', 'engine_integrations_pass'],
        ARRAY['ms_qry',                  'ms_qry_pass']
    ];
    r text[];
BEGIN
    FOREACH r SLICE 1 IN ARRAY roles LOOP
        IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = r[1]) THEN
            EXECUTE format('CREATE ROLE %I WITH LOGIN PASSWORD %L', r[1], r[2]);
        END IF;
    END LOOP;
END;
$$;

-- =============================================================================
-- GRANT PERMISSIONS: each user gets full access to its schema + rls
-- =============================================================================

-- engine_core_user -> core schema
GRANT USAGE, CREATE ON SCHEMA core TO engine_core_user;
GRANT USAGE ON SCHEMA rls TO engine_core_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA core
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO engine_core_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA core
    GRANT USAGE, SELECT ON SEQUENCES TO engine_core_user;
GRANT EXECUTE ON FUNCTION rls.set_org_context(UUID) TO engine_core_user;
GRANT EXECUTE ON FUNCTION rls.get_current_org_id() TO engine_core_user;
GRANT EXECUTE ON FUNCTION rls.set_audit_context(UUID, UUID) TO engine_core_user;

-- engine_data_user -> data schema
GRANT USAGE, CREATE ON SCHEMA data TO engine_data_user;
GRANT USAGE ON SCHEMA rls TO engine_data_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA data
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO engine_data_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA data
    GRANT USAGE, SELECT ON SEQUENCES TO engine_data_user;
GRANT EXECUTE ON FUNCTION rls.set_org_context(UUID) TO engine_data_user;
GRANT EXECUTE ON FUNCTION rls.get_current_org_id() TO engine_data_user;

-- engine_reporting_user -> reporting schema
GRANT USAGE, CREATE ON SCHEMA reporting TO engine_reporting_user;
GRANT USAGE ON SCHEMA rls TO engine_reporting_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA reporting
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO engine_reporting_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA reporting
    GRANT USAGE, SELECT ON SEQUENCES TO engine_reporting_user;
GRANT EXECUTE ON FUNCTION rls.set_org_context(UUID) TO engine_reporting_user;
GRANT EXECUTE ON FUNCTION rls.get_current_org_id() TO engine_reporting_user;

-- engine_ingestor_user -> ingestor schema
GRANT USAGE, CREATE ON SCHEMA ingestor TO engine_ingestor_user;
GRANT USAGE ON SCHEMA rls TO engine_ingestor_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA ingestor
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO engine_ingestor_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA ingestor
    GRANT USAGE, SELECT ON SEQUENCES TO engine_ingestor_user;
GRANT EXECUTE ON FUNCTION rls.set_org_context(UUID) TO engine_ingestor_user;
GRANT EXECUTE ON FUNCTION rls.get_current_org_id() TO engine_ingestor_user;

-- engine_orchestrator_user -> orchestrator schema
GRANT USAGE, CREATE ON SCHEMA orchestrator TO engine_orchestrator_user;
GRANT USAGE ON SCHEMA rls TO engine_orchestrator_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA orchestrator
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO engine_orchestrator_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA orchestrator
    GRANT USAGE, SELECT ON SEQUENCES TO engine_orchestrator_user;
GRANT EXECUTE ON FUNCTION rls.set_org_context(UUID) TO engine_orchestrator_user;
GRANT EXECUTE ON FUNCTION rls.get_current_org_id() TO engine_orchestrator_user;

-- engine_integrations_user -> integrations schema
GRANT USAGE, CREATE ON SCHEMA integrations TO engine_integrations_user;
GRANT USAGE ON SCHEMA rls TO engine_integrations_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA integrations
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO engine_integrations_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA integrations
    GRANT USAGE, SELECT ON SEQUENCES TO engine_integrations_user;
GRANT EXECUTE ON FUNCTION rls.set_org_context(UUID) TO engine_integrations_user;
GRANT EXECUTE ON FUNCTION rls.get_current_org_id() TO engine_integrations_user;

-- ms_qry (read-only) -> data schema (for processor-generators)
GRANT USAGE ON SCHEMA data TO ms_qry;
GRANT USAGE ON SCHEMA rls TO ms_qry;
GRANT SELECT ON ALL TABLES IN SCHEMA data TO ms_qry;
ALTER DEFAULT PRIVILEGES IN SCHEMA data
    GRANT SELECT ON TABLES TO ms_qry;
GRANT EXECUTE ON FUNCTION rls.set_org_context(UUID) TO ms_qry;
GRANT EXECUTE ON FUNCTION rls.get_current_org_id() TO ms_qry;

-- Legacy app_user (from 01_extensions.sql) - grant rls access
DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'app_user') THEN
        GRANT USAGE ON SCHEMA rls TO app_user;
        GRANT EXECUTE ON FUNCTION rls.set_org_context(UUID) TO app_user;
        GRANT EXECUTE ON FUNCTION rls.get_current_org_id() TO app_user;
        GRANT EXECUTE ON FUNCTION rls.set_audit_context(UUID, UUID) TO app_user;
    END IF;
END;
$$;

DO $$ BEGIN RAISE NOTICE 'P8 consolidated schemas created successfully in reportplatform database'; END $$;
