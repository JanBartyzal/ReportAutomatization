import { test as base, expect, type Page, type APIRequestContext } from '@playwright/test';

/**
 * Custom test fixtures extending Playwright's base test.
 *
 * - `authenticatedPage`: a Page that already has the MSAL storageState applied
 *   (handled via project config dependency on the setup project).
 * - `apiContext`: an APIRequestContext pre-configured with the base URL and a
 *   mock Bearer token for making direct backend calls in tests.
 * - Helper utilities for generating unique test data.
 */

// ── Types ──────────────────────────────────────────────────────────────────

interface TestFixtures {
    /** Page with authentication state already loaded. */
    authenticatedPage: Page;
    /** API context for direct backend REST calls. */
    apiContext: APIRequestContext;
}

// ── Fixtures ───────────────────────────────────────────────────────────────

export const test = base.extend<TestFixtures>({
    authenticatedPage: async ({ page }, use) => {
        // The storageState is already injected by the project config (depends on setup).
        // Navigate to the root so the app initialises with the cached auth.
        await page.goto('/');
        await page.waitForLoadState('networkidle');
        await use(page);
    },

    apiContext: async ({ playwright }, use) => {
        const baseURL = process.env.BASE_URL || 'http://localhost';
        const context = await playwright.request.newContext({
            baseURL,
            extraHTTPHeaders: {
                Authorization: 'Bearer mock-access-token-for-e2e-tests',
                'Content-Type': 'application/json',
            },
        });
        await use(context);
        await context.dispose();
    },
});

export { expect };

// ── Test Data Helpers ──────────────────────────────────────────────────────

/** Generate a unique file name with a timestamp suffix. */
export function uniqueFileName(base: string, extension: string): string {
    const ts = Date.now();
    const rand = Math.random().toString(36).substring(2, 8);
    return `${base}-${ts}-${rand}.${extension}`;
}

/** Generate a unique dashboard name. */
export function uniqueDashboardName(prefix = 'E2E Dashboard'): string {
    return `${prefix} ${new Date().toISOString().slice(0, 19).replace('T', ' ')}`;
}

/** Generate a unique report identifier tag. */
export function uniqueReportTag(prefix = 'e2e-report'): string {
    return `${prefix}-${Date.now()}`;
}

/**
 * Wait for a specific API response matching a URL pattern.
 * Useful for asserting that a mutation or fetch completed successfully.
 */
export async function waitForApiResponse(
    page: Page,
    urlPattern: string | RegExp,
    statusCode = 200,
): Promise<void> {
    await page.waitForResponse(
        (response) =>
            (typeof urlPattern === 'string'
                ? response.url().includes(urlPattern)
                : urlPattern.test(response.url())) &&
            response.status() === statusCode,
    );
}
