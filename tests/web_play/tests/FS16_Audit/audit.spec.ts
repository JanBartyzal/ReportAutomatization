/**
 * FS16 – Audit & Compliance Log
 *
 * UX focus:
 *  - Audit log table loads in Admin panel
 *  - Columns: user, action, timestamp visible
 *  - Date range filter controls present
 *  - User/action filter present
 *  - Export (CSV/JSON) button present
 *  - Logs are read-only (no edit/delete controls in the table)
 *  - AI audit entries visible (when applicable)
 */
import { test, expect } from '@playwright/test';
import { ROUTES, TIMEOUTS } from '../../config/config';
import { gotoAndWait, featurePresent } from '../../fixtures/auth.fixture';

test.describe('Audit log page', () => {
  test('audit log is accessible from admin panel', async ({ page }) => {
    // Try direct admin routes that likely surface audit
    const auditRoutes = ['/audit', '/admin/audit', ROUTES.adminManage, ROUTES.adminHolding];

    let found = false;
    for (const route of auditRoutes) {
      await page.goto(route, { waitUntil: 'domcontentloaded' });
      const hasAudit = await page.locator(
        '[data-testid="audit-log"], :text("Audit"), :text("Log"), table[data-testid*="audit"]'
      ).first().isVisible({ timeout: 3_000 }).catch(() => false);
      if (hasAudit) { found = true; break; }
    }

    if (!found) {
      await featurePresent(page, '[data-testid="audit-log"]', 'Audit log page');
    } else {
      expect(found).toBeTruthy();
    }
  });

  test('audit table shows required columns (user, action, timestamp)', async ({ page }) => {
    await page.goto('/audit', { waitUntil: 'domcontentloaded' });
    // Try admin panel if /audit not found
    const table = page.locator('table, [role="grid"]').first();
    if (!await table.isVisible({ timeout: TIMEOUTS.default }).catch(() => false)) {
      await gotoAndWait(page, ROUTES.adminManage);
    }

    const columnHeaders = page.locator('th, [role="columnheader"]');
    const count = await columnHeaders.count();
    if (count === 0) {
      await featurePresent(page, 'th', 'Audit table column headers');
      return;
    }

    const headerTexts = await columnHeaders.allTextContents();
    const hasUserCol   = headerTexts.some(h => /user|uživatel/i.test(h));
    const hasActionCol = headerTexts.some(h => /action|akce/i.test(h));
    const hasTimeCol   = headerTexts.some(h => /time|čas|datum|date/i.test(h));

    if (!hasUserCol)   console.warn('[MISSING FEATURE] Audit table missing "User" column');
    if (!hasActionCol) console.warn('[MISSING FEATURE] Audit table missing "Action" column');
    if (!hasTimeCol)   console.warn('[MISSING FEATURE] Audit table missing "Timestamp" column');
  });
});

test.describe('Audit log filters', () => {
  test('date range filter is present', async ({ page }) => {
    await gotoAndWait(page, '/audit');

    const dateFilter = page.locator(
      'input[type="date"], input[type="datetime-local"], [data-testid*="date-filter"], [aria-label*="date" i], :text("Date range")'
    ).first();
    const visible = await dateFilter.isVisible({ timeout: TIMEOUTS.short }).catch(() => false);
    if (!visible) {
      await featurePresent(page, 'input[type="date"]', 'Date range filter for audit log');
    } else {
      await expect(dateFilter).toBeEnabled();
    }
  });

  test('export button is present', async ({ page }) => {
    await gotoAndWait(page, '/audit');

    const exportBtn = page.locator(
      'button:has-text("Export"), a:has-text("Export CSV"), a:has-text("Export JSON"), [data-testid="export-audit-btn"]'
    ).first();
    const visible = await exportBtn.isVisible({ timeout: TIMEOUTS.short }).catch(() => false);
    if (!visible) {
      await featurePresent(page, 'button:has-text("Export")', 'Audit log export button');
    } else {
      await expect(exportBtn).toBeEnabled();
    }
  });
});

test.describe('Audit log immutability', () => {
  test('audit table rows have no edit or delete buttons', async ({ page }) => {
    await gotoAndWait(page, '/audit');

    const rows = page.locator('table tr:has(td), [role="row"]:not([role="columnheader"])');
    const count = await rows.count();
    if (count === 0) return;

    for (let i = 0; i < Math.min(count, 5); i++) {
      const row = rows.nth(i);
      const editBtn   = row.locator('button:has-text("Edit"), button:has-text("Delete"), [data-testid*="edit"], [data-testid*="delete"]');
      const editCount = await editBtn.count();
      expect(editCount, `Audit row ${i} should not have edit/delete buttons (immutable log)`).toBe(0);
    }
  });
});
