-- Tighten write-side RLS policies that previously trusted all application writes.
-- Each policy now mirrors the table's tenant or owner relationship.

DROP POLICY IF EXISTS submission_checklists_insert ON submission_checklists;
CREATE POLICY submission_checklists_insert ON submission_checklists
    FOR INSERT
    WITH CHECK (
        report_id IN (
            SELECT id FROM reports
            WHERE org_id::text = current_setting('app.current_org_id', true)
        )
    );

DROP POLICY IF EXISTS submission_checklists_update ON submission_checklists;
CREATE POLICY submission_checklists_update ON submission_checklists
    FOR UPDATE
    USING (
        report_id IN (
            SELECT id FROM reports
            WHERE org_id::text = current_setting('app.current_org_id', true)
        )
    )
    WITH CHECK (
        report_id IN (
            SELECT id FROM reports
            WHERE org_id::text = current_setting('app.current_org_id', true)
        )
    );

DROP POLICY IF EXISTS report_status_history_insert ON report_status_history;
CREATE POLICY report_status_history_insert ON report_status_history
    FOR INSERT
    WITH CHECK (
        report_id IN (
            SELECT id FROM reports
            WHERE org_id::text = current_setting('app.current_org_id', true)
        )
    );

DROP POLICY IF EXISTS form_versions_insert ON form_versions;
CREATE POLICY form_versions_insert ON form_versions
    FOR INSERT
    WITH CHECK (
        form_id IN (
            SELECT id FROM forms
            WHERE org_id::text = current_setting('app.current_org_id', true)
        )
    );

DROP POLICY IF EXISTS form_fields_insert ON form_fields;
CREATE POLICY form_fields_insert ON form_fields
    FOR INSERT
    WITH CHECK (
        form_version_id IN (
            SELECT fv.id
            FROM form_versions fv
            JOIN forms f ON fv.form_id = f.id
            WHERE f.org_id::text = current_setting('app.current_org_id', true)
        )
    );

DROP POLICY IF EXISTS form_fields_delete ON form_fields;
CREATE POLICY form_fields_delete ON form_fields
    FOR DELETE
    USING (
        form_version_id IN (
            SELECT fv.id
            FROM form_versions fv
            JOIN forms f ON fv.form_id = f.id
            WHERE f.org_id::text = current_setting('app.current_org_id', true)
        )
    );

DROP POLICY IF EXISTS form_field_values_insert ON form_field_values;
CREATE POLICY form_field_values_insert ON form_field_values
    FOR INSERT
    WITH CHECK (
        response_id IN (
            SELECT id FROM form_responses
            WHERE org_id::text = current_setting('app.current_org_id', true)
        )
    );

DROP POLICY IF EXISTS form_field_values_update ON form_field_values;
CREATE POLICY form_field_values_update ON form_field_values
    FOR UPDATE
    USING (
        response_id IN (
            SELECT id FROM form_responses
            WHERE org_id::text = current_setting('app.current_org_id', true)
        )
    )
    WITH CHECK (
        response_id IN (
            SELECT id FROM form_responses
            WHERE org_id::text = current_setting('app.current_org_id', true)
        )
    );

DROP POLICY IF EXISTS form_field_comments_insert ON form_field_comments;
CREATE POLICY form_field_comments_insert ON form_field_comments
    FOR INSERT
    WITH CHECK (
        response_id IN (
            SELECT id FROM form_responses
            WHERE org_id::text = current_setting('app.current_org_id', true)
        )
    );

DROP POLICY IF EXISTS form_assignments_insert ON form_assignments;
CREATE POLICY form_assignments_insert ON form_assignments
    FOR INSERT
    WITH CHECK (org_id::text = current_setting('app.current_org_id', true));

DROP POLICY IF EXISTS period_org_assignments_insert ON period_org_assignments;
CREATE POLICY period_org_assignments_insert ON period_org_assignments
    FOR INSERT
    WITH CHECK (org_id::text = current_setting('app.current_org_id', true));

DROP POLICY IF EXISTS template_versions_insert ON template_versions;
CREATE POLICY template_versions_insert ON template_versions
    FOR INSERT
    WITH CHECK (
        template_id IN (
            SELECT id FROM pptx_templates
            WHERE org_id::text = current_setting('app.current_org_id', true)
        )
    );

DROP POLICY IF EXISTS template_placeholders_insert ON template_placeholders;
CREATE POLICY template_placeholders_insert ON template_placeholders
    FOR INSERT
    WITH CHECK (
        version_id IN (
            SELECT tv.id
            FROM template_versions tv
            JOIN pptx_templates pt ON tv.template_id = pt.id
            WHERE pt.org_id::text = current_setting('app.current_org_id', true)
        )
    );

DROP POLICY IF EXISTS placeholder_mappings_insert ON placeholder_mappings;
CREATE POLICY placeholder_mappings_insert ON placeholder_mappings
    FOR INSERT
    WITH CHECK (
        template_id IN (
            SELECT id FROM pptx_templates
            WHERE org_id::text = current_setting('app.current_org_id', true)
        )
    );

DROP POLICY IF EXISTS notifications_insert ON notifications;
CREATE POLICY notifications_insert ON notifications
    FOR INSERT
    WITH CHECK (user_id::text = current_setting('app.current_user_id', true));

DROP POLICY IF EXISTS notifications_update ON notifications;
CREATE POLICY notifications_update ON notifications
    FOR UPDATE
    USING (user_id::text = current_setting('app.current_user_id', true))
    WITH CHECK (user_id::text = current_setting('app.current_user_id', true));
