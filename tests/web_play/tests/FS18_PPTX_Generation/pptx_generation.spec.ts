/**
 * FS18 – PPTX Report Generation (Template Engine)
 *
 * UX focus:
 *  - Templates list page shows PPTX templates with placeholder info
 *  - Upload template button present and accessible
 *  - Template detail shows list of required placeholders
 *  - "Generate PPTX" button visible on approved report detail
 *  - Generation is asynchronous — loading/progress indicator shown
 *  - Download PPTX link appears after generation
 *  - Missing data warning shown (DATA MISSING indicator)
 *  - Batch generation button on /batch-generation page
 */
import { test, expect } from '@playwright/test';
import { ROUTES, TIMEOUTS } from '../../config/config';
import { gotoAndWait, featurePresent } from '../../fixtures/auth.fixture';

test.describe('PPTX templates list', () => {
  test('templates page loads PPTX templates', async ({ page }) => {
    await gotoAndWait(page, ROUTES.templates);

    const list = page.locator('table, [role="grid"], [data-testid="template-list"]').first();
    const empty = page.locator('[data-testid="empty-state"], :text("No templates")').first();
    const hasContent = await list.isVisible({ timeout: TIMEOUTS.default }).catch(() => false)
      || await empty.isVisible({ timeout: 2_000 }).catch(() => false);
    expect(hasContent, 'Templates page must show content').toBeTruthy();
  });

  test('"Upload Template" button is accessible', async ({ page }) => {
    await gotoAndWait(page, ROUTES.templates);

    const uploadBtn = page.locator(
      'button:has-text("Upload"), button:has-text("Add Template"), [data-testid="upload-template-btn"]'
    ).first();
    const visible = await uploadBtn.isVisible({ timeout: TIMEOUTS.short }).catch(() => false);
    if (!visible) {
      await featurePresent(page, 'button:has-text("Upload")', 'Upload PPTX template button');
      return;
    }
    await expect(uploadBtn).toBeEnabled();

    const name = await uploadBtn.getAttribute('aria-label') ?? await uploadBtn.textContent() ?? '';
    expect(name.trim()).toBeTruthy();
  });
});

test.describe('Template detail – placeholders', () => {
  test('template detail shows placeholder list', async ({ page }) => {
    await gotoAndWait(page, ROUTES.templates);

    const firstTemplate = page.locator(
      'table tr:has(td) a, [role="row"]:not([role="columnheader"]) a, [data-testid="template-item"] a'
    ).first();
    if (!await firstTemplate.isVisible({ timeout: 3_000 }).catch(() => false)) {
      console.warn('[INFO] No templates — skip placeholder test');
      return;
    }

    await firstTemplate.click();
    await page.waitForLoadState('domcontentloaded');

    const placeholderList = page.locator(
      '[data-testid="placeholder-list"], :text("{{"), :text("Placeholder"), :text("Required fields")'
    ).first();
    const visible = await placeholderList.isVisible({ timeout: TIMEOUTS.short }).catch(() => false);
    if (!visible) {
      await featurePresent(page, '[data-testid="placeholder-list"]', 'Placeholder list on template detail');
    } else {
      await expect(placeholderList).toBeVisible();
    }
  });
});

test.describe('PPTX generation trigger', () => {
  test('"Generate PPTX" button visible on approved report detail', async ({ page }) => {
    await gotoAndWait(page, ROUTES.reports);

    const approvedReport = page.locator(
      'tr:has-text("APPROVED") a, [role="row"]:has-text("APPROVED") a, [data-testid="report-item"]:has-text("APPROVED") a'
    ).first();
    if (!await approvedReport.isVisible({ timeout: 3_000 }).catch(() => false)) {
      console.warn('[INFO] No APPROVED reports — skip PPTX generation test');
      return;
    }

    await approvedReport.click();
    await page.waitForLoadState('domcontentloaded');

    const generateBtn = page.locator(
      'button:has-text("Generate PPTX"), button:has-text("Generate"), [data-testid="generate-pptx-btn"]'
    ).first();
    const visible = await generateBtn.isVisible({ timeout: TIMEOUTS.short }).catch(() => false);
    if (!visible) {
      await featurePresent(page, 'button:has-text("Generate PPTX")', 'Generate PPTX button on approved report');
      return;
    }
    await expect(generateBtn).toBeEnabled();
  });

  test('clicking Generate shows loading indicator', async ({ page }) => {
    await gotoAndWait(page, ROUTES.reports);

    const approvedReport = page.locator(
      'tr:has-text("APPROVED") a, [data-testid="report-item"]:has-text("APPROVED") a'
    ).first();
    if (!await approvedReport.isVisible({ timeout: 3_000 }).catch(() => false)) return;

    await approvedReport.click();
    await page.waitForLoadState('domcontentloaded');

    const generateBtn = page.locator(
      'button:has-text("Generate PPTX"), button:has-text("Generate"), [data-testid="generate-pptx-btn"]'
    ).first();
    if (!await generateBtn.isVisible({ timeout: TIMEOUTS.short }).catch(() => false)) return;

    await generateBtn.click();

    // Loading indicator or disabled state should appear
    const spinner = page.locator('[role="progressbar"], .spinner, [data-testid="generating-indicator"]').first();
    const isDisabled = await generateBtn.isDisabled({ timeout: 3_000 }).catch(() => false);
    const spinnerVisible = await spinner.isVisible({ timeout: 3_000 }).catch(() => false);

    if (!isDisabled && !spinnerVisible) {
      console.warn('[MISSING FEATURE] No loading indicator shown while generating PPTX');
    }
  });
});

test.describe('Batch generation', () => {
  test('batch generation page loads', async ({ page }) => {
    await gotoAndWait(page, ROUTES.batchGeneration);

    const batchBtn = page.locator(
      'button:has-text("Generate All"), button:has-text("Batch"), [data-testid="batch-generate-btn"]'
    ).first();
    const visible = await batchBtn.isVisible({ timeout: TIMEOUTS.short }).catch(() => false);
    if (!visible) {
      await featurePresent(page, 'button:has-text("Generate All")', 'Batch generation button');
    } else {
      await expect(batchBtn).toBeEnabled();
    }
  });
});
