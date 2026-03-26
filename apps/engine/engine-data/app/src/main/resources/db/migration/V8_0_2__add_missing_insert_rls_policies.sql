-- V8_0_2: Add missing INSERT/UPDATE RLS policies for engine-data tables

-- mapping_rules: app writes rules when templates are created
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'mapping_rules_insert' AND tablename = 'mapping_rules') THEN
        CREATE POLICY mapping_rules_insert ON mapping_rules FOR INSERT WITH CHECK (true);
    END IF;
END $$;

-- mapping_history: app logs mapping operations
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'mapping_history_insert' AND tablename = 'mapping_history') THEN
        CREATE POLICY mapping_history_insert ON mapping_history FOR INSERT WITH CHECK (true);
    END IF;
END $$;

-- mapping_usage_tracking: app tracks usage
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'mapping_usage_tracking_insert' AND tablename = 'mapping_usage_tracking') THEN
        CREATE POLICY mapping_usage_tracking_insert ON mapping_usage_tracking FOR INSERT WITH CHECK (true);
    END IF;
END $$;

-- search_index: app inserts search entries
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'search_index_insert' AND tablename = 'search_index') THEN
        CREATE POLICY search_index_insert ON search_index FOR INSERT WITH CHECK (true);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'search_index_update' AND tablename = 'search_index') THEN
        CREATE POLICY search_index_update ON search_index FOR UPDATE USING (true);
    END IF;
END $$;

-- promoted_tables_registry: app inserts promoted tables
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'promoted_tables_registry_insert' AND tablename = 'promoted_tables_registry') THEN
        CREATE POLICY promoted_tables_registry_insert ON promoted_tables_registry FOR INSERT WITH CHECK (true);
    END IF;
END $$;
