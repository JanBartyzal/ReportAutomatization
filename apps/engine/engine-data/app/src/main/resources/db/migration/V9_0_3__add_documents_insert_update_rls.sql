-- V9_0_3: Add missing INSERT/UPDATE RLS policies for documents
-- The UPSERT in PipelineStoreService uses ON CONFLICT ... DO UPDATE,
-- which requires both INSERT and UPDATE policies.

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'documents_insert' AND tablename = 'documents') THEN
        CREATE POLICY documents_insert ON documents FOR INSERT WITH CHECK (true);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'documents_update' AND tablename = 'documents') THEN
        CREATE POLICY documents_update ON documents FOR UPDATE USING (true);
    END IF;
END $$;
