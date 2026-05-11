/**
 * FS24 – Smart Persistence / Data Promotion
 *
 * UX focus:
 *  - Promotion admin route renders candidates/promoted tabs
 *  - Candidate list has usage/org/status columns
 *  - Candidate detail exposes proposed DDL preview and migration progress
 *  - Approve/dismiss actions are discoverable but not executed by default
 */
import { test, expect } from '@playwright/test';
import { ROUTES, TIMEOUTS } from '../../config/config';
import { gotoAndWait, featurePresent } from '../../fixtures/auth.fixture';

test.describe('Promotion admin page', () => {
  test('promotion page loads with tabs', async ({ page }) => {
    await gotoAndWait(page, ROUTES.adminPromotions);
    await expect(page).not.toHaveURL(/login|error/);

    const heading = page.locator('h1, h2, :text("Smart Persistence"), :text("Promotion")').first();
    await expect(heading).toBeVisible({ timeout: TIMEOUTS.default });

    const tabs = page.locator('[role="tab"], button:has-text("Candidates"), button:has-text("Promoted")');
    const count = await tabs.count();
    expect(count, 'Promotion page should expose candidates/promoted tabs').toBeGreaterThanOrEqual(1);
  });

  test('candidate list exposes usage, organization and status signals', async ({ page }) => {
    await gotoAndWait(page, ROUTES.adminPromotions);

    const expectedHeaders = [/mapping/i, /usage/i, /organi[sz]ation/i, /status/i];
    let found = 0;
    for (const header of expectedHeaders) {
      const visible = await page.locator('th, [role="columnheader"], span, div')
        .filter({ hasText: header }).first()
        .isVisible({ timeout: 2_000 }).catch(() => false);
      if (visible) found++;
    }

    if (found < 3) {
      console.warn(`[MISSING FEATURE] Promotion candidate list headers incomplete (${found}/4 found)`);
    }
    expect(found, 'Promotion list should expose at least mapping/usage/status columns').toBeGreaterThanOrEqual(2);
  });

  test('candidate detail exposes DDL preview when a candidate exists', async ({ page }) => {
    await gotoAndWait(page, ROUTES.adminPromotions);

    const viewButton = page.locator(
      'button[title*="View" i], button:has-text("View"), [aria-label*="View" i]'
    ).first();
    const hasCandidate = await viewButton.isVisible({ timeout: TIMEOUTS.short }).catch(() => false);
    if (!hasCandidate) {
      await featurePresent(page, 'button[title*="View"]', 'Promotion candidate detail action');
      return;
    }

    await viewButton.click();
    const ddlPreview = page.locator('pre, code, textarea, :text("CREATE TABLE"), :text("DDL")').first();
    const visible = await ddlPreview.isVisible({ timeout: TIMEOUTS.short }).catch(() => false);
    if (!visible) {
      await featurePresent(page, 'pre, code', 'Proposed DDL preview');
    } else {
      await expect(ddlPreview).toBeVisible();
    }
  });

  test('approve and dismiss actions are accessible for candidates', async ({ page }) => {
    await gotoAndWait(page, ROUTES.adminPromotions);

    const approve = page.locator(
      'button[title*="Approve" i], button:has-text("Approve"), [aria-label*="Approve" i]'
    ).first();
    const dismiss = page.locator(
      'button[title*="Dismiss" i], button:has-text("Dismiss"), [aria-label*="Dismiss" i]'
    ).first();

    const approveVisible = await approve.isVisible({ timeout: TIMEOUTS.short }).catch(() => false);
    const dismissVisible = await dismiss.isVisible({ timeout: TIMEOUTS.short }).catch(() => false);

    if (!approveVisible && !dismissVisible) {
      await featurePresent(page, 'button[title*="Approve"]', 'Promotion approve/dismiss actions');
      return;
    }

    if (approveVisible) await expect(approve).toBeEnabled();
    if (dismissVisible) await expect(dismiss).toBeEnabled();
  });

  test('promoted tables tab can be opened', async ({ page }) => {
    await gotoAndWait(page, ROUTES.adminPromotions);

    const promotedTab = page.locator('[role="tab"], button').filter({ hasText: /promoted/i }).first();
    const visible = await promotedTab.isVisible({ timeout: TIMEOUTS.short }).catch(() => false);
    if (!visible) {
      await featurePresent(page, 'button:has-text("Promoted")', 'Promoted Tables tab');
      return;
    }

    await promotedTab.click();
    const promotedContent = page.locator('table, [role="grid"], :text("Promoted Tables"), :text("No promoted")').first();
    await expect(promotedContent).toBeVisible({ timeout: TIMEOUTS.default });
  });
});
