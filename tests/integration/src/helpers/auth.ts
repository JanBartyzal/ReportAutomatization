import crypto from 'node:crypto';

/**
 * JWT-like token claims for test purposes.
 * In the local dev environment the auth service accepts tokens with
 * the header `X-Test-Auth: true`, so we don't need a real MSAL token.
 */
export interface TestTokenClaims {
  sub: string;
  email: string;
  name: string;
  org_id: string;
  roles: string[];
  /** Token issued-at (epoch seconds). Defaults to now. */
  iat?: number;
  /** Token expiration (epoch seconds). Defaults to now + 1 hour. */
  exp?: number;
}

const DEFAULT_TTL_SECONDS = 3600;

/**
 * Generate a base64url-encoded mock JWT for integration tests.
 *
 * Structure: header.payload.signature
 * The signature is a simple HMAC-SHA256 with a static secret so the
 * auth service can recognise test tokens when X-Test-Auth is set.
 */
export function generateTestToken(claims: TestTokenClaims): string {
  const now = Math.floor(Date.now() / 1000);

  const header = {
    alg: 'HS256',
    typ: 'JWT',
    test: true,
  };

  const payload: Record<string, unknown> = {
    ...claims,
    iat: claims.iat ?? now,
    exp: claims.exp ?? now + DEFAULT_TTL_SECONDS,
    iss: 'integration-test',
    aud: 'reportplatform',
  };

  const encode = (obj: unknown): string =>
    Buffer.from(JSON.stringify(obj)).toString('base64url');

  const headerB64 = encode(header);
  const payloadB64 = encode(payload);

  // Static test secret - never used in production
  const secret = 'integration-test-secret-do-not-use-in-prod';
  const signature = crypto
    .createHmac('sha256', secret)
    .update(`${headerB64}.${payloadB64}`)
    .digest('base64url');

  return `${headerB64}.${payloadB64}.${signature}`;
}

/**
 * Returns a token with HOLDING_ADMIN role, full cross-tenant access.
 */
export function getAdminToken(orgId = 'org-holding-001'): string {
  return generateTestToken({
    sub: 'user-admin-001',
    email: 'admin@example.com',
    name: 'Test Admin',
    org_id: orgId,
    roles: ['HOLDING_ADMIN', 'ADMIN'],
  });
}

/**
 * Returns a token with EDITOR role scoped to the given tenant.
 */
export function getUserToken(tenantId: string): string {
  return generateTestToken({
    sub: `user-${tenantId}`,
    email: `user-${tenantId}@example.com`,
    name: `Test User (${tenantId})`,
    org_id: tenantId,
    roles: ['EDITOR'],
  });
}

/**
 * Returns a token with VIEWER role (read-only) scoped to the given tenant.
 */
export function getViewerToken(tenantId: string): string {
  return generateTestToken({
    sub: `viewer-${tenantId}`,
    email: `viewer-${tenantId}@example.com`,
    name: `Test Viewer (${tenantId})`,
    org_id: tenantId,
    roles: ['VIEWER'],
  });
}

/**
 * Returns a deliberately expired token for negative tests.
 */
export function getExpiredToken(): string {
  const past = Math.floor(Date.now() / 1000) - 7200; // 2 hours ago
  return generateTestToken({
    sub: 'user-expired',
    email: 'expired@example.com',
    name: 'Expired User',
    org_id: 'org-company-a',
    roles: ['EDITOR'],
    iat: past - 3600,
    exp: past,
  });
}
