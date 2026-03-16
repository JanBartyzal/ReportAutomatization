-- V3__seed_local_forms.sql
-- P6-W3-001: Seed data for LOCAL scope forms
-- Sample local forms that subsidiaries can create and manage themselves

-- Temporarily disable RLS so seed data can be inserted by the migration user
ALTER TABLE IF EXISTS forms DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS form_responses DISABLE ROW LEVEL SECURITY;

-- Widen scope column to accommodate 'SHARED_WITHIN_HOLDING' (21 chars)
-- Must drop and recreate the RLS policy that references this column
DROP POLICY IF EXISTS forms_scope_access ON forms;
ALTER TABLE forms ALTER COLUMN scope TYPE VARCHAR(30);
CREATE POLICY forms_scope_access ON forms
    USING (
        current_setting('app.current_role', TRUE) = 'HOLDING_ADMIN'
        OR scope = 'CENTRAL'
        OR (scope = 'LOCAL' AND owner_org_id = current_setting('app.current_org_id', TRUE))
        OR (scope = 'SHARED_WITHIN_HOLDING'
            AND current_setting('app.current_role', TRUE) IN ('ADMIN', 'COMPANY_ADMIN'))
    );

-- ============================================================================
-- LOCAL SCOPE FORMS - For subsidiary self-service
-- ============================================================================

-- Insert sample LOCAL scope form - Company-specific expense tracking
INSERT INTO forms (id, org_id, title, description, scope, status, owner_org_id, created_by)
VALUES (
    'a1b2c3d4-0003-0001-0001-000000000001',
    'COMPANY-A',
    'Local Expense Report 2026',
    'Monthly expense tracking form for COMPANY-A employees - local subsidiary use only',
    'LOCAL',
    'PUBLISHED',
    'COMPANY-A',
    'admin@companya.com'
) ON CONFLICT (id) DO NOTHING;

-- Create version 1 of the local form
INSERT INTO form_versions (id, form_id, version_number, schema_def, created_by)
VALUES (
    'b2c3d4e5-0003-0001-0001-000000000001',
    'a1b2c3d4-0003-0001-0001-000000000001',
    1,
    '{
        "title": "Local Expense Report",
        "description": "Monthly expense tracking",
        "currency": "CZK"
    }'::jsonb,
    'admin@companya.com'
) ON CONFLICT (id) DO NOTHING;

-- Local form fields
INSERT INTO form_fields (id, form_version_id, field_key, field_type, label, section, section_description, sort_order, required, properties)
VALUES 
-- Employee Info Section
('c3d4e5f6-0003-0001-0001-000000000001', 'b2c3d4e5-0003-0001-0001-000000000001', 'employee_info', 'text', 'Employee Information', 'Employee', 'Employee name and department', 1, true, '{"field_type": "section_header"}'::jsonb),

('d4e5f6a7-0003-0001-0001-000000000001', 'b2c3d4e5-0003-0001-0001-000000000001', 'employee_name', 'text', 'Employee Name', 'Employee', 'Full name', 2, true, '{}'::jsonb),

('e5f6a7b8-0003-0001-0001-000000000001', 'b2c3d4e5-0003-0001-0001-000000000001', 'department', 'text', 'Department', 'Employee', 'Cost center/department', 3, true, '{}'::jsonb),

-- Expense Details Section
('f6a7b8c9-0003-0001-0001-000000000001', 'b2c3d4e5-0003-0001-0001-000000000001', 'expense_details', 'text', 'Expense Details', 'Expenses', 'Itemized expense information', 10, false, '{"field_type": "section_header"}'::jsonb),

('a7b8c9d0-0003-0001-0001-000000000001', 'b2c3d4e5-0003-0001-0001-000000000001', 'expense_date', 'date', 'Expense Date', 'Expenses', 'Date of expense', 11, true, '{}'::jsonb),

('b8c9d0e1-0003-0001-0001-000000000001', 'b2c3d4e5-0003-0001-0001-000000000001', 'expense_type', 'dropdown', 'Expense Type', 'Expenses', 'Category of expense', 12, true, '{"options": ["Travel", "Meals", "Office Supplies", "Equipment", "Training", "Other"]}'::jsonb),

('c9d0e1f2-0003-0001-0001-000000000001', 'b2c3d4e5-0003-0001-0001-000000000001', 'expense_amount', 'number', 'Amount (CZK)', 'Expenses', 'Total amount', 13, true, '{"min": 0, "currency": "CZK", "format": "currency"}'::jsonb),

('d0e1f2a3-0003-0001-0001-000000000001', 'b2c3d4e5-0003-0001-0001-000000000001', 'expense_description', 'text', 'Description', 'Expenses', 'Brief description of expense', 14, true, '{"max_length": 500}'::jsonb),

('e1f2a3b4-0003-0001-0001-000000000001', 'b2c3d4e5-0003-0001-0001-000000000001', 'receipt_attached', 'dropdown', 'Receipt Attached?', 'Expenses', 'Is receipt available?', 15, true, '{"options": ["Yes", "No", "Pending"]}'::jsonb);

-- ============================================================================
-- Another LOCAL scope form - Company-B specific
-- ============================================================================

INSERT INTO forms (id, org_id, title, description, scope, status, owner_org_id, created_by)
VALUES (
    'a1b2c3d4-0003-0002-0001-000000000001',
    'COMPANY-B',
    'Local Sales Report Q1 2026',
    'Quarterly local sales tracking for COMPANY-B',
    'LOCAL',
    'PUBLISHED',
    'COMPANY-B',
    'admin@companyb.com'
) ON CONFLICT (id) DO NOTHING;

-- Create version 1
INSERT INTO form_versions (id, form_id, version_number, schema_def, created_by)
VALUES (
    'b2c3d4e5-0003-0002-0001-000000000001',
    'a1b2c3d4-0003-0002-0001-000000000001',
    1,
    '{"title": "Local Sales Report", "description": "Quarterly sales tracking"}'::jsonb,
    'admin@companyb.com'
) ON CONFLICT (id) DO NOTHING;

-- Local sales form fields
INSERT INTO form_fields (id, form_version_id, field_key, field_type, label, section, section_description, sort_order, required, properties)
VALUES 
('f2a3b4c5-0003-0002-0001-000000000001', 'b2c3d4e5-0003-0002-0001-000000000001', 'sales_header', 'text', 'Sales Data', 'Sales', 'Quarterly sales figures', 1, false, '{"field_type": "section_header"}'::jsonb),

('a3b4c5d6-0003-0002-0001-000000000001', 'b2c3d4e5-0003-0002-0001-000000000001', 'revenue_total', 'number', 'Total Revenue (CZK)', 'Sales', 'Total quarterly revenue', 2, true, '{"min": 0, "currency": "CZK", "format": "currency"}'::jsonb),

('b4c5d6e7-0003-0002-0001-000000000001', 'b2c3d4e5-0003-0002-0001-000000000001', 'new_customers', 'number', 'New Customers', 'Sales', 'Number of new customers acquired', 3, true, '{"min": 0}'::jsonb),

('c5d6e7f8-0003-0002-0001-000000000001', 'b2c3d4e5-0003-0002-0001-000000000001', 'customer_count', 'number', 'Total Customers', 'Sales', 'Total active customers', 4, true, '{"min": 0}'::jsonb),

('d6e7f8a9-0003-0002-0001-000000000001', 'b2c3d4e5-0003-0002-0001-000000000001', 'sales_notes', 'text', 'Notes', 'Sales', 'Additional sales comments', 5, false, '{"max_length": 1000}'::jsonb);

-- ============================================================================
-- SHARED_WITHIN_HOLDING scope form - Available to all subsidiaries in holding
-- ============================================================================

INSERT INTO forms (id, org_id, title, description, scope, status, owner_org_id, created_by)
VALUES (
    'a1b2c3d4-0003-0003-0001-000000000001',
    'HOLDING-001',
    'Shared KPI Metrics 2026',
    'Standardized KPI tracking shared across all subsidiaries in the holding',
    'SHARED_WITHIN_HOLDING',
    'PUBLISHED',
    'HOLDING-001',
    'holding.admin@demo.com'
) ON CONFLICT (id) DO NOTHING;

-- Create version 1
INSERT INTO form_versions (id, form_id, version_number, schema_def, created_by)
VALUES (
    'b2c3d4e5-0003-0003-0001-000000000001',
    'a1b2c3d4-0003-0003-0001-000000000001',
    1,
    '{"title": "Shared KPI Metrics", "description": "Standardized holding-wide KPIs"}'::jsonb,
    'holding.admin@demo.com'
) ON CONFLICT (id) DO NOTHING;

-- Shared KPI form fields
INSERT INTO form_fields (id, form_version_id, field_key, field_type, label, section, section_description, sort_order, required, properties)
VALUES 
('e7f8a9b0-0003-0003-0001-000000000001', 'b2c3d4e5-0003-0003-0001-000000000001', 'kpi_header', 'text', 'Key Performance Indicators', 'KPIs', 'Holding-wide standardized metrics', 1, false, '{"field_type": "section_header"}'::jsonb),

('f8a9b0c1-0003-0003-0001-000000000001', 'b2c3d4e5-0003-0003-0001-000000000001', 'kpi_revenue_growth', 'percentage', 'Revenue Growth %', 'KPIs', 'Year-over-year revenue growth', 2, true, '{"min": -100, "max": 1000}'::jsonb),

('a9b0c1d2-0003-0003-0001-000000000001', 'b2c3d4e5-0003-0003-0001-000000000001', 'kpi_ebitda_margin', 'percentage', 'EBITDA Margin %', 'KPIs', 'Profitability metric', 3, true, '{"min": -50, "max": 100}'::jsonb),

('b0c1d2e3-0003-0003-0001-000000000001', 'b2c3d4e5-0003-0003-0001-000000000001', 'kpi_headcount', 'number', 'Total Headcount', 'KPIs', 'Full-time employees', 4, true, '{"min": 0}'::jsonb),

('c1d2e3f4-0003-0003-0001-000000000001', 'b2c3d4e5-0003-0003-0001-000000000001', 'kpi_customer_satisfaction', 'percentage', 'Customer Satisfaction %', 'KPIs', 'CSAT score', 5, true, '{"min": 0, "max": 100}'::jsonb),

('d2e3f4a5-0003-0003-0001-000000000001', 'b2c3d4e5-0003-0003-0001-000000000001', 'kpi_notes', 'text', 'Comments', 'KPIs', 'Additional context on KPIs', 6, false, '{"max_length": 500}'::jsonb);

-- ============================================================================
-- INDEXES for local scope queries
-- ============================================================================
CREATE INDEX IF NOT EXISTS idx_local_forms_owner_org ON forms (owner_org_id) WHERE scope = 'LOCAL';
CREATE INDEX IF NOT EXISTS idx_shared_forms_holding ON forms (org_id) WHERE scope = 'SHARED_WITHIN_HOLDING';

-- Re-enable RLS after seeding
ALTER TABLE IF EXISTS forms ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS forms FORCE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS form_responses ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS form_responses FORCE ROW LEVEL SECURITY;
