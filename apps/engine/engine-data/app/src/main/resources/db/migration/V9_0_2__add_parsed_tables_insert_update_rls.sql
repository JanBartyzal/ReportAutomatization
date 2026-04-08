-- V9_0_2: Add missing INSERT/UPDATE RLS policies for parsed_tables
-- The UPSERT in StoreService uses ON CONFLICT ... DO UPDATE, which requires
-- both INSERT and UPDATE policies.

-- parsed_tables: orchestrator stores parsed file content
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'parsed_tables_insert' AND tablename = 'parsed_tables') THEN
        CREATE POLICY parsed_tables_insert ON parsed_tables FOR INSERT WITH CHECK (true);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'parsed_tables_update' AND tablename = 'parsed_tables') THEN
        CREATE POLICY parsed_tables_update ON parsed_tables FOR UPDATE USING (true);
    END IF;
END $$;
