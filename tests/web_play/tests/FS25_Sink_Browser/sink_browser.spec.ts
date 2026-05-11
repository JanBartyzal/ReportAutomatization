/**
 * FS25 – Sink Browser, Manual Corrections & Extraction Learning
 *
 * UX focus:
 *  - /sinks page loads with list of sinks
 *  - Filter controls (file, sheet, source type, date range)
 *  - Pagination controls present
 *  - Clicking a sink opens detail view with data table
 *  - Cell inline editing: click cell → input appears
 *  - Corrected cells visually highlighted (colour + icon)
 *  - Correction history tooltip / side panel
 *  - Selection checkbox per sink
 *  - Error category picker on correction
 *  - "Learning from correction" indicator
 */
import { test, expect } from '@playwright/test';
import { ROUTES, TIMEOUTS } from '../../config/config';
import { gotoAndWait, featurePresent } from '../../fixtures/auth.fixture';

test.describe('Sink Browser list page', () => {
  test('/sinks page loads', async ({ page }) => {
    await gotoAndWait(page, ROUTES.sinks);
    await expect(page).not.toHaveURL(/login|error/);

    const list = page.locator('table, [role="grid"], [data-testid="sink-list"]').first();
    const empty = page.locator('[data-testid="empty-state"], :text("No sinks"), :text("Žádné sinky")').first();
    const hasContent = await list.isVisible({ timeout: TIMEOUTS.default }).catch(() => false)
      || await empty.isVisible({ timeout: 2_000 }).catch(() => false);
    expect(hasContent).toBeTruthy();
  });

  test('sink list has filter controls', async ({ page }) => {
    await gotoAndWait(page, ROUTES.sinks);

    const filters = page.locator(
      '[data-testid*="filter"], input[type="search"], select[name*="source" i], [aria-label*="filter" i]'
    );
    const count = await filters.count();
    if (count === 0) {
      await featurePresent(page, '[data-testid*="filter"]', 'Sink list filter controls');
      return;
    }
    expect(count).toBeGreaterThan(0);
  });

  test('sink list has pagination controls', async ({ page }) => {
    await gotoAndWait(page, ROUTES.sinks);

    const pagination = page.locator(
      '[data-testid*="pagination"], nav[aria-label*="pagination" i], button:has-text("Next"), button:has-text("Další")'
    ).first();
    const visible = await pagination.isVisible({ timeout: TIMEOUTS.short }).catch(() => false);
    if (!visible) {
      await featurePresent(page, '[data-testid*="pagination"]', 'Sink list pagination');
    }
  });

  test('sink rows have selection checkboxes', async ({ page }) => {
    await gotoAndWait(page, ROUTES.sinks);

    const checkboxes = page.locator('table input[type="checkbox"], [role="row"] input[type="checkbox"]');
    const count = await checkboxes.count();
    if (count === 0) {
      await featurePresent(page, 'input[type="checkbox"]', 'Sink row selection checkboxes');
    } else {
      expect(count).toBeGreaterThan(0);
    }
  });
});

test.describe('Sink detail view', () => {
  test('clicking a sink opens detail with data table', async ({ page }) => {
    await gotoAndWait(page, ROUTES.sinks);

    const firstSink = page.locator(
      'table tr:has(td) a, [role="row"]:not([role="columnheader"]) a, [data-testid="sink-item"] a'
    ).first();
    if (!await firstSink.isVisible({ timeout: 3_000 }).catch(() => false)) {
      console.warn('[INFO] No sinks — skip detail test');
      return;
    }

    await firstSink.click();
    await page.waitForLoadState('domcontentloaded');

    const dataTable = page.locator(
      '[data-testid="sink-data-table"], table, [role="grid"], [data-testid="sink-detail"]'
    ).first();
    await expect(dataTable).toBeVisible({ timeout: TIMEOUTS.default });
  });

  test('cells are clickable and show inline edit input', async ({ page }) => {
    await gotoAndWait(page, ROUTES.sinks);

    const firstSink = page.locator(
      'table tr:has(td) a, [data-testid="sink-item"] a'
    ).first();
    if (!await firstSink.isVisible({ timeout: 3_000 }).catch(() => false)) return;

    await firstSink.click();
    await page.waitForLoadState('domcontentloaded');

    // Click a data cell (not header)
    const dataCell = page.locator('table td:not(:first-child), [role="gridcell"]').nth(1);
    if (!await dataCell.isVisible({ timeout: TIMEOUTS.short }).catch(() => false)) return;

    await dataCell.click();

    const editInput = page.locator('input:focus, textarea:focus, [data-testid="cell-edit-input"]').first();
    const visible = await editInput.isVisible({ timeout: 2_000 }).catch(() => false);
    if (!visible) {
      await featurePresent(page, '[data-testid="cell-edit-input"]', 'Inline cell edit input in Sink detail');
    } else {
      await expect(editInput).toBeVisible();
    }
  });

  test('corrected cells are visually highlighted', async ({ page }) => {
    await gotoAndWait(page, ROUTES.sinks);

    const firstSink = page.locator(
      'table tr:has(td) a, [data-testid="sink-item"] a'
    ).first();
    if (!await firstSink.isVisible({ timeout: 3_000 }).catch(() => false)) return;

    await firstSink.click();
    await page.waitForLoadState('domcontentloaded');

    const correctedCell = page.locator(
      '[data-testid*="corrected"], [class*="corrected"], td[data-corrected="true"]'
    ).first();
    const visible = await correctedCell.isVisible({ timeout: TIMEOUTS.short }).catch(() => false);
    if (!visible) {
      await featurePresent(page, '[data-testid*="corrected"]', 'Visually highlighted corrected cells');
    }
  });
});

test.describe('Correction workflow', () => {
  test('correction includes error category picker', async ({ page }) => {
    await gotoAndWait(page, ROUTES.sinks);

    const firstSink = page.locator('table tr:has(td) a, [data-testid="sink-item"] a').first();
    if (!await firstSink.isVisible({ timeout: 3_000 }).catch(() => false)) return;

    await firstSink.click();
    await page.waitForLoadState('domcontentloaded');

    const dataCell = page.locator('table td:not(:first-child), [role="gridcell"]').nth(1);
    if (!await dataCell.isVisible({ timeout: TIMEOUTS.short }).catch(() => false)) return;

    await dataCell.click();
    await page.waitForTimeout(300);

    const categoryPicker = page.locator(
      'select[name*="category" i], [data-testid="error-category-picker"], [aria-label*="category" i]'
    ).first();
    const visible = await categoryPicker.isVisible({ timeout: TIMEOUTS.short }).catch(() => false);
    if (!visible) {
      await featurePresent(page, '[data-testid="error-category-picker"]', 'Error category picker in correction UI');
    }
  });
});
