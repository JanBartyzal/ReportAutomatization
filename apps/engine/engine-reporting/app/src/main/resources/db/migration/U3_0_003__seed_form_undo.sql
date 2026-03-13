-- U3_0_003__seed_form_undo.sql
-- Reverses V3_0_003__seed_sample_opex_forms.sql
-- WARNING: This will delete seed data

-- Delete sample form responses first (child table)
DELETE FROM form_responses 
WHERE id IN (
    'f3a4b5c6-0001-0001-0001-000000000001',
    'a4b5c6d7-0001-0001-0001-000000000001',
    'b5c6d7e8-0001-0001-0001-000000000001'
);

-- Delete sample form fields (child table)
DELETE FROM form_fields 
WHERE form_version_id = 'b2c3d4e5-0001-0001-0001-000000000001';

-- Delete sample form version
DELETE FROM form_versions 
WHERE id = 'b2c3d4e5-0001-0001-0001-000000000001';

-- Delete sample OPEX form
DELETE FROM forms 
WHERE id = 'a1b2c3d4-0001-0001-0001-000000000001';

-- Drop sample indexes
DROP INDEX IF EXISTS idx_sample_forms_org;
DROP INDEX IF EXISTS idx_sample_responses_period;
