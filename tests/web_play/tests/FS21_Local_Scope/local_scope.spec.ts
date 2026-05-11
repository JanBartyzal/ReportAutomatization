/**
 * FS21 – Local Forms & Local PPTX Templates
 *
 * UX focus:
 *  - /local route loads (local dashboard)
 *  - Scope indicator (CENTRAL vs LOCAL) visible on forms/templates
 *  - "Create local form" available for CompanyAdmin
 *  - Local templates tab accessible
 *  - Forms/templates list has SCOPE column or badge
 *  - "Release to Holding" action available for local items (CompanyAdmin)
 */
import { test, expect } from '@playwright/test';
import { ROUTES, TIMEOUTS } from '../../config/config';
import { gotoAndWait, featurePresent } from '../../fixtures/auth.fixture';

test.describe('Local scope route', () => {
  test('/local route loads without errors', async ({ page }) => {
    await gotoAndWait(page, ROUTES.local);
    await expect(page).not.toHaveURL(/login|error/);

    const content = page.locator('main, [role="main"], [data-testid="local-dashboard"]').first();
    const visible  = await content.isVisible({ timeout: TIMEOUTS.default }).catch(() => false);
    if (!visible) {
      await featurePresent(page, '[data-testid="local-dashboard"]', 'Local dashboard (/local route)');
    } else {
      await expect(content).toBeVisible();
    }
  });
});

test.describe('Scope indicators', () => {
  test('forms list shows scope badge (CENTRAL / LOCAL)', async ({ page }) => {
    await gotoAndWait(page, ROUTES.forms);

    const scopeBadge = page.locator(
      ':text("CENTRAL"), :text("LOCAL"), :text("Lokální"), :text("Centrální"), [data-testid*="scope"]'
    ).first();
    const visible = await scopeBadge.isVisible({ timeout: TIMEOUTS.short }).catch(() => false);
    if (!visible) {
      await featurePresent(page, '[data-testid*="scope"]', 'Scope badges (CENTRAL/LOCAL) on forms list');
    } else {
      await expect(scopeBadge).toBeVisible();
    }
  });

  test('templates list shows scope badge', async ({ page }) => {
    await gotoAndWait(page, ROUTES.templates);

    const scopeBadge = page.locator(
      ':text("CENTRAL"), :text("LOCAL"), [data-testid*="scope"]'
    ).first();
    const visible = await scopeBadge.isVisible({ timeout: TIMEOUTS.short }).catch(() => false);
    if (!visible) {
      await featurePresent(page, '[data-testid*="scope"]', 'Scope badges on templates list');
    }
  });
});

test.describe('Local form creation', () => {
  test('local scope option available in new form wizard', async ({ page }) => {
    await gotoAndWait(page, ROUTES.formNew);

    const localScope = page.locator(
      'input[value="LOCAL"], option[value="LOCAL"], [data-testid="scope-local"], label:has-text("Local"), label:has-text("Lokální")'
    ).first();
    const visible = await localScope.isVisible({ timeout: TIMEOUTS.default }).catch(() => false);
    if (!visible) {
      await featurePresent(page, 'input[value="LOCAL"]', 'LOCAL scope option in new form wizard');
    } else {
      await expect(localScope).toBeVisible();
    }
  });
});

test.describe('"Release to Holding" action', () => {
  test('local forms have "Release" option', async ({ page }) => {
    await gotoAndWait(page, ROUTES.forms);

    const localForm = page.locator(
      'tr:has-text("LOCAL") a, [data-testid="form-item"]:has-text("LOCAL") a'
    ).first();
    if (!await localForm.isVisible({ timeout: 3_000 }).catch(() => false)) {
      console.warn('[INFO] No LOCAL scope forms found — skip');
      return;
    }

    await localForm.click();
    await page.waitForLoadState('domcontentloaded');

    const releaseBtn = page.locator(
      'button:has-text("Release"), button:has-text("Share"), [data-testid="release-to-holding-btn"]'
    ).first();
    const visible = await releaseBtn.isVisible({ timeout: TIMEOUTS.short }).catch(() => false);
    if (!visible) {
      await featurePresent(page, 'button:has-text("Release")', '"Release to Holding" button on local form');
    } else {
      await expect(releaseBtn).toBeEnabled();
    }
  });
});
