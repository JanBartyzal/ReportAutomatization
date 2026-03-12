-- V2: Enforce immutability at database level
-- REVOKE UPDATE and DELETE permissions on all audit tables for audit_user
-- This ensures audit logs are truly append-only even if application code is compromised

REVOKE UPDATE, DELETE ON audit_logs FROM audit_user;
REVOKE UPDATE, DELETE ON read_access_logs FROM audit_user;
REVOKE UPDATE, DELETE ON ai_audit_logs FROM audit_user;
