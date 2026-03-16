-- V3__seed_local_templates.sql
-- P6-W3-001: Seed data for LOCAL scope PPTX templates
-- Sample local templates that subsidiaries can create and use

-- Temporarily disable RLS so seed data can be inserted by the migration user
ALTER TABLE IF EXISTS pptx_templates DISABLE ROW LEVEL SECURITY;

-- Widen scope column to accommodate 'SHARED_WITHIN_HOLDING' (21 chars)
DROP POLICY IF EXISTS pptx_templates_scope_access ON pptx_templates;
ALTER TABLE pptx_templates ALTER COLUMN scope TYPE VARCHAR(30);
CREATE POLICY pptx_templates_scope_access ON pptx_templates
    USING (
        current_setting('app.current_role', TRUE) = 'HOLDING_ADMIN'
        OR scope = 'CENTRAL'
        OR (scope = 'LOCAL' AND owner_org_id = current_setting('app.current_org_id', TRUE))
        OR (scope = 'SHARED_WITHIN_HOLDING'
            AND current_setting('app.current_role', TRUE) IN ('ADMIN', 'COMPANY_ADMIN'))
    );

-- ============================================================================
-- LOCAL SCOPE TEMPLATES - Company-specific report templates
-- ============================================================================

-- Insert sample LOCAL scope template for COMPANY-A
INSERT INTO pptx_templates (id, org_id, name, description, scope, report_type, is_active, created_by)
VALUES (
    'c1d2e3f4-0003-0001-0001-000000000001',
    'COMPANY-A',
    'Company A Monthly Report',
    'Monthly performance report template specific to COMPANY-A operations',
    'LOCAL',
    'MONTHLY_REPORT',
    true,
    'admin@companya.com'
) ON CONFLICT (id) DO NOTHING;

-- Insert sample LOCAL scope template for COMPANY-B
INSERT INTO pptx_templates (id, org_id, name, description, scope, report_type, is_active, created_by)
VALUES (
    'c1d2e3f4-0003-0002-0001-000000000001',
    'COMPANY-B',
    'Company B Quarterly Review',
    'Quarterly business review template for COMPANY-B',
    'LOCAL',
    'QUARTERLY_REVIEW',
    true,
    'admin@companyb.com'
) ON CONFLICT (id) DO NOTHING;

-- ============================================================================
-- SHARED_WITHIN_HOLDING TEMPLATES - Available to all subsidiaries
-- ============================================================================

-- Insert sample SHARED scope template
INSERT INTO pptx_templates (id, org_id, name, description, scope, report_type, is_active, created_by)
VALUES (
    'c1d2e3f4-0003-0003-0001-000000000001',
    'HOLDING-001',
    'Standard Holding Quarterly Report',
    'Standardized quarterly report template shared across all subsidiaries in the holding',
    'SHARED_WITHIN_HOLDING',
    'QUARTERLY_REPORT',
    true,
    'holding.admin@demo.com'
) ON CONFLICT (id) DO NOTHING;

-- ============================================================================
-- INDEXES for local scope queries
-- ============================================================================
CREATE INDEX IF NOT EXISTS idx_local_templates_owner_org ON pptx_templates (owner_org_id) WHERE scope = 'LOCAL';
CREATE INDEX IF NOT EXISTS idx_shared_templates_holding ON pptx_templates (org_id) WHERE scope = 'SHARED_WITHIN_HOLDING';

-- Re-enable RLS after seeding
ALTER TABLE IF EXISTS pptx_templates ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS pptx_templates FORCE ROW LEVEL SECURITY;
