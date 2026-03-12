import { test, expect } from '../fixtures';
import { ReportsPage } from '../pages/reports.page';

/** Shared mock report data used across lifecycle tests. */
const MOCK_REPORTS = [
    {
        id: 'rpt-draft-001',
        report_type: 'Quarterly Financial',
        org_id: 'org-001',
        period_id: 'period-q4-2025',
        status: 'DRAFT',
        created_at: '2026-02-15T10:00:00Z',
        updated_at: '2026-03-01T14:30:00Z',
    },
    {
        id: 'rpt-submitted-001',
        report_type: 'Annual Compliance',
        org_id: 'org-001',
        period_id: 'period-2025',
        status: 'SUBMITTED',
        created_at: '2026-01-10T09:00:00Z',
        updated_at: '2026-02-28T11:00:00Z',
    },
    {
        id: 'rpt-review-001',
        report_type: 'Monthly Operations',
        org_id: 'org-002',
        period_id: 'period-m02-2026',
        status: 'UNDER_REVIEW',
        created_at: '2026-03-01T08:00:00Z',
        updated_at: '2026-03-10T16:00:00Z',
    },
    {
        id: 'rpt-approved-001',
        report_type: 'Risk Assessment',
        org_id: 'org-001',
        period_id: 'period-q3-2025',
        status: 'APPROVED',
        created_at: '2025-10-01T10:00:00Z',
        updated_at: '2025-12-15T09:00:00Z',
    },
];

test.describe('Report Lifecycle', () => {
    let reportsPage: ReportsPage;

    /**
     * Intercept reports API requests and return mock data.
     * Accepts an optional override list to simulate status transitions.
     */
    async function mockReportsApi(
        page: import('@playwright/test').Page,
        reports = MOCK_REPORTS,
    ): Promise<void> {
        await page.route('**/api/v1/reports*', async (route) => {
            if (route.request().method() === 'GET') {
                await route.fulfill({
                    status: 200,
                    contentType: 'application/json',
                    body: JSON.stringify({ data: reports, total: reports.length }),
                });
            } else {
                await route.continue();
            }
        });

        // Mock organizations and periods (referenced by the reports page)
        await page.route('**/api/v1/organizations*', async (route) => {
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify([
                    { id: 'org-001', name: 'Alpha Corp' },
                    { id: 'org-002', name: 'Beta Inc' },
                ]),
            });
        });

        await page.route('**/api/v1/periods*', async (route) => {
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify({
                    data: [
                        { id: 'period-q4-2025', name: 'Q4 2025' },
                        { id: 'period-2025', name: 'FY 2025' },
                        { id: 'period-m02-2026', name: 'Feb 2026' },
                        { id: 'period-q3-2025', name: 'Q3 2025' },
                    ],
                }),
            });
        });
    }

    test.beforeEach(async ({ authenticatedPage }) => {
        reportsPage = new ReportsPage(authenticatedPage);
    });

    test('should display report list', async ({ authenticatedPage }) => {
        await mockReportsApi(authenticatedPage);
        await reportsPage.navigate();

        await expect(reportsPage.heading).toBeVisible();
        await reportsPage.waitForTableLoaded();

        const reportTypes = await reportsPage.getReportList();
        expect(reportTypes.length).toBeGreaterThanOrEqual(4);
        expect(reportTypes).toEqual(
            expect.arrayContaining(['Quarterly Financial', 'Annual Compliance', 'Monthly Operations', 'Risk Assessment']),
        );
    });

    test('should submit a report for review', async ({ authenticatedPage }) => {
        // Start with a draft report
        await mockReportsApi(authenticatedPage);

        // Mock the submit endpoint
        await authenticatedPage.route('**/api/v1/reports/rpt-draft-001/submit', async (route) => {
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify({
                    ...MOCK_REPORTS[0],
                    status: 'SUBMITTED',
                    updated_at: new Date().toISOString(),
                }),
            });
        });

        await reportsPage.navigate();
        await reportsPage.waitForTableLoaded();

        // The draft report should have a Submit button
        const draftRow = authenticatedPage.locator('tr', {
            has: authenticatedPage.getByText('Quarterly Financial'),
        });
        const submitButton = draftRow.getByRole('button', { name: 'Submit' });
        await expect(submitButton).toBeVisible();

        // Click submit
        await submitButton.click();
        await authenticatedPage.waitForLoadState('networkidle');
    });

    test('should approve a submitted report', async ({ authenticatedPage }) => {
        // Use a report in UNDER_REVIEW status (which shows Approve/Reject buttons)
        await mockReportsApi(authenticatedPage);

        // Mock the approve endpoint
        await authenticatedPage.route('**/api/v1/reports/rpt-review-001/approve', async (route) => {
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify({
                    ...MOCK_REPORTS[2],
                    status: 'APPROVED',
                    updated_at: new Date().toISOString(),
                }),
            });
        });

        await reportsPage.navigate();
        await reportsPage.waitForTableLoaded();

        // The UNDER_REVIEW report should have Approve and Reject buttons
        const reviewRow = authenticatedPage.locator('tr', {
            has: authenticatedPage.getByText('Monthly Operations'),
        });
        const approveButton = reviewRow.getByRole('button', { name: 'Approve' });
        await expect(approveButton).toBeVisible();

        // Click approve
        await approveButton.click();
        await authenticatedPage.waitForLoadState('networkidle');
    });

    test('should reject a submitted report', async ({ authenticatedPage }) => {
        await mockReportsApi(authenticatedPage);

        // Mock the bulk reject endpoint (the app uses bulk reject even for single reports)
        await authenticatedPage.route('**/api/v1/reports/bulk-reject', async (route) => {
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify({ success: true }),
            });
        });

        await reportsPage.navigate();
        await reportsPage.waitForTableLoaded();

        // The UNDER_REVIEW report should have a Reject button
        const reviewRow = authenticatedPage.locator('tr', {
            has: authenticatedPage.getByText('Monthly Operations'),
        });
        const rejectButton = reviewRow.getByRole('button', { name: 'Reject' });
        await expect(rejectButton).toBeVisible();

        // Click reject - opens the rejection dialog
        await rejectButton.click();

        // Fill the rejection comment in the dialog
        const commentInput = authenticatedPage.getByRole('textbox');
        await expect(commentInput).toBeVisible({ timeout: 5_000 });
        await commentInput.fill('Data quality issues found in section 3.');

        // Confirm the rejection
        const confirmButton = authenticatedPage.getByRole('button', { name: /confirm|reject/i }).last();
        await confirmButton.click();
        await authenticatedPage.waitForLoadState('networkidle');
    });
});
