-- U3_0_004__seed_local_form_undo.sql
-- Reverses V3_0_004__seed_local_forms.sql
-- WARNING: This will delete seed data

-- Delete local form fields (child tables - order matters)
DELETE FROM form_fields 
WHERE form_version_id IN (
    'b2c3d4e5-0003-0001-0001-000000000001',
    'b2c3d4e5-0003-0002-0001-000000000001',
    'b2c3d4e5-0003-0003-0001-000000000001'
);

-- Delete local form versions
DELETE FROM form_versions 
WHERE id IN (
    'b2c3d4e5-0003-0001-0001-000000000001',
    'b2c3d4e5-0003-0002-0001-000000000001',
    'b2c3d4e5-0003-0003-0001-000000000001'
);

-- Delete local scope forms
DELETE FROM forms 
WHERE id IN (
    'a1b2c3d4-0003-0001-0001-000000000001',
    'a1b2c3d4-0003-0002-0001-000000000001',
    'a1b2c3d4-0003-0003-0001-000000000001'
);

-- Drop local scope indexes
DROP INDEX IF EXISTS idx_local_forms_owner_org;
DROP INDEX IF EXISTS idx_shared_forms_holding;
