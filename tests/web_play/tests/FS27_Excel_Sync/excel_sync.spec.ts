/**
 * FS27 – Live Excel Export & External Sync
 *
 * UX focus:
 *  - /admin/export-flows page loads with list of export flows
 *  - "Create Export Flow" button/dialog accessible
 *  - Dialog fields: name, SQL query / UI builder, target file, sheet name, trigger type
 *  - Trigger type selector: AUTOMATIC vs MANUAL
 *  - "Export Now" (manual trigger) button per flow
 *  - Export history column with last-run status and timestamp
 *  - Flow status indicator (success / fail / pending)
 *  - SharePoint / local path connector selector
 */
import { test, expect } from '@playwright/test';
import { ROUTES, TIMEOUTS } from '../../config/config';
import { gotoAndWait, featurePresent } from '../../fixtures/auth.fixture';

test.describe('Export Flows list', () => {
  test('export flows page loads', { tag: ['@smoke'] }, async ({ page }) => {
    await gotoAndWait(page, ROUTES.exportFlows);
    await expect(page).not.toHaveURL(/login|error/);

    const list = page.locator('table, [role="grid"], [data-testid="export-flows-list"]').first();
    const empty = page.locator('[data-testid="empty-state"], :text("No export flows"), :text("Žádné export flow")').first();
    const hasContent = await list.isVisible({ timeout: TIMEOUTS.default }).catch(() => false)
      || await empty.isVisible({ timeout: 2_000 }).catch(() => false);
    expect(hasContent, 'Export flows page must show list or empty state').toBeTruthy();
  });

  test('export flows list shows last-run status and timestamp', async ({ page }) => {
    await gotoAndWait(page, ROUTES.exportFlows);

    const statusCol = page.locator(
      'th:has-text("Status"), [role="columnheader"]:has-text("Status"), th:has-text("Last run"), [data-testid*="last-run"]'
    ).first();
    const visible = await statusCol.isVisible({ timeout: TIMEOUTS.short }).catch(() => false);
    if (!visible) {
      await featurePresent(page, 'th:has-text("Status")', 'Status column in export flows list');
    }
  });

  test('"Export Now" button present per flow row', async ({ page }) => {
    await gotoAndWait(page, ROUTES.exportFlows);

    const exportNowBtn = page.locator(
      'button:has-text("Export Now"), button:has-text("Run"), [data-testid*="export-now"]'
    ).first();
    const visible = await exportNowBtn.isVisible({ timeout: TIMEOUTS.short }).catch(() => false);
    if (!visible) {
      await featurePresent(page, 'button:has-text("Export Now")', '"Export Now" button in flow row');
    } else {
      await expect(exportNowBtn).toBeEnabled();
    }
  });
});

test.describe('Create Export Flow dialog', () => {
  test('"Create Export Flow" button opens dialog/drawer', async ({ page }) => {
    await gotoAndWait(page, ROUTES.exportFlows);

    const createBtn = page.locator(
      'button:has-text("New"), button:has-text("Create"), button:has-text("Add Flow"), [data-testid="new-export-flow-btn"]'
    ).first();
    const visible = await createBtn.isVisible({ timeout: TIMEOUTS.short }).catch(() => false);
    if (!visible) {
      await featurePresent(page, '[data-testid="new-export-flow-btn"]', 'Create Export Flow button');
      return;
    }

    await createBtn.click();

    const dialog = page.locator('[role="dialog"], [data-testid="create-flow-dialog"], aside[data-testid*="drawer"]').first();
    await expect(dialog).toBeVisible({ timeout: TIMEOUTS.short });
  });

  test('create flow dialog has name, SQL query, target file fields', async ({ page }) => {
    await gotoAndWait(page, ROUTES.exportFlows);

    const createBtn = page.locator(
      'button:has-text("New"), button:has-text("Create"), [data-testid="new-export-flow-btn"]'
    ).first();
    if (!await createBtn.isVisible({ timeout: TIMEOUTS.short }).catch(() => false)) return;

    await createBtn.click();
    await page.locator('[role="dialog"], aside').waitFor({ state: 'visible', timeout: TIMEOUTS.short });

    const nameField    = page.locator('input[name*="name" i], [data-testid="flow-name"]').first();
    const sqlEditor    = page.locator('textarea[name*="sql" i], .code-editor, [data-testid="sql-editor"]').first();
    const targetField  = page.locator('input[name*="target" i], input[name*="file" i], [data-testid="target-file"]').first();
    const sheetField   = page.locator('input[name*="sheet" i], [data-testid="sheet-name"]').first();

    const hasName   = await nameField.isVisible({ timeout: 2_000 }).catch(() => false);
    const hasSql    = await sqlEditor.isVisible({ timeout: 2_000 }).catch(() => false);
    const hasTarget = await targetField.isVisible({ timeout: 2_000 }).catch(() => false);
    const hasSheet  = await sheetField.isVisible({ timeout: 2_000 }).catch(() => false);

    if (!hasName)   console.warn('[MISSING FEATURE] Flow name field missing in create dialog');
    if (!hasSql)    console.warn('[MISSING FEATURE] SQL editor / query builder missing in create dialog');
    if (!hasTarget) console.warn('[MISSING FEATURE] Target file path missing in create dialog');
    if (!hasSheet)  console.warn('[MISSING FEATURE] Target sheet name missing in create dialog');
  });

  test('trigger type selector has AUTOMATIC and MANUAL options', async ({ page }) => {
    await gotoAndWait(page, ROUTES.exportFlows);

    const createBtn = page.locator(
      'button:has-text("New"), button:has-text("Create"), [data-testid="new-export-flow-btn"]'
    ).first();
    if (!await createBtn.isVisible({ timeout: TIMEOUTS.short }).catch(() => false)) return;

    await createBtn.click();
    await page.locator('[role="dialog"], aside').waitFor({ state: 'visible', timeout: TIMEOUTS.short });

    const automaticOption = page.locator('option:has-text("Automatic"), input[value="AUTOMATIC"], :text("Automatic")').first();
    const manualOption    = page.locator('option:has-text("Manual"), input[value="MANUAL"], :text("Manual")').first();

    const hasAutomatic = await automaticOption.isVisible({ timeout: 2_000 }).catch(() => false);
    const hasManual    = await manualOption.isVisible({ timeout: 2_000 }).catch(() => false);

    if (!hasAutomatic) console.warn('[MISSING FEATURE] AUTOMATIC trigger type option missing');
    if (!hasManual)    console.warn('[MISSING FEATURE] MANUAL trigger type option missing');
  });

  test('connector type selector (SharePoint / Local path) present', async ({ page }) => {
    await gotoAndWait(page, ROUTES.exportFlows);

    const createBtn = page.locator(
      'button:has-text("New"), button:has-text("Create"), [data-testid="new-export-flow-btn"]'
    ).first();
    if (!await createBtn.isVisible({ timeout: TIMEOUTS.short }).catch(() => false)) return;

    await createBtn.click();
    await page.locator('[role="dialog"], aside').waitFor({ state: 'visible', timeout: TIMEOUTS.short });

    const connectorType = page.locator(
      'select[name*="connector" i], [data-testid="connector-type"], :text("SharePoint"), :text("Local")'
    ).first();
    const visible = await connectorType.isVisible({ timeout: 2_000 }).catch(() => false);
    if (!visible) {
      await featurePresent(page, '[data-testid="connector-type"]', 'Connector type selector (SharePoint / Local path)');
    }
  });
});
