-- U2_0_002__seed_periods_undo.sql
-- Reverses V2_0_002__seed_periods_q1_q2_2026.sql
-- WARNING: This will delete seed data

-- Delete period organization assignments first (child table)
DELETE FROM period_org_assignments 
WHERE period_id IN (
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12'
);

-- Delete the seeded periods
DELETE FROM periods 
WHERE id IN (
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12'
);
