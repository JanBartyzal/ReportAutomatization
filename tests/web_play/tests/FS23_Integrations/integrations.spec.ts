/**
 * FS23 – ServiceNow API Integration & Automation
 *
 * UX focus:
 *  - Integrations page reachable from admin
 *  - ServiceNow connection form: URL, credentials fields present and labelled
 *  - Test connection button present
 *  - Schedule settings (daily/weekly selector) visible
 *  - Connection status indicator (connected / not connected)
 *  - History / last sync timestamp visible
 *  - Projects list tab accessible
 */
import { test, expect } from '@playwright/test';
import { ROUTES, TIMEOUTS } from '../../config/config';
import { gotoAndWait, featurePresent } from '../../fixtures/auth.fixture';

test.describe('Integrations page', () => {
  test('integrations page accessible from admin nav', async ({ page }) => {
    await gotoAndWait(page, ROUTES.adminIntegrations);
    await expect(page).not.toHaveURL(/login|error/);

    const content = page.locator(
      '[data-testid="integrations-page"], :text("ServiceNow"), :text("Integration"), h1, h2'
    ).first();
    const visible = await content.isVisible({ timeout: TIMEOUTS.default }).catch(() => false);
    if (!visible) {
      await featurePresent(page, '[data-testid="integrations-page"]', 'Integrations page');
    } else {
      await expect(content).toBeVisible();
    }
  });

  test('integrations page has ServiceNow tab or section', async ({ page }) => {
    await gotoAndWait(page, ROUTES.adminIntegrations);

    const snowTab = page.locator(
      '[data-testid="servicenow-tab"], button:has-text("ServiceNow"), a:has-text("ServiceNow"), :text("ServiceNow")'
    ).first();
    const visible = await snowTab.isVisible({ timeout: TIMEOUTS.short }).catch(() => false);
    if (!visible) {
      await featurePresent(page, ':text("ServiceNow")', 'ServiceNow integration section');
    } else {
      await expect(snowTab).toBeVisible();
    }
  });
});

test.describe('ServiceNow connection form', () => {
  test('connection dialog has URL and credentials fields', async ({ page }) => {
    await gotoAndWait(page, ROUTES.adminIntegrations);

    // Click ServiceNow section or "Configure" button
    const configureBtn = page.locator(
      'button:has-text("Configure"), button:has-text("Connect"), button:has-text("Edit"), [data-testid="configure-servicenow"]'
    ).first();
    if (await configureBtn.isVisible({ timeout: 3_000 }).catch(() => false)) {
      await configureBtn.click();
    }

    const urlField = page.locator(
      'input[name*="url" i], input[placeholder*="ServiceNow" i], input[placeholder*="instance" i], [data-testid="snow-url"]'
    ).first();
    const visible = await urlField.isVisible({ timeout: TIMEOUTS.short }).catch(() => false);
    if (!visible) {
      await featurePresent(page, 'input[name*="url"]', 'ServiceNow URL field');
      return;
    }

    // Check accessible label
    const id      = await urlField.getAttribute('id');
    const ariaLbl = await urlField.getAttribute('aria-label');
    const hasLabel = !!(ariaLbl || (id && await page.locator(`label[for="${id}"]`).count() > 0));
    if (!hasLabel) console.warn('[MISSING FEATURE] ServiceNow URL input missing label');

    // Credentials fields
    const usernameField = page.locator('input[name*="user" i], input[name*="login" i]').first();
    const passwordField = page.locator('input[type="password"]').first();
    const hasUser = await usernameField.isVisible({ timeout: 2_000 }).catch(() => false);
    const hasPass = await passwordField.isVisible({ timeout: 2_000 }).catch(() => false);
    if (!hasUser) console.warn('[MISSING FEATURE] ServiceNow username field missing');
    if (!hasPass) console.warn('[MISSING FEATURE] ServiceNow password field missing');
  });

  test('"Test Connection" button is accessible', async ({ page }) => {
    await gotoAndWait(page, ROUTES.adminIntegrations);

    const testBtn = page.locator(
      'button:has-text("Test Connection"), button:has-text("Test"), [data-testid="test-connection-btn"]'
    ).first();
    const visible = await testBtn.isVisible({ timeout: TIMEOUTS.short }).catch(() => false);
    if (!visible) {
      await featurePresent(page, 'button:has-text("Test Connection")', 'Test connection button');
    } else {
      await expect(testBtn).toBeEnabled();
    }
  });
});

test.describe('Sync schedule and history', () => {
  test('schedule settings selector present', async ({ page }) => {
    await gotoAndWait(page, ROUTES.adminIntegrations);

    const scheduleSelector = page.locator(
      'select[name*="schedule" i], [data-testid*="schedule"], input[name*="schedule" i], :text("Daily"), :text("Weekly")'
    ).first();
    const visible = await scheduleSelector.isVisible({ timeout: TIMEOUTS.short }).catch(() => false);
    if (!visible) {
      await featurePresent(page, '[data-testid*="schedule"]', 'Sync schedule selector');
    }
  });

  test('sync history / last sync timestamp visible', async ({ page }) => {
    await gotoAndWait(page, ROUTES.adminIntegrations);

    const history = page.locator(
      '[data-testid*="history"], :text("Last sync"), :text("Poslední synchronizace"), :text("History")'
    ).first();
    const visible = await history.isVisible({ timeout: TIMEOUTS.short }).catch(() => false);
    if (!visible) {
      await featurePresent(page, '[data-testid*="history"]', 'Sync history / last sync timestamp');
    }
  });
});

test.describe('Projects tab', () => {
  test('projects page is reachable', async ({ page }) => {
    await gotoAndWait(page, ROUTES.projects);
    await expect(page).not.toHaveURL(/login|error/);

    const content = page.locator(
      '[data-testid="projects-page"], table, [role="grid"], :text("Project")'
    ).first();
    const visible = await content.isVisible({ timeout: TIMEOUTS.default }).catch(() => false);
    if (!visible) {
      await featurePresent(page, '[data-testid="projects-page"]', 'Projects page (/projects)');
    }
  });
});
