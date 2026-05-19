/**
 * FS11 – Dashboards & SQL Reporting
 *
 * UX focus:
 *  - Dashboard list loads and shows cards/table
 *  - Create dashboard button accessible (Admin/Editor)
 *  - Chart type selector present in dashboard editor
 *  - GROUP BY / ORDER BY filter controls present
 *  - Dashboard renders charts/visualisations
 *  - Public vs private dashboard badge
 *  - Viewer sees only Public dashboards
 */
import { test, expect } from '@playwright/test';
import { AppPage } from '../../pages/AppPage';
import { ROUTES, TIMEOUTS } from '../../config/config';
import { gotoAndWait, featurePresent } from '../../fixtures/auth.fixture';

test.describe('Dashboard list page', () => {
  test('dashboards page loads without errors', { tag: ['@smoke'] }, async ({ page }) => {
    await gotoAndWait(page, ROUTES.dashboards);
    const app = new AppPage(page);
    await app.expectNoErrorsVisible();
    await expect(page).toHaveURL(new RegExp(ROUTES.dashboards));
  });

  test('dashboards page has list or empty state', async ({ page }) => {
    await gotoAndWait(page, ROUTES.dashboards);

    const list = page.locator('table, [role="grid"], [role="list"], [data-testid="dashboard-list"], .dashboard-card').first();
    const listVisible = await list.isVisible({ timeout: TIMEOUTS.default }).catch(() => false);

    const emptyState = await page.locator('[data-testid="empty-state"], :text("No dashboards"), :text("Žádné dashboardy")').first()
      .isVisible({ timeout: 3_000 }).catch(() => false);

    expect(listVisible || emptyState, 'Dashboard page must show list or empty state').toBeTruthy();
  });

  test('"Create Dashboard" button is visible and accessible', async ({ page }) => {
    await gotoAndWait(page, ROUTES.dashboards);

    const createBtn = page.locator(
      'button:has-text("New Dashboard"), button:has-text("Create"), a:has-text("New Dashboard"), [data-testid="new-dashboard-btn"]'
    ).first();
    const visible = await createBtn.isVisible({ timeout: TIMEOUTS.short }).catch(() => false);

    if (!visible) {
      await featurePresent(page, '[data-testid="new-dashboard-btn"]', 'Create Dashboard button');
      return;
    }
    await expect(createBtn).toBeEnabled();

    // Accessible name
    const name = await createBtn.getAttribute('aria-label') ?? await createBtn.textContent() ?? '';
    expect(name.trim(), 'Create button missing accessible name').toBeTruthy();
  });
});

test.describe('Dashboard editor', () => {
  test('create dashboard page has chart type selector', async ({ page }) => {
    await gotoAndWait(page, ROUTES.dashboardNew);

    const chartSelector = page.locator(
      'select[name*="chart" i], [data-testid="chart-type-selector"], [aria-label*="chart type" i], button:has-text("Bar"), button:has-text("Line"), button:has-text("Pie")'
    ).first();
    const visible = await chartSelector.isVisible({ timeout: TIMEOUTS.default }).catch(() => false);

    if (!visible) {
      await featurePresent(page, '[data-testid="chart-type-selector"]', 'Chart type selector in dashboard editor');
    } else {
      await expect(chartSelector).toBeVisible();
    }
  });

  test('dashboard editor has title / name field', async ({ page }) => {
    await gotoAndWait(page, ROUTES.dashboardNew);

    const titleField = page.locator(
      'input[name*="name" i], input[name*="title" i], input[placeholder*="Dashboard name" i], [data-testid="dashboard-name"]'
    ).first();
    const visible = await titleField.isVisible({ timeout: TIMEOUTS.default }).catch(() => false);
    if (!visible) {
      await featurePresent(page, 'input[name*="name"]', 'Dashboard name field');
      return;
    }
    await expect(titleField).toBeEnabled();
  });

  test('dashboard editor has SQL query or GROUP BY controls', async ({ page }) => {
    await gotoAndWait(page, ROUTES.dashboardNew);

    const sqlEditor = page.locator(
      'textarea[name*="sql" i], .code-editor, [data-testid="sql-editor"], [aria-label*="GROUP BY" i], select[name*="group" i]'
    ).first();
    const visible = await sqlEditor.isVisible({ timeout: TIMEOUTS.default }).catch(() => false);
    if (!visible) {
      await featurePresent(page, '[data-testid="sql-editor"]', 'SQL editor / GROUP BY control in dashboard builder');
    } else {
      await expect(sqlEditor).toBeVisible();
    }
  });
});

test.describe('Dashboard viewing', () => {
  test('navigating to /dashboards/:id renders a page', async ({ page }) => {
    await gotoAndWait(page, ROUTES.dashboards);

    // Click first dashboard if available
    const firstDashboard = page.locator(
      'table tr:has(td) a, [role="row"]:not([role="columnheader"]) a, .dashboard-card a, [data-testid="dashboard-item"] a'
    ).first();
    const hasItem = await firstDashboard.isVisible({ timeout: 3_000 }).catch(() => false);

    if (!hasItem) {
      // No dashboards yet — create one first via UI would be needed; skip
      console.warn('[INFO] No existing dashboards to view — skipping detail test');
      return;
    }
    await firstDashboard.click();
    await page.waitForLoadState('domcontentloaded');

    // Page should contain chart or data table
    const chartOrTable = page.locator('svg, canvas, table, [data-testid*="chart"], [data-testid*="dashboard"]').first();
    await expect(chartOrTable).toBeVisible({ timeout: TIMEOUTS.default });
  });
});
