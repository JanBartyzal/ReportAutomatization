-- P11-W1-004: RLS Policies for engine-reporting tables
-- Add missing RLS policies to tables that don't have them

-- =============================================================================
-- REPORTS - org isolation
-- =============================================================================

ALTER TABLE IF EXISTS reports ENABLE ROW LEVEL SECURITY;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies WHERE policyname = 'reports_org_isolation' AND tablename = 'reports'
    ) THEN
        CREATE POLICY reports_org_isolation ON reports
            FOR SELECT
            USING (org_id::text = current_setting('app.current_org_id', true));
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies WHERE policyname = 'reports_insert_org' AND tablename = 'reports'
    ) THEN
        CREATE POLICY reports_insert_org ON reports
            FOR INSERT
            WITH CHECK (org_id::text = current_setting('app.current_org_id', true));
    END IF;
END $$;

ALTER TABLE reports FORCE ROW LEVEL SECURITY;

-- =============================================================================
-- REPORT_STATUS_HISTORY - org isolation via reports join
-- =============================================================================

ALTER TABLE IF EXISTS report_status_history ENABLE ROW LEVEL SECURITY;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies WHERE policyname = 'report_status_history_org_isolation' AND tablename = 'report_status_history'
    ) THEN
        CREATE POLICY report_status_history_org_isolation ON report_status_history
            FOR SELECT
            USING (
                report_id IN (
                    SELECT id FROM reports WHERE org_id::text = current_setting('app.current_org_id', true)
                )
            );
    END IF;
END $$;

ALTER TABLE report_status_history FORCE ROW LEVEL SECURITY;

-- =============================================================================
-- SUBMISSION_CHECKLISTS - org isolation via reports join
-- =============================================================================

ALTER TABLE IF EXISTS submission_checklists ENABLE ROW LEVEL SECURITY;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies WHERE policyname = 'submission_checklists_org_isolation' AND tablename = 'submission_checklists'
    ) THEN
        CREATE POLICY submission_checklists_org_isolation ON submission_checklists
            FOR SELECT
            USING (
                report_id IN (
                    SELECT id FROM reports WHERE org_id::text = current_setting('app.current_org_id', true)
                )
            );
    END IF;
END $$;

ALTER TABLE submission_checklists FORCE ROW LEVEL SECURITY;

-- =============================================================================
-- PERIOD_ORG_ASSIGNMENTS - org isolation
-- =============================================================================

ALTER TABLE IF EXISTS period_org_assignments ENABLE ROW LEVEL SECURITY;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies WHERE policyname = 'period_org_assignments_org_isolation' AND tablename = 'period_org_assignments'
    ) THEN
        CREATE POLICY period_org_assignments_org_isolation ON period_org_assignments
            FOR SELECT
            USING (org_id::text = current_setting('app.current_org_id', true));
    END IF;
END $$;

ALTER TABLE period_org_assignments FORCE ROW LEVEL SECURITY;

-- =============================================================================
-- FORM_VERSIONS - org isolation via forms join
-- =============================================================================

ALTER TABLE IF EXISTS form_versions ENABLE ROW LEVEL SECURITY;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies WHERE policyname = 'form_versions_org_isolation' AND tablename = 'form_versions'
    ) THEN
        CREATE POLICY form_versions_org_isolation ON form_versions
            FOR SELECT
            USING (
                form_id IN (
                    SELECT id FROM forms WHERE org_id::text = current_setting('app.current_org_id', true)
                )
            );
    END IF;
END $$;

ALTER TABLE form_versions FORCE ROW LEVEL SECURITY;

-- =============================================================================
-- FORM_FIELDS - org isolation via forms join
-- =============================================================================

ALTER TABLE IF EXISTS form_fields ENABLE ROW LEVEL SECURITY;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies WHERE policyname = 'form_fields_org_isolation' AND tablename = 'form_fields'
    ) THEN
        CREATE POLICY form_fields_org_isolation ON form_fields
            FOR SELECT
            USING (
                form_id IN (
                    SELECT id FROM forms WHERE org_id::text = current_setting('app.current_org_id', true)
                )
            );
    END IF;
END $$;

ALTER TABLE form_fields FORCE ROW LEVEL SECURITY;

-- =============================================================================
-- FORM_FIELD_VALUES - org isolation via form_responses join
-- =============================================================================

ALTER TABLE IF EXISTS form_field_values ENABLE ROW LEVEL SECURITY;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies WHERE policyname = 'form_field_values_org_isolation' AND tablename = 'form_field_values'
    ) THEN
        CREATE POLICY form_field_values_org_isolation ON form_field_values
            FOR SELECT
            USING (
                response_id IN (
                    SELECT id FROM form_responses WHERE org_id::text = current_setting('app.current_org_id', true)
                )
            );
    END IF;
END $$;

ALTER TABLE form_field_values FORCE ROW LEVEL SECURITY;

-- =============================================================================
-- FORM_FIELD_COMMENTS - org isolation via form_responses join
-- =============================================================================

ALTER TABLE IF EXISTS form_field_comments ENABLE ROW LEVEL SECURITY;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies WHERE policyname = 'form_field_comments_org_isolation' AND tablename = 'form_field_comments'
    ) THEN
        CREATE POLICY form_field_comments_org_isolation ON form_field_comments
            FOR SELECT
            USING (
                response_id IN (
                    SELECT id FROM form_responses WHERE org_id::text = current_setting('app.current_org_id', true)
                )
            );
    END IF;
END $$;

ALTER TABLE form_field_comments FORCE ROW LEVEL SECURITY;

-- =============================================================================
-- FORM_ASSIGNMENTS - org isolation
-- =============================================================================

ALTER TABLE IF EXISTS form_assignments ENABLE ROW LEVEL SECURITY;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies WHERE policyname = 'form_assignments_org_isolation' AND tablename = 'form_assignments'
    ) THEN
        CREATE POLICY form_assignments_org_isolation ON form_assignments
            FOR SELECT
            USING (org_id::text = current_setting('app.current_org_id', true));
    END IF;
END $$;

ALTER TABLE form_assignments FORCE ROW LEVEL SECURITY;

-- =============================================================================
-- TEMPLATE_VERSIONS - org isolation via pptx_templates join
-- =============================================================================

ALTER TABLE IF EXISTS template_versions ENABLE ROW LEVEL SECURITY;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies WHERE policyname = 'template_versions_org_isolation' AND tablename = 'template_versions'
    ) THEN
        CREATE POLICY template_versions_org_isolation ON template_versions
            FOR SELECT
            USING (
                template_id IN (
                    SELECT id FROM pptx_templates WHERE org_id::text = current_setting('app.current_org_id', true)
                )
            );
    END IF;
END $$;

ALTER TABLE template_versions FORCE ROW LEVEL SECURITY;

-- =============================================================================
-- TEMPLATE_PLACEHOLDERS - org isolation via pptx_templates join
-- =============================================================================

ALTER TABLE IF EXISTS template_placeholders ENABLE ROW LEVEL SECURITY;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies WHERE policyname = 'template_placeholders_org_isolation' AND tablename = 'template_placeholders'
    ) THEN
        CREATE POLICY template_placeholders_org_isolation ON template_placeholders
            FOR SELECT
            USING (
                template_id IN (
                    SELECT id FROM pptx_templates WHERE org_id::text = current_setting('app.current_org_id', true)
                )
            );
    END IF;
END $$;

ALTER TABLE template_placeholders FORCE ROW LEVEL SECURITY;

-- =============================================================================
-- PLACEHOLDER_MAPPINGS - org isolation via pptx_templates join
-- =============================================================================

ALTER TABLE IF EXISTS placeholder_mappings ENABLE ROW LEVEL SECURITY;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies WHERE policyname = 'placeholder_mappings_org_isolation' AND tablename = 'placeholder_mappings'
    ) THEN
        CREATE POLICY placeholder_mappings_org_isolation ON placeholder_mappings
            FOR SELECT
            USING (
                template_id IN (
                    SELECT id FROM pptx_templates WHERE org_id::text = current_setting('app.current_org_id', true)
                )
            );
    END IF;
END $$;

ALTER TABLE placeholder_mappings FORCE ROW LEVEL SECURITY;

-- =============================================================================
-- NOTIFICATIONS - user_id + org_id
-- =============================================================================

ALTER TABLE IF EXISTS notifications ENABLE ROW LEVEL SECURITY;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies WHERE policyname = 'notifications_user_access' AND tablename = 'notifications'
    ) THEN
        CREATE POLICY notifications_user_access ON notifications
            FOR SELECT
            USING (
                user_id::text = current_setting('app.current_user_id', true)
                OR org_id::text = current_setting('app.current_org_id', true)
            );
    END IF;
END $$;

ALTER TABLE notifications FORCE ROW LEVEL SECURITY;

-- =============================================================================
-- NOTIFICATION_SETTINGS - user_id
-- =============================================================================

ALTER TABLE IF EXISTS notification_settings ENABLE ROW LEVEL SECURITY;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies WHERE policyname = 'notification_settings_user_access' AND tablename = 'notification_settings'
    ) THEN
        CREATE POLICY notification_settings_user_access ON notification_settings
            FOR SELECT
            USING (user_id::text = current_setting('app.current_user_id', true));
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies WHERE policyname = 'notification_settings_insert_user' AND tablename = 'notification_settings'
    ) THEN
        CREATE POLICY notification_settings_insert_user ON notification_settings
            FOR INSERT
            WITH CHECK (user_id::text = current_setting('app.current_user_id', true));
    END IF;
END $$;

ALTER TABLE notification_settings FORCE ROW LEVEL SECURITY;

-- =============================================================================
-- FEATURE_FLAGS - global, admin only
-- =============================================================================

ALTER TABLE IF EXISTS feature_flags ENABLE ROW LEVEL SECURITY;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies WHERE policyname = 'feature_flags_admin_only' AND tablename = 'feature_flags'
    ) THEN
        CREATE POLICY feature_flags_admin_only ON feature_flags
            FOR SELECT
            USING (
                current_setting('app.current_user_roles', true) LIKE '%ADMIN%' 
                OR current_setting('app.current_user_roles', true) LIKE '%HOLDING_ADMIN%'
            );
    END IF;
END $$;

ALTER TABLE feature_flags FORCE ROW LEVEL SECURITY;
