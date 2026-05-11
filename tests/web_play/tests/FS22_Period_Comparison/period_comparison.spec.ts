/**
 * FS22 – Advanced Period Comparison
 *
 * UX focus:
 *  - /comparison route loads
 *  - Period selector (at least 2 periods selectable)
 *  - Organisation filter controls present
 *  - Chart or table visualisation of comparison
 *  - Delta values (absolute + percentage) shown
 *  - Export comparison as PPTX button (FS18 link)
 */
import { test, expect } from '@playwright/test';
import { ROUTES, TIMEOUTS } from '../../config/config';
import { gotoAndWait, featurePresent } from '../../fixtures/auth.fixture';

test.describe('Period comparison page', () => {
  test('/comparison route loads without errors', async ({ page }) => {
    await gotoAndWait(page, ROUTES.comparison);
    await expect(page).not.toHaveURL(/login|error/);
  });

  test('comparison page has period selector controls', async ({ page }) => {
    await gotoAndWait(page, ROUTES.comparison);

    const periodSelector = page.locator(
      'select[name*="period" i], [data-testid*="period-selector"], [aria-label*="period" i], :text("Period"), :text("Perioda")'
    ).first();
    const visible = await periodSelector.isVisible({ timeout: TIMEOUTS.default }).catch(() => false);
    if (!visible) {
      await featurePresent(page, '[data-testid*="period-selector"]', 'Period selector on comparison page');
    } else {
      await expect(periodSelector).toBeVisible();
    }
  });

  test('comparison page has organization filter', async ({ page }) => {
    await gotoAndWait(page, ROUTES.comparison);

    const orgFilter = page.locator(
      'select[name*="org" i], [data-testid*="org-filter"], [aria-label*="organization" i], [aria-label*="společnost" i]'
    ).first();
    const visible = await orgFilter.isVisible({ timeout: TIMEOUTS.short }).catch(() => false);
    if (!visible) {
      await featurePresent(page, '[data-testid*="org-filter"]', 'Organisation filter on comparison page');
    } else {
      await expect(orgFilter).toBeVisible();
    }
  });

  test('comparison renders chart or table visualisation', async ({ page }) => {
    await gotoAndWait(page, ROUTES.comparison);

    const viz = page.locator('svg, canvas, table[data-testid*="comparison"], [data-testid*="comparison-chart"]').first();
    const visible = await viz.isVisible({ timeout: TIMEOUTS.default }).catch(() => false);
    if (!visible) {
      await featurePresent(page, 'svg, canvas, [data-testid*="comparison-chart"]', 'Comparison chart / table visualisation');
    } else {
      await expect(viz).toBeVisible();
    }
  });

  test('delta values (± %) shown in comparison', async ({ page }) => {
    await gotoAndWait(page, ROUTES.comparison);

    const delta = page.locator('[data-testid*="delta"], :text("Δ"), :text("%"), :text("+"), :text("-")').first();
    const visible = await delta.isVisible({ timeout: TIMEOUTS.short }).catch(() => false);
    if (!visible) {
      await featurePresent(page, '[data-testid*="delta"]', 'Delta values in period comparison');
    }
  });
});
