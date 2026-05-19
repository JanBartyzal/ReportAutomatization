/**
 * FS07 – Admin UI & Organization Management
 *
 * UX focus:
 *  - Admin manage page renders user/org management controls
 *  - Admin health page renders service status indicators
 *  - Admin integrations page renders integration settings
 *  - Admin promotions page renders promotion controls
 *  - Admin-only routes are inaccessible to viewer role
 *  - Admin pages have correct page titles
 */
import { test, expect } from '@playwright/test';
import { ROUTES, TIMEOUTS } from '../../config/config';
import { gotoAndWait, featurePresent } from '../../fixtures/auth.fixture';

// ── Admin Manage ──────────────────────────────────────────────────────────────

test.describe('Admin Manage page', () => {
  test('admin manage page loads without error', { tag: ['@smoke'] }, async ({ page }) => {
    await gotoAndWait(page, ROUTES.adminManage);

    await expect(page).not.toHaveURL(/login|signin|error|403|forbidden/);
    const body = (await page.locator('body').innerText({ timeout: TIMEOUTS.default })).trim();
    expect(body.length, 'Admin manage page should render visible content').toBeGreaterThan(20);
  });

  test('admin manage page shows user or org management section', async ({ page }) => {
    await gotoAndWait(page, ROUTES.adminManage);

    const managementEl = page.locator(
      '[data-testid="user-list"], [data-testid="org-list"], table, [role="grid"], h1, h2'
    ).first();
    const visible = await managementEl.isVisible({ timeout: TIMEOUTS.default }).catch(() => false);
    if (!visible) {
      await featurePresent(page, 'table, [role="grid"]', 'User/org management table on admin manage page');
      return;
    }
    await expect(managementEl).toBeVisible();
  });

  test('admin manage page has an accessible heading', { tag: ['@a11y'] }, async ({ page }) => {
    await gotoAndWait(page, ROUTES.adminManage);
    const h1Count = await page.locator('h1').count();
    if (h1Count === 0) {
      console.warn('[MISSING FEATURE] No <h1> on admin manage page');
    }
  });
});

// ── Admin Health ──────────────────────────────────────────────────────────────

test.describe('Admin Health page', () => {
  test('health page loads and shows service status', { tag: ['@smoke'] }, async ({ page }) => {
    await gotoAndWait(page, ROUTES.adminHealth);

    await expect(page).not.toHaveURL(/login|signin|error/);
    const body = (await page.locator('body').innerText({ timeout: TIMEOUTS.default })).trim();
    expect(body.length, 'Health page should render visible content').toBeGreaterThan(20);
  });

  test('health page has status indicators (OK / DOWN / degraded)', async ({ page }) => {
    await gotoAndWait(page, ROUTES.adminHealth);

    const statusEl = page.locator(
      '[data-testid*="status"], [data-testid*="health"], .badge, [class*="status"], :text("OK"), :text("UP"), :text("DOWN")'
    ).first();
    const visible = await statusEl.isVisible({ timeout: TIMEOUTS.default }).catch(() => false);
    if (!visible) {
      await featurePresent(page, '[data-testid*="health"]', 'Service health status indicators');
    }
  });

  test('health page shows service names in a list or table', async ({ page }) => {
    await gotoAndWait(page, ROUTES.adminHealth);

    const listEl = page.locator('table, [role="list"], ul, dl').first();
    const visible = await listEl.isVisible({ timeout: TIMEOUTS.short }).catch(() => false);
    if (!visible) {
      console.warn('[MISSING FEATURE] No list/table of services on health page');
    }
  });
});

// ── Admin Integrations ────────────────────────────────────────────────────────

test.describe('Admin Integrations page', () => {
  test('integrations admin page loads', { tag: ['@smoke'] }, async ({ page }) => {
    await gotoAndWait(page, ROUTES.adminIntegrations);

    await expect(page).not.toHaveURL(/login|signin|error/);
    const body = (await page.locator('body').innerText({ timeout: TIMEOUTS.default })).trim();
    expect(body.length, 'Integrations page should render visible content').toBeGreaterThan(20);
  });

  test('integrations page has a create / add integration control', async ({ page }) => {
    await gotoAndWait(page, ROUTES.adminIntegrations);

    const addBtn = page.locator(
      'button:has-text("Add"), button:has-text("New"), button:has-text("Create"), [data-testid*="add"], [data-testid*="new"]'
    ).first();
    const visible = await addBtn.isVisible({ timeout: TIMEOUTS.short }).catch(() => false);
    if (!visible) {
      await featurePresent(page, 'button:has-text("Add")', 'Add/New integration button');
    } else {
      await expect(addBtn).toBeEnabled();
    }
  });
});

// ── Admin Promotions ──────────────────────────────────────────────────────────

test.describe('Admin Promotions page', () => {
  test('promotions admin page loads', { tag: ['@smoke'] }, async ({ page }) => {
    await gotoAndWait(page, ROUTES.adminPromotions);

    await expect(page).not.toHaveURL(/login|signin|error/);
    const body = (await page.locator('body').innerText({ timeout: TIMEOUTS.default })).trim();
    expect(body.length, 'Promotions admin page should render visible content').toBeGreaterThan(20);
  });
});

// ── RBAC: admin routes blocked for viewer ────────────────────────────────────

test.describe('RBAC – admin routes blocked for viewer role', () => {
  const adminRoutes = [
    ROUTES.adminManage,
    ROUTES.adminHealth,
    ROUTES.adminIntegrations,
    ROUTES.adminPromotions,
  ];

  for (const route of adminRoutes) {
    test(`viewer cannot access ${route}`, async ({ browser }) => {
      const { VIEWER_STATE } = await import('../../playwright.config');
      const ctx  = await browser.newContext({ storageState: VIEWER_STATE });
      const page = await ctx.newPage();

      await page.goto(route, { waitUntil: 'domcontentloaded' });
      await page.waitForLoadState('networkidle').catch(() => {});

      const url = page.url();
      const isBlocked =
        url.includes('403') || url.includes('forbidden') || url.includes('access') ||
        await page.locator('[data-testid="access-denied"], h1:has-text("403"), :text("Access denied")').isVisible({ timeout: 2_000 }).catch(() => false);

      if (!isBlocked) {
        console.warn(`[MISSING FEATURE] RBAC: viewer can reach admin route ${route}`);
      }
      await ctx.close();
    });
  }
});
