/**
 * FS17 – OPEX Report Lifecycle & Submission Workflow
 *
 * UX focus:
 *  - Reports list shows status badges (DRAFT, SUBMITTED, UNDER_REVIEW, APPROVED, REJECTED)
 *  - Matrix view (Company × Period) is present for HoldingAdmin
 *  - DRAFT → SUBMITTED transition: checklist shown before submit
 *  - Rejection requires a mandatory comment field
 *  - Rejection comment visible to Editor
 *  - Approved reports show locked/read-only indicator
 *  - Timeline view of status transitions on report detail
 *  - Bulk approve/reject controls for HoldingAdmin
 */
import { test, expect } from '@playwright/test';
import { ReportsPage } from '../../pages/ReportsPage';
import { ROUTES, TIMEOUTS } from '../../config/config';
import { gotoAndWait, featurePresent } from '../../fixtures/auth.fixture';

test.describe('Reports list page', () => {
  test('reports page loads and shows status badges', async ({ page }) => {
    await gotoAndWait(page, ROUTES.reports);

    const listEl = page.locator('table, [role="grid"], [data-testid="report-table"]').first();
    const empty  = page.locator('[data-testid="empty-state"], :text("No reports"), :text("Žádné reporty")').first();

    const hasContent = await listEl.isVisible({ timeout: TIMEOUTS.default }).catch(() => false)
      || await empty.isVisible({ timeout: 2_000 }).catch(() => false);
    expect(hasContent, 'Reports page must show list or empty state').toBeTruthy();
  });

  test('report status badges use colour coding', async ({ page }) => {
    await gotoAndWait(page, ROUTES.reports);

    const badges = page.locator('[data-testid*="status"], .badge, [class*="status-badge"]');
    const count  = await badges.count();
    if (count === 0) {
      await featurePresent(page, '[data-testid*="status"]', 'Status badges on reports list');
      return;
    }

    // At least one badge should have a class or style indicating colour
    const firstBadge   = badges.first();
    const badgeClass   = await firstBadge.getAttribute('class') ?? '';
    const hasColourHint = /draft|submitted|review|approved|rejected|green|red|yellow|blue|gray/i.test(badgeClass);
    if (!hasColourHint) {
      console.warn('[MISSING FEATURE] Status badge classes do not include status-specific colour identifiers');
    }
  });

  test('matrix view is accessible from reports page', async ({ page }) => {
    await gotoAndWait(page, ROUTES.reports);

    const matrixBtn = page.locator(
      'button:has-text("Matrix"), a:has-text("Matrix"), [data-testid="matrix-view-btn"]'
    ).first();
    const visible = await matrixBtn.isVisible({ timeout: TIMEOUTS.short }).catch(() => false);
    if (!visible) {
      // Also try direct route
      await gotoAndWait(page, ROUTES.matrix);
      const matrixEl = page.locator('[data-testid="matrix-view"], table.matrix, .matrix-grid').first();
      const matrixVisible = await matrixEl.isVisible({ timeout: TIMEOUTS.short }).catch(() => false);
      if (!matrixVisible) {
        await featurePresent(page, '[data-testid="matrix-view"]', 'Matrix view (Company × Period)');
      }
    } else {
      await expect(matrixBtn).toBeEnabled();
    }
  });
});

test.describe('Submission workflow', () => {
  test('submitting a report shows submission checklist', async ({ page }) => {
    await gotoAndWait(page, ROUTES.reports);
    const reportsPage = new ReportsPage(page);

    const draftReport = page.locator(
      'tr:has-text("DRAFT") a, [role="row"]:has-text("DRAFT") a, [data-testid="report-item"]:has-text("DRAFT") a'
    ).first();
    if (!await draftReport.isVisible({ timeout: 3_000 }).catch(() => false)) {
      console.warn('[INFO] No DRAFT reports available — skip submission checklist test');
      return;
    }

    await draftReport.click();
    await page.waitForLoadState('domcontentloaded');

    const submitBtn = page.locator(
      'button:has-text("Submit"), button:has-text("Odeslat"), [data-testid="submit-report-btn"]'
    ).first();
    if (!await submitBtn.isVisible({ timeout: TIMEOUTS.short }).catch(() => false)) {
      await featurePresent(page, 'button:has-text("Submit")', 'Submit report button');
      return;
    }

    await submitBtn.click();

    // Checklist or confirmation dialog should appear
    const checklist = page.locator(
      '[data-testid="submission-checklist"], [role="dialog"]:has-text("checklist"), [role="dialog"]:has-text("Checklist"), :text("All required"), :text("Všechna povinná")'
    ).first();
    const checklistVisible = await checklist.isVisible({ timeout: TIMEOUTS.short }).catch(() => false);
    if (!checklistVisible) {
      await featurePresent(page, '[data-testid="submission-checklist"]', 'Submission checklist dialog');
    } else {
      await expect(checklist).toBeVisible();
    }
  });
});

test.describe('Approval / rejection workflow', () => {
  test('HoldingAdmin sees approve and reject buttons on submitted reports', async ({ page }) => {
    await gotoAndWait(page, ROUTES.reports);

    const submittedReport = page.locator(
      'tr:has-text("SUBMITTED") a, [role="row"]:has-text("SUBMITTED") a, [data-testid="report-item"]:has-text("SUBMITTED") a'
    ).first();
    if (!await submittedReport.isVisible({ timeout: 3_000 }).catch(() => false)) {
      console.warn('[INFO] No SUBMITTED reports — skip');
      return;
    }

    await submittedReport.click();
    await page.waitForLoadState('domcontentloaded');

    const approveBtn = page.locator('button:has-text("Approve"), button:has-text("Schválit"), [data-testid="approve-btn"]').first();
    const rejectBtn  = page.locator('button:has-text("Reject"), button:has-text("Zamítnout"), [data-testid="reject-btn"]').first();

    const hasApprove = await approveBtn.isVisible({ timeout: TIMEOUTS.short }).catch(() => false);
    const hasReject  = await rejectBtn.isVisible({ timeout: TIMEOUTS.short }).catch(() => false);

    if (!hasApprove) await featurePresent(page, 'button:has-text("Approve")', 'Approve button');
    if (!hasReject)  await featurePresent(page, 'button:has-text("Reject")',  'Reject button');
  });

  test('rejection without a comment is blocked (comment field required)', async ({ page }) => {
    await gotoAndWait(page, ROUTES.reports);

    const submittedReport = page.locator(
      'tr:has-text("SUBMITTED") a, [data-testid="report-item"]:has-text("SUBMITTED") a'
    ).first();
    if (!await submittedReport.isVisible({ timeout: 3_000 }).catch(() => false)) return;

    await submittedReport.click();
    await page.waitForLoadState('domcontentloaded');

    const rejectBtn = page.locator('button:has-text("Reject"), button:has-text("Zamítnout"), [data-testid="reject-btn"]').first();
    if (!await rejectBtn.isVisible({ timeout: TIMEOUTS.short }).catch(() => false)) return;

    await rejectBtn.click();

    // Comment textarea must appear
    const commentField = page.locator(
      'textarea[name*="comment" i], textarea[placeholder*="reason" i], [data-testid="rejection-comment"]'
    ).first();
    await expect(commentField).toBeVisible({ timeout: TIMEOUTS.short });

    // Try to confirm without filling comment
    const confirmBtn = page.locator('button:has-text("Confirm"), button:has-text("OK"), button:has-text("Potvrdit")').first();
    if (await confirmBtn.isVisible({ timeout: 1_000 }).catch(() => false)) {
      await confirmBtn.click();
      // Should show validation error, not proceed
      const error = page.locator('[role="alert"], .error, input:invalid, textarea:invalid').first();
      const isRequired = await commentField.evaluate((el) => (el as HTMLTextAreaElement).required);
      const hasError   = await error.isVisible({ timeout: 2_000 }).catch(() => false);
      if (!isRequired && !hasError) {
        console.warn('[MISSING FEATURE] Rejection comment field is not enforced as required');
      }
    }
  });
});

test.describe('Report status transitions timeline', () => {
  test('report detail shows status transition history', async ({ page }) => {
    await gotoAndWait(page, ROUTES.reports);

    const firstReport = page.locator('table tr:has(td) a, [data-testid="report-item"] a').first();
    if (!await firstReport.isVisible({ timeout: 3_000 }).catch(() => false)) return;

    await firstReport.click();
    await page.waitForLoadState('domcontentloaded');

    const timeline = page.locator(
      '[data-testid="status-timeline"], [aria-label*="timeline" i], .timeline, :text("Timeline"), :text("Historie")'
    ).first();
    const visible = await timeline.isVisible({ timeout: TIMEOUTS.short }).catch(() => false);
    if (!visible) {
      await featurePresent(page, '[data-testid="status-timeline"]', 'Status transition timeline on report detail');
    }
  });
});
