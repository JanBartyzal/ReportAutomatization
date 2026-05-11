-- Tighten write-side RLS policies that were originally added as app-wide fallbacks.
-- Inserts/updates must keep rows inside the current tenant context.

DROP POLICY IF EXISTS parsed_tables_insert ON parsed_tables;
CREATE POLICY parsed_tables_insert ON parsed_tables
    FOR INSERT
    WITH CHECK (org_id::text = current_setting('app.current_org_id', true));

DROP POLICY IF EXISTS parsed_tables_update ON parsed_tables;
CREATE POLICY parsed_tables_update ON parsed_tables
    FOR UPDATE
    USING (org_id::text = current_setting('app.current_org_id', true))
    WITH CHECK (org_id::text = current_setting('app.current_org_id', true));

DROP POLICY IF EXISTS documents_insert ON documents;
CREATE POLICY documents_insert ON documents
    FOR INSERT
    WITH CHECK (org_id::text = current_setting('app.current_org_id', true));

DROP POLICY IF EXISTS documents_update ON documents;
CREATE POLICY documents_update ON documents
    FOR UPDATE
    USING (org_id::text = current_setting('app.current_org_id', true))
    WITH CHECK (org_id::text = current_setting('app.current_org_id', true));

DROP POLICY IF EXISTS mapping_rules_insert ON mapping_rules;
CREATE POLICY mapping_rules_insert ON mapping_rules
    FOR INSERT
    WITH CHECK (
        template_id IN (
            SELECT id FROM mapping_templates
            WHERE org_id IS NULL
               OR org_id::text = current_setting('app.current_org_id', true)
        )
    );

DROP POLICY IF EXISTS mapping_history_insert ON mapping_history;
CREATE POLICY mapping_history_insert ON mapping_history
    FOR INSERT
    WITH CHECK (org_id::text = current_setting('app.current_org_id', true));

DROP POLICY IF EXISTS mapping_usage_tracking_insert ON mapping_usage_tracking;
CREATE POLICY mapping_usage_tracking_insert ON mapping_usage_tracking
    FOR INSERT
    WITH CHECK (org_id::text = current_setting('app.current_org_id', true));

DROP POLICY IF EXISTS search_index_insert ON search_index;
CREATE POLICY search_index_insert ON search_index
    FOR INSERT
    WITH CHECK (org_id::text = current_setting('app.current_org_id', true));

DROP POLICY IF EXISTS search_index_update ON search_index;
CREATE POLICY search_index_update ON search_index
    FOR UPDATE
    USING (org_id::text = current_setting('app.current_org_id', true))
    WITH CHECK (org_id::text = current_setting('app.current_org_id', true));

DROP POLICY IF EXISTS promoted_tables_registry_insert ON promoted_tables_registry;
CREATE POLICY promoted_tables_registry_insert ON promoted_tables_registry
    FOR INSERT
    WITH CHECK (
        mapping_template_id IN (
            SELECT id FROM mapping_templates
            WHERE org_id IS NULL
               OR org_id::text = current_setting('app.current_org_id', true)
        )
    );

DROP POLICY IF EXISTS sink_corrections_insert ON sink_corrections;
CREATE POLICY sink_corrections_insert ON sink_corrections
    FOR INSERT
    WITH CHECK (org_id::text = current_setting('app.current_org_id', true));

DROP POLICY IF EXISTS sink_corrections_update ON sink_corrections;
CREATE POLICY sink_corrections_update ON sink_corrections
    FOR UPDATE
    USING (org_id::text = current_setting('app.current_org_id', true))
    WITH CHECK (org_id::text = current_setting('app.current_org_id', true));

DROP POLICY IF EXISTS sink_selections_insert ON sink_selections;
CREATE POLICY sink_selections_insert ON sink_selections
    FOR INSERT
    WITH CHECK (org_id::text = current_setting('app.current_org_id', true));

DROP POLICY IF EXISTS sink_selections_update ON sink_selections;
CREATE POLICY sink_selections_update ON sink_selections
    FOR UPDATE
    USING (org_id::text = current_setting('app.current_org_id', true))
    WITH CHECK (org_id::text = current_setting('app.current_org_id', true));

DROP POLICY IF EXISTS learning_log_insert ON extraction_learning_log;
CREATE POLICY learning_log_insert ON extraction_learning_log
    FOR INSERT
    WITH CHECK (org_id::text = current_setting('app.current_org_id', true));
