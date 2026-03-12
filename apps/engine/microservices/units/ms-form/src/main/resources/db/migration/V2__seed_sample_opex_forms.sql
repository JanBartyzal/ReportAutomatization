-- V2__seed_sample_opex_forms.sql
-- Sample OPEX forms for testing and demo purposes

-- ============================================================================
-- SAMPLE OPEX FORM - Budget Collection
-- ============================================================================

-- Create sample organization (for demo)
-- Note: In production, orgs come from MS-BATCH/MS-AUTH

-- Insert sample OPEX form
INSERT INTO forms (id, org_id, title, description, scope, status, created_by)
VALUES (
    'a1b2c3d4-0001-0001-0001-000000000001',
    'HOLDING-001',
    'OPEX Budget 2026 - Quarterly Collection',
    'Quarterly OPEX budget collection form covering Personnel, IT, Office, and Travel expenses',
    'CENTRAL',
    'PUBLISHED',
    'system@demo.com'
) ON CONFLICT (id) DO NOTHING;

-- Create version 1 of the form
INSERT INTO form_versions (id, form_id, version_number, schema_def, created_by)
VALUES (
    'b2c3d4e5-0001-0001-0001-000000000001',
    'a1b2c3d4-0001-0001-0001-000000000001',
    1,
    '{
        "title": "OPEX Budget 2026 - Q1",
        "description": "Quarterly OPEX budget collection",
        "currency": "CZK"
    }'::jsonb,
    'system@demo.com'
) ON CONFLICT (id) DO NOTHING;

-- ============================================================================
-- SECTION 1: PERSONNEL
-- ============================================================================
INSERT INTO form_fields (id, form_version_id, field_key, field_type, label, section, section_description, sort_order, required, properties)
VALUES 
-- Personnel Section Header
('c3d4e5f6-0001-0001-0001-000000000001', 'b2c3d4e5-0001-0001-0001-000000000001', 'personnel_header', 'text', 'Personnel Costs', 'Personnel', 'All personnel-related expenses', 1, false, '{"field_type": "section_header"}'::jsonb),

-- Headcount
('d4e5f6a7-0001-0001-0001-000000000001', 'b2c3d4e5-0001-0001-0001-000000000001', 'headcount', 'number', 'Total Headcount', 'Personnel', 'Total number of employees', 2, true, '{"min": 0, "unit": "employees"}'::jsonb),

-- Salaries
('e5f6a7b8-0001-0001-0001-000000000001', 'b2c3d4e5-0001-0001-0001-000000000001', 'salaries_total', 'number', 'Total Salaries (CZK)', 'Personnel', 'Total salary budget including bonuses', 3, true, '{"min": 0, "currency": "CZK", "format": "currency"}'::jsonb),

-- Personnel Other
('f6a7b8c9-0001-0001-0001-000000000001', 'b2c3d4e5-0001-0001-0001-000000000001', 'personnel_other', 'number', 'Other Personnel Costs (CZK)', 'Personnel', 'Training, benefits, insurance', 4, false, '{"min": 0, "currency": "CZK", "format": "currency"}'::jsonb),

-- ============================================================================
-- SECTION 2: IT
-- ============================================================================
-- IT Section Header
('a7b8c9d0-0001-0001-0001-000000000001', 'b2c3d4e5-0001-0001-0001-000000000001', 'it_header', 'text', 'IT & Infrastructure', 'IT', 'Technology and infrastructure expenses', 10, false, '{"field_type": "section_header"}'::jsonb),

-- Hardware
('b8c9d0e1-0001-0001-0001-000000000001', 'b2c3d4e5-0001-0001-0001-000000000001', 'it_hardware', 'number', 'Hardware & Equipment (CZK)', 'IT', 'Computers, phones, servers', 11, false, '{"min": 0, "currency": "CZK", "format": "currency"}'::jsonb),

-- Software
('c9d0e1f2-0001-0001-0001-000000000001', 'b2c3d4e5-0001-0001-0001-000000000001', 'it_software', 'number', 'Software Licenses (CZK)', 'IT', 'Annual software subscriptions', 12, false, '{"min": 0, "currency": "CZK", "format": "currency"}'::jsonb),

-- Cloud Services
('d0e1f2a3-0001-0001-0001-000000000001', 'b2c3d4e5-0001-0001-0001-000000000001', 'it_cloud', 'number', 'Cloud Services (CZK)', 'IT', 'AWS, Azure, hosting', 13, false, '{"min": 0, "currency": "CZK", "format": "currency"}'::jsonb),

-- IT Support
('e1f2a3b4-0001-0001-0001-000000000001', 'b2c3d4e5-0001-0001-0001-000000000001', 'it_support', 'number', 'IT Support Services (CZK)', 'IT', 'External support contracts', 14, false, '{"min": 0, "currency": "CZK", "format": "currency"}'::jsonb),

-- ============================================================================
-- SECTION 3: OFFICE
-- ============================================================================
-- Office Section Header
('f2a3b4c5-0001-0001-0001-000000000001', 'b2c3d4e5-0001-0001-0001-000000000001', 'office_header', 'text', 'Office & Operations', 'Office', 'Physical office and operational costs', 20, false, '{"field_type": "section_header"}'::jsonb),

-- Rent
('a3b4c5d6-0001-0001-0001-000000000001', 'b2c3d4e5-0001-0001-0001-000000000001', 'office_rent', 'number', 'Office Rent (CZK)', 'Office', 'Monthly rent expenses', 21, false, '{"min": 0, "currency": "CZK", "format": "currency"}'::jsonb),

-- Utilities
('b4c5d6e7-0001-0001-0001-000000000001', 'b2c3d4e5-0001-0001-0001-000000000001', 'office_utilities', 'number', 'Utilities (CZK)', 'Office', 'Electricity, water, heating', 22, false, '{"min": 0, "currency": "CZK", "format": "currency"}'::jsonb),

-- Office Supplies
('c5d6e7f8-0001-0001-0001-000000000001', 'b2c3d4e5-0001-0001-0001-000000000001', 'office_supplies', 'number', 'Office Supplies (CZK)', 'Office', 'Stationery, equipment', 23, false, '{"min": 0, "currency": "CZK", "format": "currency"}'::jsonb),

-- Insurance
('d6e7f8a9-0001-0001-0001-000000000001', 'b2c3d4e5-0001-0001-0001-000000000001', 'office_insurance', 'number', 'Business Insurance (CZK)', 'Office', 'Liability, property insurance', 24, false, '{"min": 0, "currency": "CZK", "format": "currency"}'::jsonb),

-- ============================================================================
-- SECTION 4: TRAVEL
-- ============================================================================
-- Travel Section Header
('e7f8a9b0-0001-0001-0001-000000000001', 'b2c3d4e5-0001-0001-0001-000000000001', 'travel_header', 'text', 'Travel & Entertainment', 'Travel', 'Business travel and client entertainment', 30, false, '{"field_type": "section_header"}'::jsonb),

-- Travel Domestic
('f8a9b0c1-0001-0001-0001-000000000001', 'b2c3d4e5-0001-0001-0001-000000000001', 'travel_domestic', 'number', 'Domestic Travel (CZK)', 'Travel', 'Within Czech Republic', 31, false, '{"min": 0, "currency": "CZK", "format": "currency"}'::jsonb),

-- Travel International
('a9b0c1d2-0001-0001-0001-000000000001', 'b2c3d4e5-0001-0001-0001-000000000001', 'travel_international', 'number', 'International Travel (CZK)', 'Travel', 'Outside Czech Republic', 32, false, '{"min": 0, "currency": "CZK", "format": "currency"}'::jsonb),

-- Entertainment
('b0c1d2e3-0001-0001-0001-000000000001', 'b2c3d4e5-0001-0001-0001-000000000001', 'travel_entertainment', 'number', 'Client Entertainment (CZK)', 'Travel', 'Business meals, events', 33, false, '{"min": 0, "currency": "CZK", "format": "currency"}'::jsonb),

-- ============================================================================
-- SUMMARY SECTION
-- ============================================================================
-- Total Budget (calculated)
('c1d2e3f4-0001-0001-0001-000000000001', 'b2c3d4e5-0001-0001-0001-000000000001', 'budget_total', 'number', 'Total Budget (CZK)', 'Summary', 'Auto-calculated total', 40, true, '{"currency": "CZK", "format": "currency", "calculated": true, "formula": "sum(all_number_fields)"}'::jsonb),

-- Category dropdown for classification
('d2e3f4a5-0001-0001-0001-000000000001', 'b2c3d4e5-0001-0001-0001-000000000001', 'budget_category', 'dropdown', 'Budget Category', 'Summary', 'Primary budget classification', 41, true, '{"options": ["Operations", "Capital Expenditure", "R&D", "Marketing", "Administrative"]}'::jsonb),

-- Notes
('e3f4a5b6-0001-0001-0001-000000000001', 'b2c3d4e5-0001-0001-0001-000000000001', 'budget_notes', 'text', 'Additional Notes', 'Summary', 'Any additional comments or explanations', 42, false, '{"max_length": 2000}'::jsonb);

-- ============================================================================
-- SAMPLE FORM RESPONSES (for testing)
-- ============================================================================

-- Sample response from Company A
INSERT INTO form_responses (id, form_id, form_version_id, org_id, period_id, user_id, status, data, submitted_at)
VALUES (
    'f3a4b5c6-0001-0001-0001-000000000001',
    'a1b2c3d4-0001-0001-0001-000000000001',
    'b2c3d4e5-0001-0001-0001-000000000001',
    'COMPANY-A',
    '2026-Q1',
    'user@companya.com',
    'SUBMITTED',
    '{
        "headcount": 150,
        "salaries_total": 45000000,
        "personnel_other": 2500000,
        "it_hardware": 1200000,
        "it_software": 800000,
        "it_cloud": 600000,
        "it_support": 300000,
        "office_rent": 1800000,
        "office_utilities": 450000,
        "office_supplies": 150000,
        "office_insurance": 200000,
        "travel_domestic": 180000,
        "travel_international": 350000,
        "travel_entertainment": 120000,
        "budget_total": 52850000,
        "budget_category": "Operations"
    }'::jsonb,
    NOW()
) ON CONFLICT (id) DO NOTHING;

-- Sample response from Company B
INSERT INTO form_responses (id, form_id, form_version_id, org_id, period_id, user_id, status, data, submitted_at)
VALUES (
    'a4b5c6d7-0001-0001-0001-000000000001',
    'a1b2c3d4-0001-0001-0001-000000000001',
    'b2c3d4e5-0001-0001-0001-000000000001',
    'COMPANY-B',
    '2026-Q1',
    'user@companyb.com',
    'SUBMITTED',
    '{
        "headcount": 85,
        "salaries_total": 28000000,
        "personnel_other": 1200000,
        "it_hardware": 600000,
        "it_software": 450000,
        "it_cloud": 350000,
        "it_support": 180000,
        "office_rent": 900000,
        "office_utilities": 250000,
        "office_supplies": 80000,
        "office_insurance": 120000,
        "travel_domestic": 90000,
        "travel_international": 200000,
        "travel_entertainment": 65000,
        "budget_total": 32120000,
        "budget_category": "Operations"
    }'::jsonb,
    NOW()
) ON CONFLICT (id) DO NOTHING;

-- Sample draft response from Company C
INSERT INTO form_responses (id, form_id, form_version_id, org_id, period_id, user_id, status, data, submitted_at)
VALUES (
    'b5c6d7e8-0001-0001-0001-000000000001',
    'a1b2c3d4-0001-0001-0001-000000000001',
    'b2c3d4e5-0001-0001-0001-000000000001',
    'COMPANY-C',
    '2026-Q1',
    'user@companyc.com',
    'DRAFT',
    '{
        "headcount": 200,
        "salaries_total": 60000000,
        "personnel_other": 3000000
    }'::jsonb,
    NULL
) ON CONFLICT (id) DO NOTHING;

-- ============================================================================
-- INDEXES for performance
-- ============================================================================
CREATE INDEX IF NOT EXISTS idx_sample_forms_org ON forms (org_id) WHERE org_id = 'HOLDING-001';
CREATE INDEX IF NOT EXISTS idx_sample_responses_period ON form_responses (period_id) WHERE period_id = '2026-Q1';
