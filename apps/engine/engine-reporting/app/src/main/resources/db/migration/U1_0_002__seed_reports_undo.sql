-- U1_0_002__seed_reports_undo.sql
-- Reverses V1_0_002__seed_reports_q1_q2_2026.sql
-- WARNING: This will delete seed data

-- Delete submission checklists first (child table)
DELETE FROM submission_checklists 
WHERE report_id IN (
    'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12',
    'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13',
    'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14',
    'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a15',
    'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12',
    'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13',
    'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14',
    'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a15'
);

-- Delete status history first (child table)
DELETE FROM report_status_history 
WHERE report_id IN (
    'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14'
);

-- Delete the seeded reports
DELETE FROM reports 
WHERE id IN (
    'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12',
    'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13',
    'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14',
    'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a15',
    'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12',
    'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13',
    'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14',
    'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a15'
);
