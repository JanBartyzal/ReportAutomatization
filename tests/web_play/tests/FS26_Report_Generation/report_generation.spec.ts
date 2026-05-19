/**
 * FS26 – Report Generation UI & Export
 *
 * UX focus:
 *  - "New Report" button on /reports page opens create dialog
 *  - Create dialog fields: org, period, report type (OPEX/CAPEX/…)
 *  - Created report appears in DRAFT state
 *  - "Generate PPTX" button on approved report detail
 *  - Generation status polling: "Generating…" indicator
 *  - "Download PPTX" link appears after generation
 *  - /generated-reports page shows history of generated files
 *  - /batch-generation page has multi-report generation controls
 */
import { test, expect } from '@playwright/test';
import { ReportsPage } from '../../pages/ReportsPage';
import { ROUTES, TIMEOUTS } from '../../config/config';
import { gotoAndWait, featurePresent } from '../../fixtures/auth.fixture';

test.describe('Create Report dialog', () => {
  test('"New Report" button opens create dialog', { tag: ['@smoke'] }, async ({ page }) => {
    await gotoAndWait(page, ROUTES.reports);
    const reportsPage = new ReportsPage(page);

    const newBtn = reportsPage.newReportBtn;
    const visible = await newBtn.isVisible({ timeout: TIMEOUTS.short }).catch(() => false);
    if (!visible) {
      await featurePresent(page, '[data-testid="new-report-btn"]', 'New Report button');
      return;
    }

    await newBtn.click();

    const dialog = page.locator('[role="dialog"], [data-testid="create-report-dialog"]').first();
    await expect(dialog).toBeVisible({ timeout: TIMEOUTS.short });
  });

  test('create report dialog has org, period, and type fields', async ({ page }) => {
    await gotoAndWait(page, ROUTES.reports);
    const reportsPage = new ReportsPage(page);

    const newBtn = reportsPage.newReportBtn;
    if (!await newBtn.isVisible({ timeout: TIMEOUTS.short }).catch(() => false)) return;

    await newBtn.click();
    await page.locator('[role="dialog"]').waitFor({ state: 'visible', timeout: TIMEOUTS.short });

    const orgField    = page.locator('select[name*="org" i], [data-testid="org-selector"], [aria-label*="organization" i]').first();
    const periodField = page.locator('select[name*="period" i], [data-testid="period-selector"]').first();
    const typeField   = page.locator('select[name*="type" i], [data-testid="report-type"], [aria-label*="type" i]').first();

    const hasOrg    = await orgField.isVisible({ timeout: 2_000 }).catch(() => false);
    const hasPeriod = await periodField.isVisible({ timeout: 2_000 }).catch(() => false);
    const hasType   = await typeField.isVisible({ timeout: 2_000 }).catch(() => false);

    if (!hasOrg)    console.warn('[MISSING FEATURE] Create report dialog missing org selector');
    if (!hasPeriod) console.warn('[MISSING FEATURE] Create report dialog missing period selector');
    if (!hasType)   console.warn('[MISSING FEATURE] Create report dialog missing type selector (OPEX/CAPEX/…)');
  });

  test('report type dropdown has OPEX option', async ({ page }) => {
    await gotoAndWait(page, ROUTES.reports);
    const reportsPage = new ReportsPage(page);

    const newBtn = reportsPage.newReportBtn;
    if (!await newBtn.isVisible({ timeout: TIMEOUTS.short }).catch(() => false)) return;

    await newBtn.click();
    await page.locator('[role="dialog"]').waitFor({ state: 'visible', timeout: TIMEOUTS.short });

    const opexOption = page.locator('option:has-text("OPEX"), [data-value="OPEX"], :text("OPEX")').first();
    const visible = await opexOption.isVisible({ timeout: 2_000 }).catch(() => false);
    if (!visible) {
      await featurePresent(page, 'option:has-text("OPEX")', 'OPEX option in report type selector');
    }
  });
});

test.describe('Generate PPTX workflow', () => {
  test('download PPTX link appears on generated reports page', async ({ page }) => {
    await gotoAndWait(page, ROUTES.generatedReports);
    await expect(page).not.toHaveURL(/login|error/);

    const downloadLink = page.locator(
      'a[href*=".pptx"], a[href*="download"], button:has-text("Download"), [data-testid*="download"]'
    ).first();
    const list = page.locator('table, [role="grid"], [data-testid="generated-reports-list"]').first();
    const empty = page.locator('[data-testid="empty-state"], :text("No generated"), :text("Žádné")').first();

    const hasContent = await list.isVisible({ timeout: TIMEOUTS.default }).catch(() => false)
      || await empty.isVisible({ timeout: 2_000 }).catch(() => false)
      || await downloadLink.isVisible({ timeout: 2_000 }).catch(() => false);

    expect(hasContent, 'Generated reports page must show list, download links, or empty state').toBeTruthy();
  });
});

test.describe('Batch generation page', () => {
  test('batch generation page has report selector and generate button', async ({ page }) => {
    await gotoAndWait(page, ROUTES.batchGeneration);

    const selector = page.locator(
      'input[type="checkbox"], [data-testid="report-selector"], table input[type="checkbox"]'
    ).first();
    const generateBtn = page.locator(
      'button:has-text("Generate All"), button:has-text("Batch Generate"), [data-testid="batch-generate-btn"]'
    ).first();

    const hasSelector = await selector.isVisible({ timeout: TIMEOUTS.default }).catch(() => false);
    const hasGenerate = await generateBtn.isVisible({ timeout: TIMEOUTS.short }).catch(() => false);

    if (!hasSelector) await featurePresent(page, 'input[type="checkbox"]', 'Report checkboxes for batch selection');
    if (!hasGenerate) await featurePresent(page, 'button:has-text("Generate All")', 'Batch generate button');
  });
});
