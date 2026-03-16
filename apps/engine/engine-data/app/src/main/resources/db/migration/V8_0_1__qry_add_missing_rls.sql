-- P11-W1-004: RLS Policies for engine-data tables
-- Add missing RLS policies to tables that don't have them

-- =============================================================================
-- PARSED_TABLES - org isolation via files join
-- =============================================================================

ALTER TABLE IF EXISTS parsed_tables ENABLE ROW LEVEL SECURITY;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies WHERE policyname = 'parsed_tables_org_isolation' AND tablename = 'parsed_tables'
    ) THEN
        CREATE POLICY parsed_tables_org_isolation ON parsed_tables
            FOR SELECT
            USING (
                file_id IN (
                    SELECT id::text FROM files WHERE org_id::text = current_setting('app.current_org_id', true)
                )
            );
    END IF;
END $$;

ALTER TABLE parsed_tables FORCE ROW LEVEL SECURITY;

-- =============================================================================
-- DOCUMENTS - org isolation via files join
-- =============================================================================

ALTER TABLE IF EXISTS documents ENABLE ROW LEVEL SECURITY;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies WHERE policyname = 'documents_org_isolation' AND tablename = 'documents'
    ) THEN
        CREATE POLICY documents_org_isolation ON documents
            FOR SELECT
            USING (
                file_id IN (
                    SELECT id::text FROM files WHERE org_id::text = current_setting('app.current_org_id', true)
                )
            );
    END IF;
END $$;

ALTER TABLE documents FORCE ROW LEVEL SECURITY;

-- =============================================================================
-- DOCUMENT_EMBEDDINGS - org isolation via documents join
-- =============================================================================

ALTER TABLE IF EXISTS document_embeddings ENABLE ROW LEVEL SECURITY;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies WHERE policyname = 'document_embeddings_org_isolation' AND tablename = 'document_embeddings'
    ) THEN
        CREATE POLICY document_embeddings_org_isolation ON document_embeddings
            FOR SELECT
            USING (
                document_id IN (
                    SELECT id FROM documents WHERE file_id IN (
                        SELECT id::text FROM files WHERE org_id::text = current_setting('app.current_org_id', true)
                    )
                )
            );
    END IF;
END $$;

ALTER TABLE document_embeddings FORCE ROW LEVEL SECURITY;

-- =============================================================================
-- PROCESSING_LOGS - org isolation
-- =============================================================================

ALTER TABLE IF EXISTS processing_logs ENABLE ROW LEVEL SECURITY;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies WHERE policyname = 'processing_logs_org_isolation' AND tablename = 'processing_logs'
    ) THEN
        CREATE POLICY processing_logs_org_isolation ON processing_logs
            FOR SELECT
            USING (
                file_id IN (
                    SELECT id::text FROM files WHERE org_id::text = current_setting('app.current_org_id', true)
                )
            );
    END IF;
END $$;

ALTER TABLE processing_logs FORCE ROW LEVEL SECURITY;

-- =============================================================================
-- MAPPING_TEMPLATES - org isolation or global
-- =============================================================================

ALTER TABLE IF EXISTS mapping_templates ENABLE ROW LEVEL SECURITY;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies WHERE policyname = 'mapping_templates_org_isolation' AND tablename = 'mapping_templates'
    ) THEN
        CREATE POLICY mapping_templates_org_isolation ON mapping_templates
            FOR SELECT
            USING (
                org_id IS NULL 
                OR org_id::text = current_setting('app.current_org_id', true)
            );
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies WHERE policyname = 'mapping_templates_insert_org' AND tablename = 'mapping_templates'
    ) THEN
        CREATE POLICY mapping_templates_insert_org ON mapping_templates
            FOR INSERT
            WITH CHECK (
                org_id IS NULL 
                OR org_id::text = current_setting('app.current_org_id', true)
            );
    END IF;
END $$;

ALTER TABLE mapping_templates FORCE ROW LEVEL SECURITY;

-- =============================================================================
-- MAPPING_RULES - org isolation via mapping_templates join
-- =============================================================================

ALTER TABLE IF EXISTS mapping_rules ENABLE ROW LEVEL SECURITY;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies WHERE policyname = 'mapping_rules_org_isolation' AND tablename = 'mapping_rules'
    ) THEN
        CREATE POLICY mapping_rules_org_isolation ON mapping_rules
            FOR SELECT
            USING (
                template_id IN (
                    SELECT id FROM mapping_templates 
                    WHERE org_id IS NULL 
                    OR org_id::text = current_setting('app.current_org_id', true)
                )
            );
    END IF;
END $$;

ALTER TABLE mapping_rules FORCE ROW LEVEL SECURITY;

-- =============================================================================
-- MAPPING_HISTORY - org isolation
-- =============================================================================

ALTER TABLE IF EXISTS mapping_history ENABLE ROW LEVEL SECURITY;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies WHERE policyname = 'mapping_history_org_isolation' AND tablename = 'mapping_history'
    ) THEN
        CREATE POLICY mapping_history_org_isolation ON mapping_history
            FOR SELECT
            USING (org_id::text = current_setting('app.current_org_id', true));
    END IF;
END $$;

ALTER TABLE mapping_history FORCE ROW LEVEL SECURITY;

-- =============================================================================
-- MAPPING_USAGE_TRACKING - org isolation
-- =============================================================================

ALTER TABLE IF EXISTS mapping_usage_tracking ENABLE ROW LEVEL SECURITY;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies WHERE policyname = 'mapping_usage_org_isolation' AND tablename = 'mapping_usage_tracking'
    ) THEN
        CREATE POLICY mapping_usage_org_isolation ON mapping_usage_tracking
            FOR SELECT
            USING (org_id::text = current_setting('app.current_org_id', true));
    END IF;
END $$;

ALTER TABLE mapping_usage_tracking FORCE ROW LEVEL SECURITY;

-- =============================================================================
-- PROMOTED_TABLES_REGISTRY - org isolation
-- =============================================================================

ALTER TABLE IF EXISTS promoted_tables_registry ENABLE ROW LEVEL SECURITY;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies WHERE policyname = 'promoted_tables_registry_org_isolation' AND tablename = 'promoted_tables_registry'
    ) THEN
        CREATE POLICY promoted_tables_registry_org_isolation ON promoted_tables_registry
            FOR SELECT
            USING (
                mapping_template_id IN (
                    SELECT id FROM mapping_templates
                    WHERE org_id IS NULL
                    OR org_id::text = current_setting('app.current_org_id', true)
                )
            );
    END IF;
END $$;

ALTER TABLE promoted_tables_registry FORCE ROW LEVEL SECURITY;
