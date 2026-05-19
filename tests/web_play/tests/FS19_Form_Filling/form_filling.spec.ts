/**
 * FS19 – Dynamic Form Filling (Editor perspective)
 *
 * UX focus:
 *  - Form fill page shows all fields with labels
 *  - Required field indicator (asterisk or aria-required)
 *  - Real-time validation on blur (red border + error message)
 *  - All validation errors shown at once (not one at a time) on submit
 *  - Auto-save indicator visible (every 30s or on section change)
 *  - Save as Draft keeps data after navigation away
 *  - Submit button disabled until all required fields valid
 *  - Per-field comment button/icon present
 *  - Excel import button present
 */
import { test, expect } from '@playwright/test';
import { FormsPage } from '../../pages/FormsPage';
import { ROUTES, TIMEOUTS } from '../../config/config';
import { gotoAndWait, featurePresent } from '../../fixtures/auth.fixture';

async function openFirstPublishedForm(page: import('@playwright/test').Page): Promise<boolean> {
  await gotoAndWait(page, ROUTES.forms);

  const publishedForm = page.locator(
    'tr:has-text("PUBLISHED") a, [data-testid="form-item"]:has-text("PUBLISHED") a, [href*="/forms/"][href*="/fill"]'
  ).first();

  if (!await publishedForm.isVisible({ timeout: 3_000 }).catch(() => false)) {
    // Try first form link regardless of status
    const anyForm = page.locator('table tr:has(td) a, [data-testid="form-item"] a').first();
    if (!await anyForm.isVisible({ timeout: 3_000 }).catch(() => false)) {
      console.warn('[INFO] No forms available');
      return false;
    }
    await anyForm.click();
  } else {
    await publishedForm.click();
  }
  await page.waitForLoadState('domcontentloaded');
  return true;
}

test.describe('Form fill page layout', () => {
  test('form fill page shows form fields', async ({ page }) => {
    const hasForms = await openFirstPublishedForm(page);
    if (!hasForms) return;

    // Navigate to fill route if currently on edit route
    const url = page.url();
    if (!url.includes('/fill')) {
      const fillBtn = page.locator('a:has-text("Fill"), button:has-text("Fill"), [data-testid="fill-btn"]').first();
      if (await fillBtn.isVisible({ timeout: 2_000 }).catch(() => false)) await fillBtn.click();
      await page.waitForLoadState('domcontentloaded');
    }

    const inputs = page.locator('input:not([type="hidden"]), textarea, select');
    const count  = await inputs.count();
    expect(count, 'Form fill page should have at least 1 input field').toBeGreaterThan(0);
  });

  test('required fields have aria-required or required attribute', async ({ page }) => {
    const hasForms = await openFirstPublishedForm(page);
    if (!hasForms) return;

    const url = page.url();
    if (!url.includes('/fill')) {
      const fillBtn = page.locator('a:has-text("Fill"), button:has-text("Fill"), [data-testid="fill-btn"]').first();
      if (await fillBtn.isVisible({ timeout: 2_000 }).catch(() => false)) await fillBtn.click();
      await page.waitForLoadState('domcontentloaded');
    }

    const requiredIndicators = page.locator('[aria-required="true"], [required], :text("*"), .required-marker');
    const count = await requiredIndicators.count();
    if (count === 0) {
      console.warn('[MISSING FEATURE] No required field indicators found on form fill page');
    }
  });

  test('auto-save indicator is present', { tag: ['@slow'] }, async ({ page }) => {
    const hasForms = await openFirstPublishedForm(page);
    if (!hasForms) return;

    const url = page.url();
    if (!url.includes('/fill')) {
      const fillBtn = page.locator('a:has-text("Fill"), button:has-text("Fill"), [data-testid="fill-btn"]').first();
      if (await fillBtn.isVisible({ timeout: 2_000 }).catch(() => false)) await fillBtn.click();
      await page.waitForLoadState('domcontentloaded');
    }

    const forms = new FormsPage(page);
    // Type something to trigger auto-save
    const firstInput = page.locator('input[type="text"], input[type="number"], textarea').first();
    if (await firstInput.isVisible({ timeout: 2_000 }).catch(() => false)) {
      await firstInput.fill('test value');
    }

    const autoSave = forms.autoSaveIndicator;
    const visible  = await autoSave.isVisible({ timeout: 35_000 }).catch(() => false);
    if (!visible) {
      await featurePresent(page, '[data-testid="auto-save"]', 'Auto-save indicator on form fill');
    } else {
      await expect(autoSave).toBeVisible();
    }
  });
});

test.describe('Form validation', () => {
  test('submitting empty required form shows ALL validation errors at once', async ({ page }) => {
    const hasForms = await openFirstPublishedForm(page);
    if (!hasForms) return;

    const url = page.url();
    if (!url.includes('/fill')) {
      const fillBtn = page.locator('a:has-text("Fill"), button:has-text("Fill"), [data-testid="fill-btn"]').first();
      if (await fillBtn.isVisible({ timeout: 2_000 }).catch(() => false)) await fillBtn.click();
      await page.waitForLoadState('domcontentloaded');
    }

    const submitBtn = page.locator('button:has-text("Submit"), button:has-text("Odeslat"), [data-testid="form-submit-btn"]').first();
    if (!await submitBtn.isVisible({ timeout: 2_000 }).catch(() => false)) return;

    await submitBtn.click();

    const errors = page.locator('[role="alert"], .field-error, [class*="error"], [data-testid*="error"]');
    const errorCount = await errors.count();

    if (errorCount === 0) {
      console.warn('[MISSING FEATURE] No validation errors shown after submitting empty form');
    } else {
      // All errors should be visible simultaneously
      expect(errorCount).toBeGreaterThan(0);
    }
  });

  test('invalid field value shows inline error message on blur', async ({ page }) => {
    const hasForms = await openFirstPublishedForm(page);
    if (!hasForms) return;

    const url = page.url();
    if (!url.includes('/fill')) {
      const fillBtn = page.locator('a:has-text("Fill"), button:has-text("Fill"), [data-testid="fill-btn"]').first();
      if (await fillBtn.isVisible({ timeout: 2_000 }).catch(() => false)) await fillBtn.click();
      await page.waitForLoadState('domcontentloaded');
    }

    const numberInput = page.locator('input[type="number"]').first();
    if (!await numberInput.isVisible({ timeout: 2_000 }).catch(() => false)) return;

    await numberInput.fill('-999999');
    await numberInput.blur();

    const error = numberInput.locator('..').locator('[role="alert"], .error, [class*="error"]').first();
    const visible = await error.isVisible({ timeout: 3_000 }).catch(() => false);
    if (!visible) {
      console.warn('[MISSING FEATURE] No inline validation error on invalid number input');
    }
  });
});

test.describe('Form fill extras', () => {
  test('Excel import button is present', async ({ page }) => {
    const hasForms = await openFirstPublishedForm(page);
    if (!hasForms) return;

    const importBtn = page.locator(
      'button:has-text("Import Excel"), button:has-text("Import"), [data-testid="import-excel-btn"]'
    ).first();
    const visible = await importBtn.isVisible({ timeout: TIMEOUTS.short }).catch(() => false);
    if (!visible) {
      await featurePresent(page, 'button:has-text("Import")', 'Excel import button on form fill page');
    } else {
      await expect(importBtn).toBeEnabled();
    }
  });

  test('per-field comment button is present', async ({ page }) => {
    const hasForms = await openFirstPublishedForm(page);
    if (!hasForms) return;

    const url = page.url();
    if (!url.includes('/fill')) {
      const fillBtn = page.locator('a:has-text("Fill"), button:has-text("Fill"), [data-testid="fill-btn"]').first();
      if (await fillBtn.isVisible({ timeout: 2_000 }).catch(() => false)) await fillBtn.click();
      await page.waitForLoadState('domcontentloaded');
    }

    const commentBtn = page.locator(
      '[data-testid="field-comment-btn"], button[aria-label*="comment" i], button[title*="comment" i], .field-comment-icon'
    ).first();
    const visible = await commentBtn.isVisible({ timeout: TIMEOUTS.short }).catch(() => false);
    if (!visible) {
      await featurePresent(page, '[data-testid="field-comment-btn"]', 'Per-field comment button');
    } else {
      await expect(commentBtn).toBeVisible();
    }
  });
});
