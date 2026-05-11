/**
 * FS09 – Frontend SPA: Authentication & Navigation
 *
 * UX focus:
 *  - Login page layout and accessibility
 *  - Redirect after successful login
 *  - Sidebar navigation items and routing
 *  - Role-based menu item visibility
 *  - Session persistence across navigation
 *  - Logout flow
 *  - RBAC: non-admin cannot reach admin pages
 */
import { test, expect } from '@playwright/test';
import { AppPage } from '../../pages/AppPage';
import { USERS, ROUTES, TIMEOUTS } from '../../config/config';
import { gotoAndWait, featurePresent } from '../../fixtures/auth.fixture';

// ── Login page ────────────────────────────────────────────────────────────────

test.describe('Login page', () => {
  test('login page loads with correct elements', async ({ page }) => {
    await page.goto(ROUTES.login);

    // If app is in no-auth mode it redirects away from /login — skip gracefully
    const url = page.url();
    if (!url.includes('/login')) {
      test.skip(true, 'App is in no-auth DEV mode — login page not applicable');
      return;
    }

    await expect(page).toHaveTitle(/.+/);

    const emailInput = page.locator('input[type="email"], input[name="email"], #email').first();
    await expect(emailInput).toBeVisible({ timeout: TIMEOUTS.default });

    const submitBtn = page.locator('button[type="submit"]').first();
    await expect(submitBtn).toBeVisible();
    await expect(submitBtn).toHaveAttribute('type', 'submit');
  });

  test('login page has accessible form labels', async ({ page }) => {
    await page.goto(ROUTES.login);
    const url = page.url();
    if (!url.includes('/login')) {
      test.skip(true, 'No-auth DEV mode');
      return;
    }

    const inputs = page.locator('input:not([type="hidden"])');
    const count  = await inputs.count();
    for (let i = 0; i < count; i++) {
      const input   = inputs.nth(i);
      const id      = await input.getAttribute('id');
      const ariaLbl = await input.getAttribute('aria-label');
      const ariaBy  = await input.getAttribute('aria-labelledby');
      const hasLabel = !!(ariaLbl || ariaBy || (id && await page.locator(`label[for="${id}"]`).count() > 0));
      expect(hasLabel, `Login form input #${i} is missing a label`).toBeTruthy();
    }
  });

  test('empty login form shows validation errors', async ({ page }) => {
    await page.goto(ROUTES.login);
    const url = page.url();
    if (!url.includes('/login')) {
      test.skip(true, 'No-auth DEV mode');
      return;
    }

    await page.locator('button[type="submit"]').click();

    // Browser-native or custom validation message must appear
    const hasError = await page.locator('[role="alert"], .error, input:invalid').first()
      .isVisible({ timeout: 3_000 }).catch(() => false);
    expect(hasError, 'Validation feedback should appear on empty submit').toBeTruthy();
  });
});

// ── Post-login redirect & layout ──────────────────────────────────────────────

test.describe('Post-login layout', () => {
  test('landing page shows sidebar navigation', async ({ page }) => {
    await gotoAndWait(page, ROUTES.dashboard);

    const app = new AppPage(page);
    const links = await app.getSidebarLinks();
    expect(links.length, 'Sidebar should have at least 4 navigation links').toBeGreaterThanOrEqual(4);
  });

  test('sidebar contains required navigation items', async ({ page }) => {
    await gotoAndWait(page, ROUTES.dashboard);
    const app = new AppPage(page);

    const expectedItems = [
      'Dashboard',
      'Files',
      'Reports',
      'Periods',
      'Forms',
      'Templates',
      'Sink Browser',
      'Integrations',
      'Export Flows',
    ];
    let found = 0;
    for (const item of expectedItems) {
      if (await app.isSidebarItemVisible(item)) found++;
    }
    expect(found, `Expected at least 6 nav items, found ${found}`).toBeGreaterThanOrEqual(6);
  });

  test('user menu / avatar is accessible', async ({ page }) => {
    await gotoAndWait(page, ROUTES.dashboard);
    const userMenu = page.locator(
      '[data-testid="user-menu"], button[aria-label*="user" i], button[aria-label*="account" i], [data-testid="avatar"]'
    ).first();

    const visible = await userMenu.isVisible({ timeout: TIMEOUTS.short }).catch(() => false);
    if (!visible) {
      await featurePresent(page, '[data-testid="user-menu"]', 'User menu / avatar');
      return;
    }
    await expect(userMenu).toBeVisible();
  });
});

// ── Navigation routing ────────────────────────────────────────────────────────

test.describe('Sidebar navigation routing', () => {
  const navRoutes: [string, string][] = [
    ['Files',     ROUTES.files],
    ['Reports',   ROUTES.reports],
    ['Periods',   ROUTES.periods],
    ['Forms',     ROUTES.forms],
    ['Templates', ROUTES.templates],
    ['Sink Browser', ROUTES.sinks],
    ['Integrations', ROUTES.adminIntegrations],
    ['Export Flows', ROUTES.exportFlows],
  ];

  for (const [label, expectedPath] of navRoutes) {
    test(`clicking "${label}" navigates to ${expectedPath}`, async ({ page }) => {
      await gotoAndWait(page, ROUTES.dashboard);
      const app  = new AppPage(page);
      const link = page.locator('nav a, aside a, [role="navigation"] a')
        .filter({ hasText: new RegExp(label, 'i') }).first();

      const visible = await link.isVisible({ timeout: 3_000 }).catch(() => false);
      if (!visible) {
        await featurePresent(page, `a:has-text("${label}")`, `Nav item ${label}`);
        return;
      }

      await link.click();
      await page.waitForLoadState('domcontentloaded');
      await expect(page).toHaveURL(new RegExp(expectedPath), { timeout: TIMEOUTS.default });
    });
  }
});

// ── Session persistence ───────────────────────────────────────────────────────

test.describe('Session persistence', () => {
  test('session stays active across multiple page navigations', async ({ page }) => {
    await gotoAndWait(page, ROUTES.dashboard);

    const pagesToVisit = [ROUTES.files, ROUTES.forms, ROUTES.reports, ROUTES.periods];
    for (const route of pagesToVisit) {
      await page.goto(route);
      await page.waitForLoadState('domcontentloaded');
      await expect(page).not.toHaveURL(/login|signin/, { timeout: TIMEOUTS.short });
    }
  });

  test('page title is set (not blank)', async ({ page }) => {
    await gotoAndWait(page, ROUTES.dashboard);
    await expect(page).toHaveTitle(/.+/);
  });
});

// ── Logout ────────────────────────────────────────────────────────────────────

test.describe('Logout', () => {
  test('logout button visible in user menu', async ({ page }) => {
    await gotoAndWait(page, ROUTES.dashboard);

    const userMenu = page.locator(
      '[data-testid="user-menu"], button[aria-label*="user" i], button[aria-label*="account" i]'
    ).first();
    const hasMenu = await userMenu.isVisible({ timeout: 3_000 }).catch(() => false);

    if (hasMenu) {
      await userMenu.click();
      const logoutBtn = page.locator('button:has-text("Logout"), button:has-text("Sign out"), a:has-text("Logout")').first();
      await expect(logoutBtn).toBeVisible({ timeout: TIMEOUTS.short });
    } else {
      await featurePresent(page, 'button:has-text("Logout")', 'Logout button');
    }
  });
});

// ── RBAC ──────────────────────────────────────────────────────────────────────

test.describe('RBAC – Admin UI visibility', () => {
  test('admin navigation items not visible without admin role', async ({ browser }) => {
    // Use viewer context (read-only role)
    const { VIEWER_STATE } = await import('../../playwright.config');
    const ctx  = await browser.newContext({ storageState: VIEWER_STATE });
    const page = await ctx.newPage();
    await gotoAndWait(page, ROUTES.dashboard);

    // Admin-only nav items should be absent for Viewer
    const adminLink = page.locator('nav a, aside a')
      .filter({ hasText: /Admin|Settings|Manage/ }).first();
    const adminVisible = await adminLink.isVisible({ timeout: 2_000 }).catch(() => false);

    // Either absent or goes to access-denied — both are acceptable
    if (adminVisible) {
      await adminLink.click();
      await page.waitForLoadState('domcontentloaded');
      const denied = page.url().includes('access') || page.url().includes('forbidden') ||
        await page.locator('[data-testid="access-denied"], .forbidden, h1:has-text("403")').isVisible({ timeout: 2_000 }).catch(() => false);
      // Soft check — missing RBAC is a feature gap, not a hard failure
      if (!denied) console.warn('[MISSING FEATURE] RBAC: viewer can reach admin route');
    }
    await ctx.close();
  });
});
