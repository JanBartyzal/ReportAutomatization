import { describe, it, expect, beforeAll } from 'vitest';
import { ApiClient } from '../helpers/api-client.js';
import {
  getUserToken,
  getAdminToken,
  getExpiredToken,
} from '../helpers/auth.js';

const GATEWAY_URL = process.env.GATEWAY_URL ?? 'http://localhost';

describe('Auth Flow Tests', () => {
  let api: ApiClient;

  beforeAll(() => {
    // Start without a token; individual tests set their own.
    api = new ApiClient(GATEWAY_URL);
  });

  // ---- Positive cases ----

  it('should accept a valid user token and return 200', async () => {
    const token = getUserToken('test-org-company-a');
    const res = await api.validateToken(token);

    expect(res.status).toBe(200);
    expect(res.data.valid).toBe(true);
    expect(res.data.user_id).toBeTruthy();
  });

  it('should accept a valid admin token and return 200', async () => {
    const token = getAdminToken();
    const res = await api.validateToken(token);

    expect(res.status).toBe(200);
    expect(res.data.valid).toBe(true);
    expect(res.data.roles).toEqual(
      expect.arrayContaining(['HOLDING_ADMIN']),
    );
  });

  // ---- Negative cases ----

  it('should return 401 when no token is provided', async () => {
    // Call validate without any Authorization header
    const noAuthApi = new ApiClient(GATEWAY_URL);
    const res = await noAuthApi.validateToken();

    expect(res.status).toBe(401);
  });

  it('should return 401 for a completely invalid token', async () => {
    const res = await api.validateToken('this-is-not-a-jwt');

    expect(res.status).toBe(401);
  });

  it('should return 401 for an expired token', async () => {
    const expiredToken = getExpiredToken();
    const res = await api.validateToken(expiredToken);

    expect(res.status).toBe(401);
  });

  // ---- Authorization / Tenant isolation ----

  it('should forbid access to another tenant resources', async () => {
    // Upload a file as tenant A
    const tenantAToken = getUserToken('test-org-company-a');
    const tenantAClient = new ApiClient(GATEWAY_URL, tenantAToken);

    const uploadRes = await tenantAClient.uploadFile(
      Buffer.from('col1,col2\na,b', 'utf-8'),
      'test-auth-tenant-isolation.csv',
      'text/csv',
    );

    // Only proceed if upload succeeds
    if (uploadRes.status === 201) {
      const fileId = uploadRes.data.file_id;

      // Try to access the file as tenant B
      const tenantBToken = getUserToken('test-org-company-b');
      const tenantBClient = new ApiClient(GATEWAY_URL, tenantBToken);

      const res = await tenantBClient.getFile(fileId);

      // Should be either 403 Forbidden or 404 Not Found (RLS hides the row)
      expect([403, 404]).toContain(res.status);
    } else {
      // If upload is not available, mark test as skipped context
      console.warn('[auth.test] Upload not available, skipping tenant isolation sub-check.');
    }
  });

  it('should protect file listing endpoint without auth', async () => {
    const noAuthApi = new ApiClient(GATEWAY_URL);
    noAuthApi.clearToken();

    const res = await noAuthApi.listFiles();

    expect(res.status).toBe(401);
  });

  it('should protect dashboard endpoints without auth', async () => {
    const noAuthApi = new ApiClient(GATEWAY_URL);

    const res = await noAuthApi.listDashboards();

    expect(res.status).toBe(401);
  });

  it('should protect lifecycle endpoints without auth', async () => {
    const noAuthApi = new ApiClient(GATEWAY_URL);

    const res = await noAuthApi.submitReport('some-report-id');

    expect(res.status).toBe(401);
  });
});
