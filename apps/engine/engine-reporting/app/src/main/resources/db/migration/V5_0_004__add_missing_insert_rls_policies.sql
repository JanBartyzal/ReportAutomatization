-- V5_0_004: Add missing INSERT/UPDATE RLS policies
-- Many tables have FORCE ROW LEVEL SECURITY but only SELECT policies,
-- blocking all INSERT/UPDATE operations from the application.

-- submission_checklists: app inserts checklist when report is created
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'submission_checklists_insert' AND tablename = 'submission_checklists') THEN
        CREATE POLICY submission_checklists_insert ON submission_checklists FOR INSERT WITH CHECK (true);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'submission_checklists_update' AND tablename = 'submission_checklists') THEN
        CREATE POLICY submission_checklists_update ON submission_checklists FOR UPDATE USING (true);
    END IF;
END $$;

-- report_status_history: app inserts history on state transitions
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'report_status_history_insert' AND tablename = 'report_status_history') THEN
        CREATE POLICY report_status_history_insert ON report_status_history FOR INSERT WITH CHECK (true);
    END IF;
END $$;

-- form_versions: app inserts when form is created/updated
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'form_versions_insert' AND tablename = 'form_versions') THEN
        CREATE POLICY form_versions_insert ON form_versions FOR INSERT WITH CHECK (true);
    END IF;
END $$;

-- form_fields: app inserts field definitions per version
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'form_fields_insert' AND tablename = 'form_fields') THEN
        CREATE POLICY form_fields_insert ON form_fields FOR INSERT WITH CHECK (true);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'form_fields_delete' AND tablename = 'form_fields') THEN
        CREATE POLICY form_fields_delete ON form_fields FOR DELETE USING (true);
    END IF;
END $$;

-- form_field_values: app inserts values when response is submitted
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'form_field_values_insert' AND tablename = 'form_field_values') THEN
        CREATE POLICY form_field_values_insert ON form_field_values FOR INSERT WITH CHECK (true);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'form_field_values_update' AND tablename = 'form_field_values') THEN
        CREATE POLICY form_field_values_update ON form_field_values FOR UPDATE USING (true);
    END IF;
END $$;

-- form_field_comments: app inserts comments
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'form_field_comments_insert' AND tablename = 'form_field_comments') THEN
        CREATE POLICY form_field_comments_insert ON form_field_comments FOR INSERT WITH CHECK (true);
    END IF;
END $$;

-- form_assignments: app assigns orgs to forms
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'form_assignments_insert' AND tablename = 'form_assignments') THEN
        CREATE POLICY form_assignments_insert ON form_assignments FOR INSERT WITH CHECK (true);
    END IF;
END $$;

-- period_org_assignments: app assigns orgs to periods
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'period_org_assignments_insert' AND tablename = 'period_org_assignments') THEN
        CREATE POLICY period_org_assignments_insert ON period_org_assignments FOR INSERT WITH CHECK (true);
    END IF;
END $$;

-- template_versions: app creates template versions
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'template_versions_insert' AND tablename = 'template_versions') THEN
        CREATE POLICY template_versions_insert ON template_versions FOR INSERT WITH CHECK (true);
    END IF;
END $$;

-- template_placeholders: app creates placeholders
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'template_placeholders_insert' AND tablename = 'template_placeholders') THEN
        CREATE POLICY template_placeholders_insert ON template_placeholders FOR INSERT WITH CHECK (true);
    END IF;
END $$;

-- placeholder_mappings: app creates mappings
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'placeholder_mappings_insert' AND tablename = 'placeholder_mappings') THEN
        CREATE POLICY placeholder_mappings_insert ON placeholder_mappings FOR INSERT WITH CHECK (true);
    END IF;
END $$;

-- notifications: app inserts notifications
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'notifications_insert' AND tablename = 'notifications') THEN
        CREATE POLICY notifications_insert ON notifications FOR INSERT WITH CHECK (true);
    END IF;
END $$;

-- notifications: app updates read status
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'notifications_update' AND tablename = 'notifications') THEN
        CREATE POLICY notifications_update ON notifications FOR UPDATE USING (true);
    END IF;
END $$;

-- reports: app updates report status
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'reports_update' AND tablename = 'reports') THEN
        CREATE POLICY reports_update ON reports FOR UPDATE USING (
            org_id::text = current_setting('app.current_org_id', true)
        );
    END IF;
END $$;
