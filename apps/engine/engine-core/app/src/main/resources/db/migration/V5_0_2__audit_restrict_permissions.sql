-- V2: Enforce immutability at database level
-- REVOKE UPDATE and DELETE permissions on audit tables for the service user
-- This ensures audit logs are truly append-only even if application code is compromised

DO $$
BEGIN
    -- Revoke from current consolidated user (engine_core_user)
    IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'engine_core_user') THEN
        EXECUTE 'REVOKE UPDATE, DELETE ON audit_logs FROM engine_core_user';
        EXECUTE 'REVOKE UPDATE, DELETE ON read_access_logs FROM engine_core_user';
        EXECUTE 'REVOKE UPDATE, DELETE ON ai_audit_logs FROM engine_core_user';
    END IF;
    -- Revoke from legacy audit_user if it still exists
    IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'audit_user') THEN
        EXECUTE 'REVOKE UPDATE, DELETE ON audit_logs FROM audit_user';
        EXECUTE 'REVOKE UPDATE, DELETE ON read_access_logs FROM audit_user';
        EXECUTE 'REVOKE UPDATE, DELETE ON ai_audit_logs FROM audit_user';
    END IF;
END;
$$;
