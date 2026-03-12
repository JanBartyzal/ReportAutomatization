import { test, expect } from '@playwright/test';
import { LoginPage } from '../pages/login.page';

test.describe('Authentication', () => {
    test('should display login page for unauthenticated user', async ({ browser }) => {
        // Create a fresh context with NO stored auth state
        const context = await browser.newContext({ storageState: undefined });
        const page = await context.newPage();
        const loginPage = new LoginPage(page);

        await loginPage.navigate();
        await loginPage.expectLoginPage();

        await context.close();
    });

    test('should redirect to dashboard after successful login', async ({ browser }) => {
        // Start unauthenticated
        const context = await browser.newContext({ storageState: undefined });
        const page = await context.newPage();
        const loginPage = new LoginPage(page);

        await page.goto('/');
        // Unauthenticated users are redirected to /login
        await page.waitForURL('**/login');
        await loginPage.expectLoginPage();

        // Perform mock login (injects MSAL session storage entries)
        await loginPage.mockLogin();

        // After mock login and reload, the app should show the authenticated view
        // and redirect from / to /dashboard
        await page.goto('/');
        await expect(page).toHaveURL(/\/dashboard/);

        await context.close();
    });

    test('should show user info in header after login', async ({ page }) => {
        // This test uses the pre-authenticated storageState from the setup project
        await page.goto('/dashboard');
        await page.waitForLoadState('networkidle');

        // The AppLayout sidebar shows a Persona component with the user's name
        const persona = page.locator('[class*="userSection"]');
        await expect(persona).toBeVisible();

        // The persona should display the test user's name
        await expect(page.getByText('Test User')).toBeVisible();
    });

    test('should handle logout correctly', async ({ page }) => {
        await page.goto('/dashboard');
        await page.waitForLoadState('networkidle');

        // Click the Sign Out button in the sidebar
        const signOutButton = page.getByRole('button', { name: 'Sign Out' });
        await expect(signOutButton).toBeVisible();
        await signOutButton.click();

        // After logout, user should be redirected to the login page
        await expect(page).toHaveURL(/\/login/, { timeout: 10_000 });

        // Verify the login UI is displayed
        await expect(page.getByText('Sign in with Microsoft')).toBeVisible();
    });
});
