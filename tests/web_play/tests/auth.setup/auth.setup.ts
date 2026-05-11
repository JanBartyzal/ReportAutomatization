/**
 * Auth setup tests – executed by the 'setup' project before other projects.
 * Validates that storageState files were created by globalSetup and are usable.
 */
import { test, expect } from '@playwright/test';
import fs from 'fs';
import { ADMIN_STATE, EDITOR_STATE, VIEWER_STATE } from '../../playwright.config';
import { ROUTES } from '../../config/config';

test('holding-admin auth state is valid', async ({ browser }) => {
  expect(fs.existsSync(ADMIN_STATE), 'Admin auth state file must exist').toBeTruthy();
  const ctx  = await browser.newContext({ storageState: ADMIN_STATE });
  const page = await ctx.newPage();
  await page.goto(ROUTES.dashboard);
  await expect(page).not.toHaveURL(/login|signin/, { timeout: 10_000 });
  await ctx.close();
});

test('editor auth state is valid', async ({ browser }) => {
  expect(fs.existsSync(EDITOR_STATE), 'Editor auth state file must exist').toBeTruthy();
  const ctx  = await browser.newContext({ storageState: EDITOR_STATE });
  const page = await ctx.newPage();
  await page.goto(ROUTES.upload);
  await expect(page).not.toHaveURL(/login|signin/, { timeout: 10_000 });
  await ctx.close();
});

test('viewer auth state is valid', async ({ browser }) => {
  expect(fs.existsSync(VIEWER_STATE), 'Viewer auth state file must exist').toBeTruthy();
  const ctx  = await browser.newContext({ storageState: VIEWER_STATE });
  const page = await ctx.newPage();
  await page.goto(ROUTES.dashboard);
  await expect(page).not.toHaveURL(/login|signin/, { timeout: 10_000 });
  await ctx.close();
});
