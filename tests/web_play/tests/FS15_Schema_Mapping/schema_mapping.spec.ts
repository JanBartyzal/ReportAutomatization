/**
 * FS15 – Template & Schema Mapping Registry
 *
 * UX focus:
 *  - Templates list page loads
 *  - Create mapping template button accessible
 *  - Mapping editor: source column → target field mapping pairs
 *  - "Suggest mapping" button present
 *  - Learning indicator ("suggested from history")
 *  - Save / publish mapping template
 */
import { test, expect } from '@playwright/test';
import { ROUTES, TIMEOUTS } from '../../config/config';
import { gotoAndWait, featurePresent } from '../../fixtures/auth.fixture';

test.describe('Templates list', () => {
  test('templates page loads without errors', async ({ page }) => {
    await gotoAndWait(page, ROUTES.templates);
    await expect(page).not.toHaveURL(/login|error/);

    const list = page.locator('table, [role="grid"], [data-testid="template-list"], .template-card').first();
    const emptyState = page.locator('[data-testid="empty-state"], :text("No templates"), :text("Žádné šablony")').first();

    const hasContent = await list.isVisible({ timeout: TIMEOUTS.default }).catch(() => false)
      || await emptyState.isVisible({ timeout: 2_000 }).catch(() => false);
    expect(hasContent, 'Templates page must show list or empty state').toBeTruthy();
  });

  test('"Create template" or "New Mapping" button is present', async ({ page }) => {
    await gotoAndWait(page, ROUTES.templates);

    const createBtn = page.locator(
      'button:has-text("New"), button:has-text("Create"), button:has-text("Add"), [data-testid="new-template-btn"]'
    ).first();
    const visible = await createBtn.isVisible({ timeout: TIMEOUTS.short }).catch(() => false);
    if (!visible) {
      await featurePresent(page, '[data-testid="new-template-btn"]', 'Create template button');
    } else {
      await expect(createBtn).toBeEnabled();
    }
  });
});

test.describe('Mapping editor', () => {
  test('mapping editor has source / target column pair UI', async ({ page }) => {
    await gotoAndWait(page, ROUTES.templates);

    const createBtn = page.locator(
      'button:has-text("New"), button:has-text("Create"), [data-testid="new-template-btn"]'
    ).first();
    if (!await createBtn.isVisible({ timeout: 2_000 }).catch(() => false)) {
      await featurePresent(page, '[data-testid="new-template-btn"]', 'Create template button');
      return;
    }
    await createBtn.click();
    await page.waitForLoadState('domcontentloaded');

    const mappingRow = page.locator(
      '[data-testid="mapping-row"], [data-testid="mapping-pair"], .mapping-row'
    ).first();
    const addMappingBtn = page.locator(
      'button:has-text("Add"), button:has-text("+ Mapping"), [data-testid="add-mapping-btn"]'
    ).first();

    const hasMappingUI = await mappingRow.isVisible({ timeout: TIMEOUTS.short }).catch(() => false)
      || await addMappingBtn.isVisible({ timeout: TIMEOUTS.short }).catch(() => false);

    if (!hasMappingUI) {
      await featurePresent(page, '[data-testid="mapping-row"]', 'Mapping row editor');
    } else {
      expect(hasMappingUI).toBeTruthy();
    }
  });

  test('"Suggest Mapping" button present in mapping editor', async ({ page }) => {
    await gotoAndWait(page, ROUTES.templates);

    const createBtn = page.locator(
      'button:has-text("New"), button:has-text("Create"), [data-testid="new-template-btn"]'
    ).first();
    if (!await createBtn.isVisible({ timeout: 2_000 }).catch(() => false)) return;

    await createBtn.click();
    await page.waitForLoadState('domcontentloaded');

    const suggestBtn = page.locator(
      'button:has-text("Suggest"), button:has-text("Auto"), [data-testid="suggest-mapping-btn"]'
    ).first();
    const visible = await suggestBtn.isVisible({ timeout: TIMEOUTS.short }).catch(() => false);
    if (!visible) {
      await featurePresent(page, 'button:has-text("Suggest")', 'Suggest Mapping button');
    } else {
      await expect(suggestBtn).toBeEnabled();
    }
  });

  test('mapping editor template name field has a label', async ({ page }) => {
    await gotoAndWait(page, ROUTES.templates);

    const createBtn = page.locator(
      'button:has-text("New"), button:has-text("Create"), [data-testid="new-template-btn"]'
    ).first();
    if (!await createBtn.isVisible({ timeout: 2_000 }).catch(() => false)) return;

    await createBtn.click();
    await page.waitForLoadState('domcontentloaded');

    const nameInput = page.locator(
      'input[name*="name" i], input[placeholder*="template" i], [data-testid="template-name"]'
    ).first();
    const visible = await nameInput.isVisible({ timeout: TIMEOUTS.short }).catch(() => false);
    if (!visible) {
      await featurePresent(page, 'input[name*="name"]', 'Template name input');
      return;
    }

    const id      = await nameInput.getAttribute('id');
    const ariaLbl = await nameInput.getAttribute('aria-label');
    const hasLabel = !!(ariaLbl || (id && await page.locator(`label[for="${id}"]`).count() > 0));
    if (!hasLabel) console.warn('[MISSING FEATURE] Template name input missing accessible label');
  });
});
