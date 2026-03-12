import { type Page, type Locator, expect } from '@playwright/test';

/**
 * Page Object for the Reports page.
 *
 * Covers listing reports, submitting for review, approving, and rejecting.
 */
export class ReportsPage {
    readonly page: Page;
    readonly heading: Locator;
    readonly reportTable: Locator;
    readonly loadingSpinner: Locator;

    constructor(page: Page) {
        this.page = page;
        this.heading = page.getByRole('heading', { name: 'Reports' });
        this.reportTable = page.locator('table');
        this.loadingSpinner = page.getByText('Loading reports...');
    }

    /** Navigate to the reports list page. */
    async navigate(): Promise<void> {
        await this.page.goto('/reports');
        await this.page.waitForLoadState('networkidle');
    }

    /** Wait until the report table has finished loading. */
    async waitForTableLoaded(): Promise<void> {
        // Wait for the spinner to disappear
        await expect(this.loadingSpinner).not.toBeVisible({ timeout: 15_000 });
        await expect(this.reportTable).toBeVisible();
    }

    /** Return an array of report type strings from the table rows. */
    async getReportList(): Promise<string[]> {
        await this.waitForTableLoaded();
        const rows = this.reportTable.locator('tbody tr');
        const count = await rows.count();
        const types: string[] = [];
        for (let i = 0; i < count; i++) {
            // Report type is in the second column (first is selection checkbox)
            const typeCell = rows.nth(i).locator('td').nth(1);
            const text = await typeCell.textContent();
            if (text) types.push(text.trim());
        }
        return types;
    }

    /**
     * Click the Submit button for a report row identified by its report ID.
     * The Submit button only appears for reports in DRAFT status.
     */
    async submitReport(reportId: string): Promise<void> {
        const row = this.getReportRow(reportId);
        const submitButton = row.getByRole('button', { name: 'Submit' });
        await expect(submitButton).toBeVisible();
        await submitButton.click();
        // Wait for the mutation to complete
        await this.page.waitForLoadState('networkidle');
    }

    /**
     * Click the Approve button for a report row.
     * The Approve button only appears for reports in UNDER_REVIEW status.
     */
    async approveReport(reportId: string): Promise<void> {
        const row = this.getReportRow(reportId);
        const approveButton = row.getByRole('button', { name: 'Approve' });
        await expect(approveButton).toBeVisible();
        await approveButton.click();
        await this.page.waitForLoadState('networkidle');
    }

    /**
     * Click the Reject button for a report row and provide a rejection comment.
     * The Reject button only appears for reports in UNDER_REVIEW status.
     */
    async rejectReport(reportId: string, comment = 'Rejected by E2E test'): Promise<void> {
        const row = this.getReportRow(reportId);
        const rejectButton = row.getByRole('button', { name: 'Reject' });
        await expect(rejectButton).toBeVisible();
        await rejectButton.click();

        // Fill rejection comment in the dialog
        const commentInput = this.page.getByRole('textbox');
        await commentInput.fill(comment);

        // Confirm rejection
        const confirmButton = this.page.getByRole('button', { name: /confirm|reject/i }).last();
        await confirmButton.click();
        await this.page.waitForLoadState('networkidle');
    }

    /** Get the status badge text for a given report ID. */
    async getReportStatus(reportId: string): Promise<string> {
        const row = this.getReportRow(reportId);
        // Status is in the 5th column (index 4)
        const statusCell = row.locator('td').nth(4);
        const badge = statusCell.locator('[class*="badge"], [class*="Badge"]');
        const text = await badge.textContent();
        return (text ?? '').trim();
    }

    /** Open the detail page for a report by clicking the view (eye) button. */
    async openReportDetail(reportId: string): Promise<void> {
        const row = this.getReportRow(reportId);
        // The eye icon button is typically the first action button
        const viewButton = row.locator('button').first();
        await viewButton.click();
        await this.page.waitForURL(`**/reports/${reportId}`);
        await this.page.waitForLoadState('networkidle');
    }

    /** Locate a table row containing the given report ID text. */
    private getReportRow(reportId: string): Locator {
        return this.reportTable.locator('tbody tr', {
            has: this.page.getByText(reportId),
        });
    }
}
