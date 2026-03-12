-- V2__seed_periods_q1_q2_2026.sql
-- Seed data for testing - Q1/2026 and Q2/2026 periods

-- Q1 2026 Period (QUARTERLY)
INSERT INTO periods (id, name, period_type, period_code, start_date, end_date, submission_deadline, review_deadline, status, holding_id, created_by, created_at, updated_at)
VALUES 
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'Q1 2026', 'QUARTERLY', 'Q1-2026', '2026-01-01', '2026-03-31', '2026-04-15 23:59:59+00', '2026-04-30 23:59:59+00', 'COLLECTING', 'holding-001', 'system', NOW(), NOW()),
    ('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', 'Q2 2026', 'QUARTERLY', 'Q2-2026', '2026-04-01', '2026-06-30', '2026-07-15 23:59:59+00', '2026-07-30 23:59:59+00', 'OPEN', 'holding-001', 'system', NOW(), NOW())
ON CONFLICT (period_code) DO NOTHING;

-- Period Organization Assignments - assign test organizations to periods
-- Assuming we have orgs: org-001 to org-005
INSERT INTO period_org_assignments (id, period_id, org_id, assigned_at)
SELECT 
    gen_random_uuid(),
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    'org-' || LPAD(i::text, 3, '0'),
    NOW()
FROM generate_series(1, 5) i
ON CONFLICT DO NOTHING;

INSERT INTO period_org_assignments (id, period_id, org_id, assigned_at)
SELECT 
    gen_random_uuid(),
    'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12',
    'org-' || LPAD(i::text, 3, '0'),
    NOW()
FROM generate_series(1, 5) i
ON CONFLICT DO NOTHING;
