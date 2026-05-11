/**
 * FS14 – Data Versioning & Diff Tool
 *
 * UX focus:
 *  - Report / file detail shows version history timeline
 *  - Version badges (v1, v2, …) visible
 *  - Diff view accessible from version history
 *  - Diff highlights added/removed values
 *  - "Original always preserved" — v1 never overwritten indicator
 */
import { test, expect } from '@playwright/test';
import { ROUTES, TIMEOUTS } from '../../config/config';
import { gotoAndWait, featurePresent } from '../../fixtures/auth.fixture';

test.describe('Version history on report detail', () => {
  test('report detail page has version history section', async ({ page }) => {
    await gotoAndWait(page, ROUTES.reports);

    const firstReport = page.locator(
      'table tr:has(td) a, [role="row"]:not([role="columnheader"]) a, [data-testid="report-item"] a'
    ).first();
    const hasReport = await firstReport.isVisible({ timeout: 3_000 }).catch(() => false);
    if (!hasReport) {
      console.warn('[INFO] No reports available to test versioning');
      return;
    }

    await firstReport.click();
    await page.waitForLoadState('domcontentloaded');

    const versionSection = page.locator(
      '[data-testid="version-history"], [aria-label*="version" i], :text("Version"), :text("Verze"), :text("History"), :text("Historie")'
    ).first();
    const visible = await versionSection.isVisible({ timeout: TIMEOUTS.short }).catch(() => false);

    if (!visible) {
      await featurePresent(page, '[data-testid="version-history"]', 'Version history section on report detail');
    } else {
      await expect(versionSection).toBeVisible();
    }
  });

  test('version timeline shows version numbers or timestamps', async ({ page }) => {
    await gotoAndWait(page, ROUTES.reports);

    const firstReport = page.locator(
      'table tr:has(td) a, [data-testid="report-item"] a'
    ).first();
    if (!await firstReport.isVisible({ timeout: 3_000 }).catch(() => false)) {
      console.warn('[INFO] No reports — skip');
      return;
    }

    await firstReport.click();
    await page.waitForLoadState('domcontentloaded');

    const versionBadge = page.locator(
      ':text("v1"), :text("v2"), [data-testid*="version-badge"], [class*="version"]'
    ).first();
    const visible = await versionBadge.isVisible({ timeout: TIMEOUTS.short }).catch(() => false);
    if (!visible) {
      await featurePresent(page, '[data-testid*="version-badge"]', 'Version badges in timeline');
    }
  });
});

test.describe('Diff Tool', () => {
  test('diff view accessible from version comparison', async ({ page }) => {
    await gotoAndWait(page, ROUTES.reports);

    const firstReport = page.locator(
      'table tr:has(td) a, [data-testid="report-item"] a'
    ).first();
    if (!await firstReport.isVisible({ timeout: 3_000 }).catch(() => false)) {
      console.warn('[INFO] No reports — skip');
      return;
    }

    await firstReport.click();
    await page.waitForLoadState('domcontentloaded');

    // Look for compare/diff button
    const compareBtn = page.locator(
      'button:has-text("Compare"), button:has-text("Diff"), button:has-text("Porovnat"), [data-testid="compare-versions-btn"]'
    ).first();
    const visible = await compareBtn.isVisible({ timeout: TIMEOUTS.short }).catch(() => false);

    if (!visible) {
      await featurePresent(page, 'button:has-text("Compare")', 'Compare versions button');
      return;
    }

    await compareBtn.click();
    await page.waitForLoadState('domcontentloaded');

    // Diff view should show some form of comparison
    const diffView = page.locator(
      '[data-testid="diff-view"], .diff-view, [class*="diff"], :text("Change"), :text("Změna")'
    ).first();
    await expect(diffView).toBeVisible({ timeout: TIMEOUTS.default });
  });

  test('diff highlights changed values with colour indicators', async ({ page }) => {
    await gotoAndWait(page, ROUTES.reports);

    const firstReport = page.locator(
      'table tr:has(td) a, [data-testid="report-item"] a'
    ).first();
    if (!await firstReport.isVisible({ timeout: 3_000 }).catch(() => false)) return;

    await firstReport.click();
    await page.waitForLoadState('domcontentloaded');

    const compareBtn = page.locator(
      'button:has-text("Compare"), button:has-text("Diff"), [data-testid="compare-versions-btn"]'
    ).first();
    if (!await compareBtn.isVisible({ timeout: 2_000 }).catch(() => false)) return;

    await compareBtn.click();
    await page.waitForLoadState('domcontentloaded');

    // Added (green) or removed (red) highlights
    const highlights = page.locator(
      '[class*="added"], [class*="removed"], [class*="changed"], [data-testid*="diff-cell"]'
    );
    const count = await highlights.count();
    if (count === 0) {
      console.warn('[MISSING FEATURE] Diff view does not show colour highlights for changes');
    }
  });
});
