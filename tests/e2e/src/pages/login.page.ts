import { type Page, type Locator, expect } from '@playwright/test';

/**
 * Page Object for the Login page.
 *
 * Encapsulates selectors and actions related to the unauthenticated login view
 * rendered by `LoginPage.tsx` via the MSAL `<UnauthenticatedTemplate>`.
 */
export class LoginPage {
    readonly page: Page;
    readonly heading: Locator;
    readonly subtitle: Locator;
    readonly signInButton: Locator;

    constructor(page: Page) {
        this.page = page;
        this.heading = page.getByRole('heading', { name: 'Welcome' });
        this.subtitle = page.getByText('Sign in with your Microsoft account to continue');
        this.signInButton = page.getByRole('button', { name: 'Sign in with Microsoft' });
    }

    /** Navigate directly to the login page. */
    async navigate(): Promise<void> {
        await this.page.goto('/login');
        await this.page.waitForLoadState('networkidle');
    }

    /** Assert the login page is visible with all expected elements. */
    async expectLoginPage(): Promise<void> {
        await expect(this.heading).toBeVisible();
        await expect(this.subtitle).toBeVisible();
        await expect(this.signInButton).toBeVisible();
    }

    /**
     * Mock a successful MSAL login by injecting session storage entries and
     * reloading the page. This simulates what `auth.setup.ts` does but can
     * be called inline within a single test when needed.
     */
    async mockLogin(): Promise<void> {
        const clientId = process.env.VITE_AZURE_CLIENT_ID || '00000000-0000-0000-0000-000000000000';
        const tenantId = 'test-tenant-001';
        const homeAccountId = 'test-home-account-001.test-tenant-001';
        const environment = 'login.microsoftonline.com';
        const accountKey = `${homeAccountId}-${environment}-${tenantId}`;
        const now = Math.floor(Date.now() / 1000);
        const expiresOn = now + 3600;

        await this.page.evaluate(
            ({ accountKey, homeAccountId, environment, tenantId, clientId, now, expiresOn }) => {
                const accountEntity = {
                    homeAccountId,
                    environment,
                    tenantId,
                    username: 'test@reportplatform.local',
                    localAccountId: 'test-local-account-001',
                    name: 'Test User',
                    authorityType: 'MSSTS',
                    clientInfo: '',
                    realm: tenantId,
                };

                const accessTokenKey = [homeAccountId, environment, 'accesstoken', clientId, tenantId, 'user.read openid profile'].join('-');
                const idTokenKey = [homeAccountId, environment, 'idtoken', clientId, tenantId, ''].join('-');

                sessionStorage.setItem('msal.account.keys', JSON.stringify([accountKey]));
                sessionStorage.setItem(accountKey, JSON.stringify(accountEntity));
                sessionStorage.setItem(
                    `msal.token.keys.${clientId}`,
                    JSON.stringify({ accessToken: [accessTokenKey], idToken: [idTokenKey], refreshToken: [] }),
                );
                sessionStorage.setItem(
                    accessTokenKey,
                    JSON.stringify({
                        homeAccountId,
                        environment,
                        credentialType: 'AccessToken',
                        clientId,
                        secret: 'mock-access-token-for-e2e-tests',
                        realm: tenantId,
                        target: 'user.read openid profile',
                        cachedAt: String(now),
                        expiresOn: String(expiresOn),
                        extendedExpiresOn: String(expiresOn + 3600),
                        tokenType: 'Bearer',
                    }),
                );
                sessionStorage.setItem(
                    idTokenKey,
                    JSON.stringify({
                        homeAccountId,
                        environment,
                        credentialType: 'IdToken',
                        clientId,
                        secret: 'eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0.eyJuYW1lIjoiVGVzdCBVc2VyIn0.mock',
                        realm: tenantId,
                    }),
                );
                sessionStorage.setItem(`msal.${clientId}.active-account`, homeAccountId);
            },
            { accountKey, homeAccountId, environment, tenantId, clientId, now, expiresOn },
        );

        await this.page.reload();
        await this.page.waitForLoadState('networkidle');
    }
}
