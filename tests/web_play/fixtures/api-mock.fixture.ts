/**
 * API mock fixture — intercepts backend REST calls with minimal stub responses.
 *
 * Usage:
 *   import { withApiMocks } from '../fixtures/api-mock.fixture';
 *
 *   test('page renders offline', async ({ page }) => {
 *     await withApiMocks(page);
 *     await page.goto('/reports');
 *     // page renders app shell even without a running backend
 *   });
 *
 * The stubs return the smallest valid shape so the React app can render
 * without crashing on null-access. Any real test that needs specific data
 * should NOT use this fixture — let the real backend respond.
 *
 * Intercept strategy: match /api/** and /v1/** prefix patterns used by the
 * RA Spring Boot services. Routes that return lists default to []; routes
 * that return single objects default to a minimal {}. 404s are passed
 * through so missing-feature detection still works.
 */
import type { Page } from '@playwright/test';

type MockRouteEntry = {
  pattern: RegExp;
  body: unknown;
  status?: number;
  contentType?: string;
};

const LIST_ENDPOINTS: RegExp[] = [
  /\/api\/reports/,
  /\/api\/periods/,
  /\/api\/forms/,
  /\/api\/dashboards/,
  /\/api\/files/,
  /\/api\/sinks/,
  /\/api\/notifications/,
  /\/api\/audit/,
  /\/api\/templates/,
  /\/api\/export-flows/,
  /\/api\/named-queries/,
  /\/api\/text-templates/,
  /\/api\/projects/,
  /\/api\/integrations/,
  /\/api\/promotions/,
  /\/api\/batches/,
  /\/v1\/reports/,
  /\/v1\/periods/,
  /\/v1\/forms/,
];

const OBJECT_ENDPOINTS: RegExp[] = [
  /\/api\/health/,
  /\/api\/admin/,
  /\/v1\/health/,
];

/** Install API mocks on the given page for UX smoke tests. */
export async function withApiMocks(page: Page): Promise<void> {
  await page.route('**/{api,v1}/**', async (route) => {
    const url = route.request().url();

    // Pass through non-GET requests so write operations are not silently swallowed
    if (route.request().method() !== 'GET') {
      await route.continue();
      return;
    }

    // Check list endpoints first
    for (const pattern of LIST_ENDPOINTS) {
      if (pattern.test(url)) {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ data: [], total: 0, page: 0, size: 20 }),
        });
        return;
      }
    }

    // Object endpoints
    for (const pattern of OBJECT_ENDPOINTS) {
      if (pattern.test(url)) {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ status: 'ok' }),
        });
        return;
      }
    }

    // All other API calls: pass through (real backend or 404)
    await route.continue();
  });
}

/**
 * Remove all API mocks installed by withApiMocks.
 * Call in afterEach if test file mixes mocked and unmocked tests.
 */
export async function clearApiMocks(page: Page): Promise<void> {
  await page.unrouteAll({ behavior: 'wait' });
}
