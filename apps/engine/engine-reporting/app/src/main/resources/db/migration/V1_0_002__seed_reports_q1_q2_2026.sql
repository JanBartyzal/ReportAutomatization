-- V2__seed_reports_q1_q2_2026.sql
-- Seed data for testing - reports for Q1/Q2 2026 periods

-- Create reports for Q1 2026 period (a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11)
-- Using different statuses for testing: DRAFT, SUBMITTED, UNDER_REVIEW, APPROVED, REJECTED

-- Report for org-001: APPROVED
INSERT INTO reports (id, org_id, period_id, report_type, status, locked, submitted_by, submitted_at, reviewed_by, reviewed_at, approved_by, approved_at, created_by, created_at, updated_at)
VALUES 
    ('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'org-001', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'QUARTERLY_REPORT', 'APPROVED', TRUE, 'user-001', '2026-04-10 10:00:00+00', 'admin-001', '2026-04-12 14:00:00+00', 'admin-001', '2026-04-12 15:00:00+00', 'user-001', '2026-04-01 09:00:00+00', '2026-04-12 15:00:00+00')
ON CONFLICT (org_id, period_id, report_type) DO NOTHING;

-- Report for org-002: SUBMITTED
INSERT INTO reports (id, org_id, period_id, report_type, status, locked, submitted_by, submitted_at, created_by, created_at, updated_at)
VALUES 
    ('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', 'org-002', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'QUARTERLY_REPORT', 'SUBMITTED', FALSE, 'user-002', '2026-04-14 11:00:00+00', 'user-002', '2026-04-02 09:00:00+00', '2026-04-14 11:00:00+00')
ON CONFLICT (org_id, period_id, report_type) DO NOTHING;

-- Report for org-003: UNDER_REVIEW
INSERT INTO reports (id, org_id, period_id, report_type, status, locked, submitted_by, submitted_at, reviewed_by, reviewed_at, created_by, created_at, updated_at)
VALUES 
    ('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13', 'org-003', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'QUARTERLY_REPORT', 'UNDER_REVIEW', FALSE, 'user-003', '2026-04-13 16:00:00+00', 'admin-001', '2026-04-14 09:00:00+00', 'user-003', '2026-04-03 09:00:00+00', '2026-04-14 09:00:00+00')
ON CONFLICT (org_id, period_id, report_type) DO NOTHING;

-- Report for org-004: REJECTED
INSERT INTO reports (id, org_id, period_id, report_type, status, locked, submitted_by, submitted_at, reviewed_by, reviewed_at, created_by, created_at, updated_at)
VALUES 
    ('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14', 'org-004', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'QUARTERLY_REPORT', 'REJECTED', FALSE, 'user-004', '2026-04-11 14:00:00+00', 'admin-001', '2026-04-12 10:00:00+00', 'user-004', '2026-04-04 09:00:00+00', '2026-04-12 10:00:00+00')
ON CONFLICT (org_id, period_id, report_type) DO NOTHING;

-- Report for org-005: DRAFT
INSERT INTO reports (id, org_id, period_id, report_type, status, locked, created_by, created_at, updated_at)
VALUES 
    ('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a15', 'org-005', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'QUARTERLY_REPORT', 'DRAFT', FALSE, 'user-005', '2026-04-05 09:00:00+00', '2026-04-05 09:00:00+00')
ON CONFLICT (org_id, period_id, report_type) DO NOTHING;

-- Create reports for Q2 2026 period (b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12)
-- All in DRAFT status for testing

INSERT INTO reports (id, org_id, period_id, report_type, status, locked, created_by, created_at, updated_at)
VALUES 
    ('d0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'org-001', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', 'QUARTERLY_REPORT', 'DRAFT', FALSE, 'user-001', '2026-04-15 09:00:00+00', '2026-04-15 09:00:00+00'),
    ('d0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', 'org-002', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', 'QUARTERLY_REPORT', 'DRAFT', FALSE, 'user-002', '2026-04-15 09:00:00+00', '2026-04-15 09:00:00+00'),
    ('d0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13', 'org-003', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', 'QUARTERLY_REPORT', 'DRAFT', FALSE, 'user-003', '2026-04-15 09:00:00+00', '2026-04-15 09:00:00+00'),
    ('d0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14', 'org-004', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', 'QUARTERLY_REPORT', 'DRAFT', FALSE, 'user-004', '2026-04-15 09:00:00+00', '2026-04-15 09:00:00+00'),
    ('d0eebc99-9c0b-4ef8-bb6d-6bb9bd380a15', 'org-005', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', 'QUARTERLY_REPORT', 'DRAFT', FALSE, 'user-005', '2026-04-15 09:00:00+00', '2026-04-15 09:00:00+00')
ON CONFLICT (org_id, period_id, report_type) DO NOTHING;

-- Add status history for Q1 reports

-- History for org-001 (APPROVED)
INSERT INTO report_status_history (id, report_id, from_status, to_status, user_id, comment, created_at)
VALUES 
    ('e0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', NULL, 'DRAFT', 'user-001', 'Report created', '2026-04-01 09:00:00+00'),
    ('e0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'DRAFT', 'SUBMITTED', 'user-001', NULL, '2026-04-10 10:00:00+00'),
    ('e0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13', 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'SUBMITTED', 'UNDER_REVIEW', 'admin-001', 'Started review', '2026-04-12 14:00:00+00'),
    ('e0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14', 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'UNDER_REVIEW', 'APPROVED', 'admin-001', 'All checks passed', '2026-04-12 15:00:00+00')
ON CONFLICT DO NOTHING;

-- History for org-004 (REJECTED)
INSERT INTO report_status_history (id, report_id, from_status, to_status, user_id, comment, created_at)
VALUES 
    ('e0eebc99-9c0b-4ef8-bb6d-6bb9bd380a21', 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14', NULL, 'DRAFT', 'user-004', 'Report created', '2026-04-04 09:00:00+00'),
    ('e0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22', 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14', 'DRAFT', 'SUBMITTED', 'user-004', NULL, '2026-04-11 14:00:00+00'),
    ('e0eebc99-9c0b-4ef8-bb6d-6bb9bd380a23', 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14', 'SUBMITTED', 'REJECTED', 'admin-001', 'Missing required financial data in section B', '2026-04-12 10:00:00+00')
ON CONFLICT DO NOTHING;

-- Add submission checklists for some reports
INSERT INTO submission_checklists (id, report_id, checklist_json, completed_pct, updated_at)
VALUES 
    ('f0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', '[{"item": "Financial Statement", "completed": true}, {"item": "Balance Sheet", "completed": true}, {"item": "Cash Flow", "completed": true}]', 100, '2026-04-12 15:00:00+00'),
    ('f0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', '[{"item": "Financial Statement", "completed": true}, {"item": "Balance Sheet", "completed": true}, {"item": "Cash Flow", "completed": true}]', 100, '2026-04-14 11:00:00+00')
ON CONFLICT (report_id) DO NOTHING;
