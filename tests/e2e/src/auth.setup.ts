import { test as setup, expect } from '@playwright/test';
import path from 'path';
import fs from 'fs';

/**
 * Global authentication setup for Playwright E2E tests.
 *
 * Since the app uses MSAL (Azure AD) for auth, we mock the MSAL cache entries
 * in sessionStorage so the MsalProvider considers the user authenticated.
 * This avoids any dependency on a real Azure AD tenant during E2E runs.
 */

const MOCK_USER = {
    name: 'Test User',
    email: 'test@reportplatform.local',
    tenantId: 'test-tenant-001',
    localAccountId: 'test-local-account-001',
    homeAccountId: 'test-home-account-001.test-tenant-001',
    environment: 'login.microsoftonline.com',
    roles: ['Admin', 'User'],
};

const CLIENT_ID = process.env.VITE_AZURE_CLIENT_ID || '00000000-0000-0000-0000-000000000000';
const AUTHORITY = `https://login.microsoftonline.com/${MOCK_USER.tenantId}`;

const AUTH_DIR = path.join(__dirname, '..', '.auth');
const STORAGE_STATE_PATH = path.join(AUTH_DIR, 'user.json');

setup('authenticate', async ({ page }) => {
    // Ensure .auth directory exists
    if (!fs.existsSync(AUTH_DIR)) {
        fs.mkdirSync(AUTH_DIR, { recursive: true });
    }

    // Navigate to the app so we can manipulate sessionStorage on the correct origin
    await page.goto('/');

    // Build the MSAL cache keys and values that MsalProvider expects in sessionStorage
    const accountKey = `${MOCK_USER.homeAccountId}-${MOCK_USER.environment}-${MOCK_USER.tenantId}`;
    const now = Math.floor(Date.now() / 1000);
    const expiresOn = now + 3600; // 1 hour from now

    const accountEntity = {
        homeAccountId: MOCK_USER.homeAccountId,
        environment: MOCK_USER.environment,
        tenantId: MOCK_USER.tenantId,
        username: MOCK_USER.email,
        localAccountId: MOCK_USER.localAccountId,
        name: MOCK_USER.name,
        authorityType: 'MSSTS',
        clientInfo: '',
        realm: MOCK_USER.tenantId,
    };

    const accessTokenKey = [
        MOCK_USER.homeAccountId,
        MOCK_USER.environment,
        'accesstoken',
        CLIENT_ID,
        MOCK_USER.tenantId,
        'user.read openid profile',
    ].join('-');

    const accessTokenEntity = {
        homeAccountId: MOCK_USER.homeAccountId,
        environment: MOCK_USER.environment,
        credentialType: 'AccessToken',
        clientId: CLIENT_ID,
        secret: 'mock-access-token-for-e2e-tests',
        realm: MOCK_USER.tenantId,
        target: 'user.read openid profile',
        cachedAt: String(now),
        expiresOn: String(expiresOn),
        extendedExpiresOn: String(expiresOn + 3600),
        tokenType: 'Bearer',
    };

    const idTokenKey = [
        MOCK_USER.homeAccountId,
        MOCK_USER.environment,
        'idtoken',
        CLIENT_ID,
        MOCK_USER.tenantId,
        '',
    ].join('-');

    // Build a minimal JWT-shaped id token (header.payload.signature) with user claims
    const idTokenPayload = {
        aud: CLIENT_ID,
        iss: `${AUTHORITY}/v2.0`,
        iat: now,
        exp: expiresOn,
        name: MOCK_USER.name,
        preferred_username: MOCK_USER.email,
        oid: MOCK_USER.localAccountId,
        tid: MOCK_USER.tenantId,
        roles: MOCK_USER.roles,
        sub: MOCK_USER.localAccountId,
    };

    const base64Encode = (obj: Record<string, unknown>): string => {
        return Buffer.from(JSON.stringify(obj)).toString('base64url');
    };

    const mockIdToken = [
        base64Encode({ alg: 'none', typ: 'JWT' }),
        base64Encode(idTokenPayload),
        'mock-signature',
    ].join('.');

    const idTokenEntity = {
        homeAccountId: MOCK_USER.homeAccountId,
        environment: MOCK_USER.environment,
        credentialType: 'IdToken',
        clientId: CLIENT_ID,
        secret: mockIdToken,
        realm: MOCK_USER.tenantId,
    };

    // Inject all MSAL cache entries into sessionStorage
    await page.evaluate(
        ({ accountKey, accountEntity, accessTokenKey, accessTokenEntity, idTokenKey, idTokenEntity, clientId }) => {
            // Account keys index
            sessionStorage.setItem(
                'msal.account.keys',
                JSON.stringify([accountKey])
            );
            // Account entity
            sessionStorage.setItem(accountKey, JSON.stringify(accountEntity));

            // Token keys index
            sessionStorage.setItem(
                `msal.token.keys.${clientId}`,
                JSON.stringify({
                    accessToken: [accessTokenKey],
                    idToken: [idTokenKey],
                    refreshToken: [],
                })
            );

            // Access token entity
            sessionStorage.setItem(accessTokenKey, JSON.stringify(accessTokenEntity));
            // ID token entity
            sessionStorage.setItem(idTokenKey, JSON.stringify(idTokenEntity));

            // Active account hint
            sessionStorage.setItem(
                `msal.${clientId}.active-account`,
                accountEntity.homeAccountId
            );
        },
        { accountKey, accountEntity, accessTokenKey, accessTokenEntity, idTokenKey, idTokenEntity, clientId: CLIENT_ID }
    );

    // Reload so MsalProvider picks up the cached session
    await page.reload();

    // Verify we are now treated as authenticated (should NOT see the login page)
    await expect(page.getByText('Sign in with Microsoft')).not.toBeVisible({ timeout: 10_000 });

    // Persist browser storage state for other test projects
    await page.context().storageState({ path: STORAGE_STATE_PATH });
});
