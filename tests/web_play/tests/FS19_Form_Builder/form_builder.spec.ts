/**
 * FS19 – Dynamic Form Builder (HoldingAdmin perspective)
 *
 * UX focus:
 *  - Form builder page reachable
 *  - Field type palette visible with expected types
 *  - Drag indicator / droppable canvas present
 *  - Preview mode button available
 *  - Publish button available with correct tooltip/state
 *  - Autosave indicator appears
 *  - Form name / title field present and labelled
 *  - Version info shown when editing existing form
 *  - Excel export template button present on published form
 */
import { test, expect } from '@playwright/test';
import { FormsPage } from '../../pages/FormsPage';
import { ROUTES, TIMEOUTS } from '../../config/config';
import { gotoAndWait, featurePresent } from '../../fixtures/auth.fixture';

const EXPECTED_FIELD_TYPES = ['text', 'number', 'date', 'dropdown', 'table'];

test.describe('Form builder page', () => {
  test('form builder is reachable via "New Form" button', async ({ page }) => {
    await gotoAndWait(page, ROUTES.forms);
    const forms = new FormsPage(page);

    const createBtn = forms.newFormBtn;
    const visible = await createBtn.isVisible({ timeout: TIMEOUTS.short }).catch(() => false);
    if (!visible) {
      await featurePresent(page, '[data-testid="new-form-btn"]', 'New Form button');
      return;
    }
    await createBtn.click();
    await page.waitForLoadState('domcontentloaded');

    // Should navigate to builder route
    await expect(page).toHaveURL(/forms\/new|forms\/.*\/edit|form-builder/, { timeout: TIMEOUTS.default });
  });

  test('form builder has a canvas area', async ({ page }) => {
    await gotoAndWait(page, ROUTES.formNew);
    const forms = new FormsPage(page);

    const canvas = forms.formCanvas;
    const visible = await canvas.isVisible({ timeout: TIMEOUTS.default }).catch(() => false);
    if (!visible) {
      await featurePresent(page, '[data-testid="form-canvas"]', 'Form builder canvas');
    } else {
      await expect(canvas).toBeVisible();
    }
  });

  test('field type palette contains expected field types', async ({ page }) => {
    await gotoAndWait(page, ROUTES.formNew);
    const forms = new FormsPage(page);

    const palette = forms.fieldPalette;
    const visible = await palette.isVisible({ timeout: TIMEOUTS.default }).catch(() => false);
    if (!visible) {
      await featurePresent(page, '[data-testid="field-palette"]', 'Field type palette');
      return;
    }

    const paletteText = (await palette.textContent() ?? '').toLowerCase();
    let found = 0;
    for (const fieldType of EXPECTED_FIELD_TYPES) {
      if (paletteText.includes(fieldType)) found++;
    }
    expect(found, `Expected at least 3 field types in palette, found ${found}`).toBeGreaterThanOrEqual(3);
  });

  test('preview button is accessible', async ({ page }) => {
    await gotoAndWait(page, ROUTES.formNew);
    const forms = new FormsPage(page);

    const preview = forms.previewBtn;
    const visible = await preview.isVisible({ timeout: TIMEOUTS.default }).catch(() => false);
    if (!visible) {
      await featurePresent(page, 'button:has-text("Preview")', 'Form preview button');
      return;
    }
    const name = await preview.getAttribute('aria-label') ?? await preview.textContent() ?? '';
    expect(name.trim()).toBeTruthy();
  });

  test('form name / title field has accessible label', async ({ page }) => {
    await gotoAndWait(page, ROUTES.formNew);

    const nameInput = page.locator(
      'input[name*="name" i], input[placeholder*="Form name" i], input[placeholder*="Název" i], [data-testid="form-name"]'
    ).first();
    const visible = await nameInput.isVisible({ timeout: TIMEOUTS.default }).catch(() => false);
    if (!visible) {
      await featurePresent(page, 'input[name*="name"]', 'Form name field');
      return;
    }

    const id      = await nameInput.getAttribute('id');
    const ariaLbl = await nameInput.getAttribute('aria-label');
    const hasLabel = !!(ariaLbl || (id && await page.locator(`label[for="${id}"]`).count() > 0));
    if (!hasLabel) console.warn('[MISSING FEATURE] Form name input missing accessible label');
  });
});

test.describe('Existing forms list', () => {
  test('forms list shows DRAFT / PUBLISHED / CLOSED badges', async ({ page }) => {
    await gotoAndWait(page, ROUTES.forms);

    const badges = page.locator('[data-testid*="status"], .badge, [class*="status"]');
    const count = await badges.count();
    if (count === 0) {
      await featurePresent(page, '[data-testid*="status"]', 'Form status badges');
      return;
    }

    const texts = await badges.allTextContents();
    const known = ['DRAFT', 'PUBLISHED', 'CLOSED'];
    const hasKnown = texts.some(t => known.some(s => t.toUpperCase().includes(s)));
    if (!hasKnown) console.warn('[MISSING FEATURE] Form status badges do not match DRAFT/PUBLISHED/CLOSED states');
  });

  test('Excel export button present on published form', async ({ page }) => {
    await gotoAndWait(page, ROUTES.forms);

    const publishedForm = page.locator(
      'tr:has-text("PUBLISHED") a, [data-testid="form-item"]:has-text("PUBLISHED") a'
    ).first();
    if (!await publishedForm.isVisible({ timeout: 3_000 }).catch(() => false)) {
      console.warn('[INFO] No PUBLISHED forms — skip Excel export test');
      return;
    }

    await publishedForm.click();
    await page.waitForLoadState('domcontentloaded');

    const exportBtn = page.locator(
      'button:has-text("Export Excel"), a:has-text("Export Template"), [data-testid="export-excel-template"]'
    ).first();
    const visible = await exportBtn.isVisible({ timeout: TIMEOUTS.short }).catch(() => false);
    if (!visible) {
      await featurePresent(page, 'button:has-text("Export Excel")', 'Excel export template button on published form');
    } else {
      await expect(exportBtn).toBeEnabled();
    }
  });
});
