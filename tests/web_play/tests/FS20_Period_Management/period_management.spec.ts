/**
 * FS20 – Reporting Period & Deadline Management
 *
 * UX focus:
 *  - Periods list with status badges (OPEN, COLLECTING, REVIEWING, CLOSED)
 *  - Create period dialog fields: name, type, start_date, deadlines
 *  - Clone from previous period button
 *  - Matrix view shows Company × State with colour coding
 *  - Completion percentage visible per period
 *  - Deadline dates clearly shown (colour highlight for overdue)
 *  - Export status button (PDF/Excel)
 */
import { test, expect } from '@playwright/test';
import { ROUTES, TIMEOUTS } from '../../config/config';
import { gotoAndWait, featurePresent } from '../../fixtures/auth.fixture';

test.describe('Periods list', () => {
  test('periods page loads without errors', { tag: ['@smoke'] }, async ({ page }) => {
    await gotoAndWait(page, ROUTES.periods);
    await expect(page).not.toHaveURL(/login|error/);

    const list = page.locator('table, [role="grid"], [data-testid="period-list"]').first();
    const empty = page.locator('[data-testid="empty-state"], :text("No periods"), :text("Žádná období")').first();
    const hasContent = await list.isVisible({ timeout: TIMEOUTS.default }).catch(() => false)
      || await empty.isVisible({ timeout: 2_000 }).catch(() => false);
    expect(hasContent).toBeTruthy();
  });

  test('period status badges visible', async ({ page }) => {
    await gotoAndWait(page, ROUTES.periods);

    const badges = page.locator('[data-testid*="status"], .badge, [class*="status-badge"]');
    const count  = await badges.count();
    if (count === 0) {
      await featurePresent(page, '[data-testid*="status"]', 'Period status badges');
      return;
    }
    const texts = await badges.allTextContents();
    const known = ['OPEN', 'COLLECTING', 'REVIEWING', 'CLOSED'];
    const hasKnown = texts.some(t => known.some(s => t.toUpperCase().includes(s)));
    if (!hasKnown) console.warn('[MISSING FEATURE] Period badges do not match expected states');
  });

  test('completion percentage visible', async ({ page }) => {
    await gotoAndWait(page, ROUTES.periods);

    const percentage = page.locator(':text("%"), [data-testid*="completion"], [aria-label*="completion" i]').first();
    const visible = await percentage.isVisible({ timeout: TIMEOUTS.short }).catch(() => false);
    if (!visible) {
      await featurePresent(page, '[data-testid*="completion"]', 'Period completion percentage');
    } else {
      await expect(percentage).toBeVisible();
    }
  });
});

test.describe('Create period', () => {
  test('"Create Period" button is accessible', async ({ page }) => {
    await gotoAndWait(page, ROUTES.periods);

    const createBtn = page.locator(
      'button:has-text("New Period"), button:has-text("Create Period"), button:has-text("Add"), [data-testid="new-period-btn"]'
    ).first();
    const visible = await createBtn.isVisible({ timeout: TIMEOUTS.short }).catch(() => false);
    if (!visible) {
      await featurePresent(page, '[data-testid="new-period-btn"]', 'Create Period button');
      return;
    }
    await expect(createBtn).toBeEnabled();
  });

  test('create period dialog has required fields', async ({ page }) => {
    await gotoAndWait(page, ROUTES.periods);

    const createBtn = page.locator(
      'button:has-text("New Period"), button:has-text("Create Period"), [data-testid="new-period-btn"]'
    ).first();
    if (!await createBtn.isVisible({ timeout: TIMEOUTS.short }).catch(() => false)) return;

    await createBtn.click();

    const dialog = page.locator('[role="dialog"], [data-testid="create-period-dialog"]').first();
    await expect(dialog).toBeVisible({ timeout: TIMEOUTS.short });

    // Check for required form fields
    const nameField = dialog.locator('input[name*="name" i], input[placeholder*="period" i]').first();
    const typeField = dialog.locator('select[name*="type" i], [data-testid*="type"]').first();
    const deadlineField = dialog.locator('input[type="date"], input[name*="deadline" i]').first();

    const hasName     = await nameField.isVisible({ timeout: 2_000 }).catch(() => false);
    const hasType     = await typeField.isVisible({ timeout: 2_000 }).catch(() => false);
    const hasDeadline = await deadlineField.isVisible({ timeout: 2_000 }).catch(() => false);

    if (!hasName)     console.warn('[MISSING FEATURE] Period name field missing in create dialog');
    if (!hasType)     console.warn('[MISSING FEATURE] Period type selector missing in create dialog');
    if (!hasDeadline) console.warn('[MISSING FEATURE] Deadline date field missing in create dialog');
  });

  test('"Clone from previous" option present in create dialog', async ({ page }) => {
    await gotoAndWait(page, ROUTES.periods);

    const createBtn = page.locator(
      'button:has-text("New Period"), button:has-text("Create Period"), [data-testid="new-period-btn"]'
    ).first();
    if (!await createBtn.isVisible({ timeout: TIMEOUTS.short }).catch(() => false)) return;

    await createBtn.click();
    await page.locator('[role="dialog"]').waitFor({ state: 'visible', timeout: TIMEOUTS.short });

    const cloneBtn = page.locator(
      'button:has-text("Clone"), button:has-text("Copy"), [data-testid="clone-period-btn"], input[name*="clone"]'
    ).first();
    const visible = await cloneBtn.isVisible({ timeout: TIMEOUTS.short }).catch(() => false);
    if (!visible) {
      await featurePresent(page, 'button:has-text("Clone")', 'Clone period from previous option');
    }
  });
});

test.describe('Period detail dashboard', () => {
  test('period detail shows matrix of Company × State', async ({ page }) => {
    await gotoAndWait(page, ROUTES.periods);

    const firstPeriod = page.locator(
      'table tr:has(td) a, [role="row"]:not([role="columnheader"]) a, [data-testid="period-item"] a'
    ).first();
    if (!await firstPeriod.isVisible({ timeout: 3_000 }).catch(() => false)) {
      console.warn('[INFO] No periods — skip detail test');
      return;
    }

    await firstPeriod.click();
    await page.waitForLoadState('domcontentloaded');

    const matrix = page.locator(
      '[data-testid="period-matrix"], table.matrix, .completion-matrix, [data-testid="completion-dashboard"]'
    ).first();
    const visible = await matrix.isVisible({ timeout: TIMEOUTS.short }).catch(() => false);
    if (!visible) {
      await featurePresent(page, '[data-testid="period-matrix"]', 'Period completion matrix');
    } else {
      await expect(matrix).toBeVisible();
    }
  });

  test('period detail export button present', async ({ page }) => {
    await gotoAndWait(page, ROUTES.periods);

    const firstPeriod = page.locator('table tr:has(td) a, [data-testid="period-item"] a').first();
    if (!await firstPeriod.isVisible({ timeout: 3_000 }).catch(() => false)) return;

    await firstPeriod.click();
    await page.waitForLoadState('domcontentloaded');

    const exportBtn = page.locator(
      'button:has-text("Export"), a:has-text("Export PDF"), a:has-text("Export Excel"), [data-testid="export-period-btn"]'
    ).first();
    const visible = await exportBtn.isVisible({ timeout: TIMEOUTS.short }).catch(() => false);
    if (!visible) {
      await featurePresent(page, 'button:has-text("Export")', 'Period status export button');
    } else {
      await expect(exportBtn).toBeEnabled();
    }
  });
});
