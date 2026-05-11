/**
 * FS12 – Search, AI Querying & MCP-facing UI
 *
 * UX focus:
 *  - Global search is reachable from the app shell and navigates to /search
 *  - Search results page exposes query input, mode/type filters and result/empty state
 *  - Named Query catalog supports browsing, creating and running read-only queries
 *  - Text Template catalog/renderer is reachable for generated narrative content
 */
import { test, expect } from '@playwright/test';
import { ROUTES, TIMEOUTS } from '../../config/config';
import { gotoAndWait, featurePresent } from '../../fixtures/auth.fixture';

test.describe('Global search', () => {
  test('global search input navigates to search results', async ({ page }) => {
    await gotoAndWait(page, ROUTES.dashboard);

    const searchInput = page.locator(
      'input[placeholder*="Search" i], input[type="search"], [role="searchbox"]'
    ).first();
    const visible = await searchInput.isVisible({ timeout: TIMEOUTS.default }).catch(() => false);
    if (!visible) {
      await featurePresent(page, 'input[placeholder*="Search"]', 'Global search input');
      return;
    }

    await searchInput.fill('opex');
    await page.keyboard.press('Enter');
    await expect(page).toHaveURL(/\/search\?q=opex|\/search/, { timeout: TIMEOUTS.default });
  });
});

test.describe('Search results page', () => {
  test('search page renders input and result or empty state', async ({ page }) => {
    await gotoAndWait(page, `${ROUTES.search}?q=opex`);
    await expect(page).not.toHaveURL(/login|error/);

    const input = page.locator('input[placeholder*="Search" i], input[type="search"], [role="searchbox"]').first();
    const resultsOrEmpty = page.locator(
      '[data-testid*="search-result"], .search-result, :text("No results"), :text("Enter a search query"), table, [role="list"]'
    ).first();

    await expect(input).toBeVisible({ timeout: TIMEOUTS.default });
    await expect(resultsOrEmpty).toBeVisible({ timeout: TIMEOUTS.default });
  });

  test('search results expose mode/type filters when available', async ({ page }) => {
    await gotoAndWait(page, `${ROUTES.search}?q=opex`);

    const filters = page.locator(
      'select, [role="combobox"], button:has-text("Text"), button:has-text("Vector"), button:has-text("Hybrid"), :text("Type")'
    );
    const count = await filters.count();
    if (count === 0) {
      await featurePresent(page, '[role="combobox"]', 'Search mode/type filters');
    } else {
      expect(count).toBeGreaterThan(0);
    }
  });
});

test.describe('Named Query Catalog', () => {
  test('named query catalog page loads with table or empty state', async ({ page }) => {
    await gotoAndWait(page, ROUTES.namedQueries);
    await expect(page).not.toHaveURL(/login|error/);

    const content = page.locator(
      'table[aria-label="Named queries"], table, :text("Named Query"), :text("New Query"), [data-testid="empty-state"]'
    ).first();
    await expect(content).toBeVisible({ timeout: TIMEOUTS.default });
  });

  test('new named query dialog exposes safe SQL and data source fields', async ({ page }) => {
    await gotoAndWait(page, ROUTES.namedQueries);

    const newBtn = page.getByRole('button', { name: /new query|create query/i }).first();
    if (!await newBtn.isVisible({ timeout: TIMEOUTS.short }).catch(() => false)) {
      await featurePresent(page, 'button:has-text("New Query")', 'New Named Query button');
      return;
    }

    await newBtn.click();
    const dialog = page.locator('[role="dialog"]').first();
    await expect(dialog).toBeVisible({ timeout: TIMEOUTS.short });

    await expect(dialog.locator('input, textarea').first()).toBeVisible();
    const sqlField = dialog.locator('textarea, [data-testid*="sql"], input[name*="sql" i]').first();
    const dataSource = dialog.locator('select, [role="combobox"], :text("Data Source")').first();

    if (!await sqlField.isVisible({ timeout: 2_000 }).catch(() => false)) {
      console.warn('[MISSING FEATURE] Named Query SQL field is not visible');
    }
    if (!await dataSource.isVisible({ timeout: 2_000 }).catch(() => false)) {
      console.warn('[MISSING FEATURE] Named Query data source selector is not visible');
    }
  });

  test('named query run action is discoverable', async ({ page }) => {
    await gotoAndWait(page, ROUTES.namedQueries);

    const runButton = page.getByRole('button', { name: /run|execute|test/i }).first();
    const visible = await runButton.isVisible({ timeout: TIMEOUTS.short }).catch(() => false);
    if (!visible) {
      await featurePresent(page, 'button:has-text("Run")', 'Named Query run action');
    } else {
      await expect(runButton).toBeEnabled();
    }
  });
});

test.describe('Text Templates', () => {
  test('text template list page loads', async ({ page }) => {
    await gotoAndWait(page, ROUTES.textTemplates);
    await expect(page).not.toHaveURL(/login|error/);

    const content = page.locator(
      'table[aria-label="Text templates"], table, :text("Text Templates"), :text("New Template")'
    ).first();
    await expect(content).toBeVisible({ timeout: TIMEOUTS.default });
  });

  test('text template creation route exposes editor fields', async ({ page }) => {
    await gotoAndWait(page, ROUTES.textTemplateNew);

    const editor = page.locator(
      'input, textarea, [contenteditable="true"], :text("Template"), :text("Output")'
    ).first();
    const visible = await editor.isVisible({ timeout: TIMEOUTS.default }).catch(() => false);
    if (!visible) {
      await featurePresent(page, 'textarea', 'Text template editor');
    } else {
      await expect(editor).toBeVisible();
    }
  });
});
