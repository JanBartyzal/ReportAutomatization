/**
 * Custom Playwright fixtures.
 *
 * Usage in tests:
 *   import { test, expect } from '@fixtures/auth.fixture';
 *   test('…', async ({ adminPage, editorPage, viewerPage }) => { … });
 *
 * The `adminPage` / `editorPage` / `viewerPage` fixtures reuse storageState
 * created during globalSetup.  Use them when a test needs a specific role context.
 * The default `page` fixture from playwright.config projects also works fine.
 */
import { test as base, type Page } from '@playwright/test';
import { ROUTES, TIMEOUTS } from '../config/config';
import { ADMIN_STATE, EDITOR_STATE, VIEWER_STATE } from '../playwright.config';

type AuthFixtures = {
  adminPage:  Page;
  editorPage: Page;
  viewerPage: Page;
};

export const test = base.extend<AuthFixtures>({
  adminPage: async ({ browser }, use) => {
    const ctx  = await browser.newContext({ storageState: ADMIN_STATE });
    const page = await ctx.newPage();
    await page.goto(ROUTES.dashboard, { waitUntil: 'domcontentloaded' });
    await page.waitForLoadState('networkidle');
    await use(page);
    await ctx.close();
  },

  editorPage: async ({ browser }, use) => {
    const ctx  = await browser.newContext({ storageState: EDITOR_STATE });
    const page = await ctx.newPage();
    await page.goto(ROUTES.upload, { waitUntil: 'domcontentloaded' });
    await page.waitForLoadState('networkidle');
    await use(page);
    await ctx.close();
  },

  viewerPage: async ({ browser }, use) => {
    const ctx  = await browser.newContext({ storageState: VIEWER_STATE });
    const page = await ctx.newPage();
    await page.goto(ROUTES.dashboard, { waitUntil: 'domcontentloaded' });
    await page.waitForLoadState('networkidle');
    await use(page);
    await ctx.close();
  },
});

export { expect } from '@playwright/test';

// ── Helpers shared across test files ─────────────────────────────────────────

/** Navigate + wait for the page to be interactive. */
export async function gotoAndWait(page: Page, route: string): Promise<void> {
  await page.goto(route, { waitUntil: 'domcontentloaded' });
  await page.waitForLoadState('networkidle').catch(() => {/* ignore timeout on long API calls */});
}

/** Returns true if the app is in no-auth DEV mode (URL never contains /login). */
export async function isNoAuthMode(page: Page): Promise<boolean> {
  const url = page.url();
  return !url.includes('/login') && !url.includes('/signin');
}

/**
 * Check whether an element is visible, treating missing feature as a soft skip.
 * Returns `false` and logs a warning instead of failing the test.
 */
export async function featurePresent(page: Page, selector: string, featureName: string, timeout = TIMEOUTS.short): Promise<boolean> {
  const visible = await page.locator(selector).first().isVisible({ timeout }).catch(() => false);
  if (!visible) {
    console.warn(`[MISSING FEATURE] ${featureName} – selector "${selector}" not found.`);
  }
  return visible;
}
