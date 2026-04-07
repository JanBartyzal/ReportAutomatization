-- P8 Consolidated Service Users Initialization
-- Creates database roles and databases for consolidated deployment units

-- =============================================================================
-- ENGINE-CORE: Auth + Admin + Batch + Versioning + Audit
-- =============================================================================
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'engine_core_user') THEN
        CREATE ROLE engine_core_user WITH LOGIN PASSWORD 'engine_core_pass';
    END IF;
END
$$;

CREATE DATABASE engine_core_db OWNER engine_core_user;
GRANT ALL PRIVILEGES ON DATABASE engine_core_db TO engine_core_user;

-- Connect to engine_core_db to set up extensions and RLS
\c engine_core_db;

CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
SET timezone = 'UTC';

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

GRANT USAGE ON SCHEMA public TO engine_core_user;
GRANT USAGE ON SCHEMA rls TO engine_core_user;
GRANT EXECUTE ON FUNCTION rls.set_org_context(UUID) TO engine_core_user;
GRANT EXECUTE ON FUNCTION rls.get_current_org_id() TO engine_core_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO engine_core_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO engine_core_user;

-- Reconnect to default database
\c reportplatform;

-- =============================================================================
-- ENGINE-DATA: Sinks + Query + Dashboard + Search + Template
-- =============================================================================
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'engine_data_user') THEN
        CREATE ROLE engine_data_user WITH LOGIN PASSWORD 'engine_data_pass';
    END IF;
END
$$;

CREATE DATABASE engine_data_db OWNER engine_data_user;
GRANT ALL PRIVILEGES ON DATABASE engine_data_db TO engine_data_user;

\c engine_data_db;

CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
SET timezone = 'UTC';

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

GRANT USAGE ON SCHEMA public TO engine_data_user;
GRANT USAGE ON SCHEMA rls TO engine_data_user;
GRANT EXECUTE ON FUNCTION rls.set_org_context(UUID) TO engine_data_user;
GRANT EXECUTE ON FUNCTION rls.get_current_org_id() TO engine_data_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO engine_data_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO engine_data_user;

\c reportplatform;

-- =============================================================================
-- ENGINE-REPORTING: Lifecycle + Period + Form + PPTX Template + Notification
-- =============================================================================
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'engine_reporting_user') THEN
        CREATE ROLE engine_reporting_user WITH LOGIN PASSWORD 'engine_reporting_pass';
    END IF;
END
$$;

CREATE DATABASE engine_reporting_db OWNER engine_reporting_user;
GRANT ALL PRIVILEGES ON DATABASE engine_reporting_db TO engine_reporting_user;

\c engine_reporting_db;

CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
SET timezone = 'UTC';

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

GRANT USAGE ON SCHEMA public TO engine_reporting_user;
GRANT USAGE ON SCHEMA rls TO engine_reporting_user;
GRANT EXECUTE ON FUNCTION rls.set_org_context(UUID) TO engine_reporting_user;
GRANT EXECUTE ON FUNCTION rls.get_current_org_id() TO engine_reporting_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO engine_reporting_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO engine_reporting_user;

\c reportplatform;

-- =============================================================================
-- ENGINE-INGESTOR: File ingestion and processing
-- =============================================================================
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'engine_ingestor_user') THEN
        CREATE ROLE engine_ingestor_user WITH LOGIN PASSWORD 'engine_ingestor_pass';
    END IF;
END
$$;

CREATE DATABASE engine_ingestor_db OWNER engine_ingestor_user;
GRANT ALL PRIVILEGES ON DATABASE engine_ingestor_db TO engine_ingestor_user;

\c engine_ingestor_db;

CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
SET timezone = 'UTC';

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

GRANT USAGE ON SCHEMA public TO engine_ingestor_user;
GRANT USAGE ON SCHEMA rls TO engine_ingestor_user;
GRANT EXECUTE ON FUNCTION rls.set_org_context(UUID) TO engine_ingestor_user;
GRANT EXECUTE ON FUNCTION rls.get_current_org_id() TO engine_ingestor_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO engine_ingestor_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO engine_ingestor_user;

\c reportplatform;

-- =============================================================================
-- ENGINE-ORCHESTRATOR: Workflow orchestration
-- =============================================================================
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'engine_orchestrator_user') THEN
        CREATE ROLE engine_orchestrator_user WITH LOGIN PASSWORD 'engine_orchestrator_pass';
    END IF;
END
$$;

CREATE DATABASE engine_orchestrator_db OWNER engine_orchestrator_user;
GRANT ALL PRIVILEGES ON DATABASE engine_orchestrator_db TO engine_orchestrator_user;

\c engine_orchestrator_db;

CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
SET timezone = 'UTC';

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

GRANT USAGE ON SCHEMA public TO engine_orchestrator_user;
GRANT USAGE ON SCHEMA rls TO engine_orchestrator_user;
GRANT EXECUTE ON FUNCTION rls.set_org_context(UUID) TO engine_orchestrator_user;
GRANT EXECUTE ON FUNCTION rls.get_current_org_id() TO engine_orchestrator_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO engine_orchestrator_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO engine_orchestrator_user;

\c reportplatform;

-- =============================================================================
-- ENGINE-INTEGRATIONS: External integrations
-- =============================================================================
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'engine_integrations_user') THEN
        CREATE ROLE engine_integrations_user WITH LOGIN PASSWORD 'engine_integrations_pass';
    END IF;
END
$$;

CREATE DATABASE engine_integrations_db OWNER engine_integrations_user;
GRANT ALL PRIVILEGES ON DATABASE engine_integrations_db TO engine_integrations_user;

\c engine_integrations_db;

CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
SET timezone = 'UTC';

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

GRANT USAGE ON SCHEMA public TO engine_integrations_user;
GRANT USAGE ON SCHEMA rls TO engine_integrations_user;
GRANT EXECUTE ON FUNCTION rls.set_org_context(UUID) TO engine_integrations_user;
GRANT EXECUTE ON FUNCTION rls.get_current_org_id() TO engine_integrations_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO engine_integrations_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO engine_integrations_user;

\c reportplatform;

DO $$ BEGIN RAISE NOTICE 'P8 consolidated service users and databases created successfully'; END $$;
