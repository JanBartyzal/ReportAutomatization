-- P11-W1-004: RLS Policies for batches, batch_files, periods tables
-- These tables have ENABLE ROW LEVEL SECURITY but no CREATE POLICY
-- This migration adds the missing policies

-- =============================================================================
-- BATCHES table - org isolation policy
-- =============================================================================

-- Check if policy already exists before creating
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies WHERE policyname = 'batches_org_isolation' AND tablename = 'batches'
    ) THEN
        CREATE POLICY batches_org_isolation ON batches
            FOR SELECT
            USING (org_id::text = current_setting('app.current_org_id', true));
    END IF;
END $$;

-- Policy for INSERT - allow if org matches
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies WHERE policyname = 'batches_insert_org' AND tablename = 'batches'
    ) THEN
        CREATE POLICY batches_insert_org ON batches
            FOR INSERT
            WITH CHECK (org_id::text = current_setting('app.current_org_id', true));
    END IF;
END $$;

-- Policy for UPDATE - allow if org matches
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies WHERE policyname = 'batches_update_org' AND tablename = 'batches'
    ) THEN
        CREATE POLICY batches_update_org ON batches
            FOR UPDATE
            USING (org_id::text = current_setting('app.current_org_id', true));
    END IF;
END $$;

-- Policy for DELETE - allow if org matches
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies WHERE policyname = 'batches_delete_org' AND tablename = 'batches'
    ) THEN
        CREATE POLICY batches_delete_org ON batches
            FOR DELETE
            USING (org_id::text = current_setting('app.current_org_id', true));
    END IF;
END $$;

-- Force RLS for table owner as well
ALTER TABLE batches FORCE ROW LEVEL SECURITY;

-- =============================================================================
-- BATCH_FILES table - org isolation via batches join
-- =============================================================================

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies WHERE policyname = 'batch_files_org_isolation' AND tablename = 'batch_files'
    ) THEN
        CREATE POLICY batch_files_org_isolation ON batch_files
            FOR SELECT
            USING (
                batch_id IN (
                    SELECT id FROM batches WHERE org_id::text = current_setting('app.current_org_id', true)
                )
            );
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies WHERE policyname = 'batch_files_insert_org' AND tablename = 'batch_files'
    ) THEN
        CREATE POLICY batch_files_insert_org ON batch_files
            FOR INSERT
            WITH CHECK (
                batch_id IN (
                    SELECT id FROM batches WHERE org_id::text = current_setting('app.current_org_id', true)
                )
            );
    END IF;
END $$;

ALTER TABLE batch_files FORCE ROW LEVEL SECURITY;

-- =============================================================================
-- PERIODS table - org isolation policy
-- =============================================================================

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies WHERE policyname = 'periods_org_isolation' AND tablename = 'periods'
    ) THEN
        CREATE POLICY periods_org_isolation ON periods
            FOR SELECT
            USING (org_id::text = current_setting('app.current_org_id', true));
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies WHERE policyname = 'periods_insert_org' AND tablename = 'periods'
    ) THEN
        CREATE POLICY periods_insert_org ON periods
            FOR INSERT
            WITH CHECK (org_id::text = current_setting('app.current_org_id', true));
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies WHERE policyname = 'periods_update_org' AND tablename = 'periods'
    ) THEN
        CREATE POLICY periods_update_org ON periods
            FOR UPDATE
            USING (org_id::text = current_setting('app.current_org_id', true));
    END IF;
END $$;

ALTER TABLE periods FORCE ROW LEVEL SECURITY;

-- =============================================================================
-- PROMOTION_CANDIDATES table - org isolation
-- =============================================================================

-- Enable RLS if not already enabled
ALTER TABLE IF EXISTS promotion_candidates ENABLE ROW LEVEL SECURITY;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies WHERE policyname = 'promotion_candidates_org_isolation' AND tablename = 'promotion_candidates'
    ) THEN
        CREATE POLICY promotion_candidates_org_isolation ON promotion_candidates
            FOR SELECT
            USING (org_id::text = current_setting('app.current_org_id', true));
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies WHERE policyname = 'promotion_candidates_insert_org' AND tablename = 'promotion_candidates'
    ) THEN
        CREATE POLICY promotion_candidates_insert_org ON promotion_candidates
            FOR INSERT
            WITH CHECK (org_id::text = current_setting('app.current_org_id', true));
    END IF;
END $$;

ALTER TABLE promotion_candidates FORCE ROW LEVEL SECURITY;

-- =============================================================================
-- ROLE_AUDIT_LOG table - admin only access
-- =============================================================================

ALTER TABLE IF EXISTS role_audit_log ENABLE ROW LEVEL SECURITY;

-- Role audit log is admin-only, so we create a policy that checks for admin role
-- Note: This requires the app.current_user_roles setting to be set by the application
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies WHERE policyname = 'role_audit_log_admin_only' AND tablename = 'role_audit_log'
    ) THEN
        CREATE POLICY role_audit_log_admin_only ON role_audit_log
            FOR SELECT
            USING (
                current_setting('app.current_user_roles', true) LIKE '%ADMIN%' 
                OR current_setting('app.current_user_roles', true) LIKE '%HOLDING_ADMIN%'
            );
    END IF;
END $$;

ALTER TABLE role_audit_log FORCE ROW LEVEL SECURITY;
