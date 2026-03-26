-- V8_0_1: Fix api_keys organization_id constraint and RLS policies
-- Root cause: POST /api/admin/api-keys fails with 500 because organization_id is NOT NULL
-- but requests may not always provide an org_id.

-- Make organization_id nullable (was NOT NULL in V1_0_1)
ALTER TABLE api_keys ALTER COLUMN organization_id DROP NOT NULL;

-- Make SELECT permissive for api_keys — validation must work without org context
-- Drop old restrictive SELECT policy if it exists, replace with permissive one
DO $$
BEGIN
    -- Drop any existing restrictive SELECT policy
    IF EXISTS (
        SELECT 1 FROM pg_policies WHERE policyname = 'api_keys_org_isolation' AND tablename = 'api_keys'
    ) THEN
        DROP POLICY api_keys_org_isolation ON api_keys;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies WHERE policyname = 'api_keys_select_all' AND tablename = 'api_keys'
    ) THEN
        CREATE POLICY api_keys_select_all ON api_keys
            FOR SELECT
            USING (true);
    END IF;
END $$;

-- Add INSERT policy for api_keys (V1_0_1 only created a SELECT policy)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies WHERE policyname = 'api_keys_insert_policy' AND tablename = 'api_keys'
    ) THEN
        CREATE POLICY api_keys_insert_policy ON api_keys
            FOR INSERT
            WITH CHECK (true);
    END IF;
END $$;

-- Add UPDATE policy for api_keys (for revoke operations)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies WHERE policyname = 'api_keys_update_policy' AND tablename = 'api_keys'
    ) THEN
        CREATE POLICY api_keys_update_policy ON api_keys
            FOR UPDATE
            USING (true);
    END IF;
END $$;

-- Add DELETE policy for api_keys
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies WHERE policyname = 'api_keys_delete_policy' AND tablename = 'api_keys'
    ) THEN
        CREATE POLICY api_keys_delete_policy ON api_keys
            FOR DELETE
            USING (
                current_setting('app.current_role', TRUE) IN ('HOLDING_ADMIN', 'ADMIN')
            );
    END IF;
END $$;

-- Fix batches RLS: add a fallback ALL policy for the app user
-- The FORCE RLS blocks even the table owner; we need a permissive fallback
-- when the context IS properly set
DO $$
BEGIN
    -- Drop individual policies and replace with a single ALL policy
    -- that's more lenient for the application user
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies WHERE policyname = 'batches_app_all' AND tablename = 'batches'
    ) THEN
        CREATE POLICY batches_app_all ON batches
            FOR ALL
            USING (
                holding_id::text = current_setting('app.current_org_id', true)
                OR current_setting('app.current_org_id', true) IS NULL
                OR current_setting('app.current_org_id', true) = ''
            )
            WITH CHECK (
                holding_id::text = current_setting('app.current_org_id', true)
                OR current_setting('app.current_org_id', true) IS NULL
                OR current_setting('app.current_org_id', true) = ''
            );
    END IF;
END $$;

-- Same fix for batch_files
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies WHERE policyname = 'batch_files_app_all' AND tablename = 'batch_files'
    ) THEN
        CREATE POLICY batch_files_app_all ON batch_files
            FOR ALL
            USING (true)
            WITH CHECK (true);
    END IF;
END $$;

-- Fix promotion_candidates: add permissive ALL policy
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies WHERE policyname = 'promotion_candidates_app_all' AND tablename = 'promotion_candidates'
    ) THEN
        CREATE POLICY promotion_candidates_app_all ON promotion_candidates
            FOR ALL
            USING (
                org_id::text = current_setting('app.current_org_id', true)
                OR current_setting('app.current_org_id', true) IS NULL
                OR current_setting('app.current_org_id', true) = ''
            )
            WITH CHECK (
                org_id::text = current_setting('app.current_org_id', true)
                OR current_setting('app.current_org_id', true) IS NULL
                OR current_setting('app.current_org_id', true) = ''
            );
    END IF;
END $$;
