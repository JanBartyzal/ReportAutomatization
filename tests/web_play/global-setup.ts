/**
 * Global setup: creates auth storageState files for each user role.
 * Runs once before all test projects.
 *
 * Dev bypass mode: the frontend skips MSAL when no Azure credentials are set —
 * navigation to / auto-redirects to /dashboard without any login form.
 * When a real login form is present, we fill in dev credentials.
 */
import { chromium, FullConfig } from '@playwright/test';
import fs from 'fs';
import path from 'path';
import { USERS, ROUTES, BASE_URL } from './config/config';
import { ADMIN_STATE, EDITOR_STATE, VIEWER_STATE } from './playwright.config';

async function saveAuthState(
  browser: ReturnType<typeof chromium.launch> extends Promise<infer T> ? T : never,
  user: { email: string; password: string },
  outputPath: string,
): Promise<void> {
  const context = await browser.newContext();
  const page = await context.newPage();

  await page.goto(BASE_URL, { waitUntil: 'domcontentloaded' });

  // Wait to see if app redirects to dashboard (no-auth DEV mode)
  try {
    await page.waitForURL(/\/(dashboard|files|reports|forms|upload)/, { timeout: 5_000 });
    // No-auth mode: already inside the app
  } catch {
    // Auth form is present — try dev login
    const emailInput = page.locator('input[type="email"], input[name="email"], #email').first();
    if (await emailInput.isVisible({ timeout: 3_000 }).catch(() => false)) {
      await emailInput.fill(user.email);
      const passwordInput = page.locator('input[type="password"]').first();
      if (await passwordInput.isVisible({ timeout: 2_000 }).catch(() => false)) {
        await passwordInput.fill(user.password || 'admin123');
      }
      await page.locator('button[type="submit"]').click();
      await page.waitForURL(/\/(dashboard|files|reports)/, { timeout: 15_000 });
    } else {
      // Try dev bypass button
      const devBtn = page.locator('button:has-text("Dev Login"), button:has-text("Bypass"), [data-testid="dev-login"]').first();
      if (await devBtn.isVisible({ timeout: 2_000 }).catch(() => false)) {
        await devBtn.click();
        await page.waitForURL(/\/(dashboard|files|reports)/, { timeout: 10_000 });
      }
    }
  }

  fs.mkdirSync(path.dirname(outputPath), { recursive: true });
  await context.storageState({ path: outputPath });
  await context.close();
}

export default async function globalSetup(_config: FullConfig): Promise<void> {
  const browser = await chromium.launch();

  try {
    await Promise.all([
      saveAuthState(browser, USERS.holdingAdmin, ADMIN_STATE),
      saveAuthState(browser, USERS.editor,       EDITOR_STATE),
      saveAuthState(browser, USERS.viewer,        VIEWER_STATE),
    ]);
    console.log('[global-setup] Auth state files saved.');
  } finally {
    await browser.close();
  }
}
