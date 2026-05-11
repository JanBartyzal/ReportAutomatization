import { type Page, type Locator, expect } from '@playwright/test';
import { TIMEOUTS } from '../config/config';

export type ReportStatus = 'DRAFT' | 'SUBMITTED' | 'UNDER_REVIEW' | 'APPROVED' | 'REJECTED';

export class ReportsPage {
  readonly page: Page;
  readonly reportTable: Locator;
  readonly newReportBtn: Locator;
  readonly matrixView: Locator;
  readonly statusFilter: Locator;

  constructor(page: Page) {
    this.page         = page;
    this.reportTable  = page.locator('table, [role="grid"], [data-testid="report-table"]').first();
    this.newReportBtn = page.locator('button:has-text("New Report"), button:has-text("Create"), [data-testid="new-report-btn"]').first();
    this.matrixView   = page.locator('[data-testid="matrix-view"], .matrix-view').first();
    this.statusFilter = page.locator('select[name*="status" i], [data-testid="status-filter"]').first();
  }

  async waitForTable(): Promise<void> {
    await this.reportTable.waitFor({ state: 'visible', timeout: TIMEOUTS.default });
  }

  async getStatusBadges(): Promise<string[]> {
    const badges = this.page.locator('[data-testid*="status"], .badge, [class*="status-badge"]');
    return badges.allTextContents();
  }

  async clickFirstReportWithStatus(status: ReportStatus): Promise<void> {
    const row = this.page
      .locator('tr, [role="row"]')
      .filter({ hasText: status })
      .first();
    await row.click();
    await this.page.waitForLoadState('domcontentloaded');
  }

  async submitReport(): Promise<void> {
    const submitBtn = this.page.locator(
      'button:has-text("Submit"), button:has-text("Odeslat"), [data-testid="submit-report-btn"]'
    ).first();
    await submitBtn.click();
  }

  async approveReport(comment?: string): Promise<void> {
    const approveBtn = this.page.locator(
      'button:has-text("Approve"), button:has-text("Schválit"), [data-testid="approve-btn"]'
    ).first();
    await approveBtn.click();
    if (comment) {
      const commentField = this.page.locator('textarea, input[name*="comment" i]').first();
      await commentField.fill(comment);
    }
    await this.confirmAction();
  }

  async rejectReport(comment: string): Promise<void> {
    const rejectBtn = this.page.locator(
      'button:has-text("Reject"), button:has-text("Zamítnout"), [data-testid="reject-btn"]'
    ).first();
    await rejectBtn.click();

    // Comment is mandatory for rejection
    const commentField = this.page.locator(
      'textarea[name*="comment" i], textarea[placeholder*="reason" i], [data-testid="rejection-comment"]'
    ).first();
    await expect(commentField).toBeVisible({ timeout: TIMEOUTS.short });
    await commentField.fill(comment);

    await this.confirmAction();
  }

  async expectStatus(status: ReportStatus): Promise<void> {
    await expect(
      this.page.locator(`[data-testid*="status"], .badge`).filter({ hasText: status }).first()
    ).toBeVisible({ timeout: TIMEOUTS.default });
  }

  private async confirmAction(): Promise<void> {
    const confirmBtn = this.page.locator(
      'button:has-text("Confirm"), button:has-text("OK"), button:has-text("Potvrdit"), [data-testid="confirm-btn"]'
    ).first();
    if (await confirmBtn.isVisible({ timeout: 2_000 }).catch(() => false)) {
      await confirmBtn.click();
    }
  }
}
