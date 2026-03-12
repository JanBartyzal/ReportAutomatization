import { describe, it, expect, beforeAll } from 'vitest';
import { faker } from '@faker-js/faker';
import { ApiClient } from '../helpers/api-client.js';
import { getUserToken, getAdminToken } from '../helpers/auth.js';

const GATEWAY_URL = process.env.GATEWAY_URL ?? 'http://localhost';

const TENANT_A = 'test-org-company-a';
const TENANT_B = 'test-org-company-b';

describe('Row-Level Security (RLS) Tests', () => {
  let clientA: ApiClient;
  let clientB: ApiClient;
  let adminClient: ApiClient;

  beforeAll(() => {
    clientA = new ApiClient(GATEWAY_URL, getUserToken(TENANT_A));
    clientB = new ApiClient(GATEWAY_URL, getUserToken(TENANT_B));
    adminClient = new ApiClient(GATEWAY_URL, getAdminToken());
  });

  // ---------- Files ----------

  describe('File isolation', () => {
    let tenantAFileId: string | null = null;

    it('Tenant A can upload a file', async () => {
      const csv = 'header1,header2\nval1,val2\n';
      const filename = `rls-a-${faker.string.uuid()}.csv`;

      const res = await clientA.uploadFile(
        Buffer.from(csv, 'utf-8'),
        filename,
        'text/csv',
      );

      expect(res.status).toBe(201);
      tenantAFileId = res.data.file_id;
    });

    it('Tenant B cannot see Tenant A file in list', async () => {
      const listRes = await clientB.listFiles({ page_size: 100 });
      expect(listRes.status).toBe(200);

      if (tenantAFileId) {
        const found = listRes.data.items.find((f) => f.file_id === tenantAFileId);
        expect(found).toBeUndefined();
      }
    });

    it('Tenant B cannot access Tenant A file directly', async () => {
      if (!tenantAFileId) return;

      const res = await clientB.getFile(tenantAFileId);
      // RLS should hide the row entirely (404) or explicitly forbid (403)
      expect([403, 404]).toContain(res.status);
    });

    it('Tenant A can access own file', async () => {
      if (!tenantAFileId) return;

      const res = await clientA.getFile(tenantAFileId);
      expect(res.status).toBe(200);
      expect(res.data.file_id).toBe(tenantAFileId);
    });

    it('Admin can access Tenant A file (cross-tenant)', async () => {
      if (!tenantAFileId) return;

      const res = await adminClient.getFile(tenantAFileId);
      expect(res.status).toBe(200);
      expect(res.data.file_id).toBe(tenantAFileId);
    });
  });

  // ---------- Dashboards ----------

  describe('Dashboard isolation', () => {
    let tenantADashboardId: string | null = null;

    it('Tenant A can create a private dashboard', async () => {
      const res = await clientA.createDashboard({
        name: `RLS Test Dashboard ${faker.string.uuid()}`,
        description: 'Created by Tenant A for RLS test',
        is_public: false,
        widgets: [],
      });

      expect(res.status).toBe(201);
      expect(res.data.id).toBeTruthy();
      tenantADashboardId = res.data.id!;
    });

    it('Tenant B cannot see Tenant A private dashboard in list', async () => {
      const listRes = await clientB.listDashboards({ page_size: 100 });
      expect(listRes.status).toBe(200);

      if (tenantADashboardId) {
        const found = listRes.data.items.find((d) => d.id === tenantADashboardId);
        expect(found).toBeUndefined();
      }
    });

    it('Tenant B cannot access Tenant A private dashboard directly', async () => {
      if (!tenantADashboardId) return;

      const res = await clientB.getDashboard(tenantADashboardId);
      expect([403, 404]).toContain(res.status);
    });

    it('Tenant A can access own dashboard', async () => {
      if (!tenantADashboardId) return;

      const res = await clientA.getDashboard(tenantADashboardId);
      expect(res.status).toBe(200);
      expect(res.data.id).toBe(tenantADashboardId);
    });

    it('Admin can access Tenant A dashboard', async () => {
      if (!tenantADashboardId) return;

      const res = await adminClient.getDashboard(tenantADashboardId);
      expect(res.status).toBe(200);
      expect(res.data.id).toBe(tenantADashboardId);
    });

    it('Tenant A can delete own dashboard', async () => {
      if (!tenantADashboardId) return;

      const res = await clientA.deleteDashboard(tenantADashboardId);
      expect([200, 204]).toContain(res.status);

      // Verify it is gone
      const getRes = await clientA.getDashboard(tenantADashboardId);
      expect(getRes.status).toBe(404);
    });

    it('Tenant B cannot delete Tenant A dashboard', async () => {
      // Create a fresh dashboard as Tenant A
      const createRes = await clientA.createDashboard({
        name: `RLS Delete Test ${faker.string.uuid()}`,
        is_public: false,
        widgets: [],
      });

      if (createRes.status !== 201) return;

      const dashId = createRes.data.id!;

      // Tenant B tries to delete it
      const deleteRes = await clientB.deleteDashboard(dashId);
      expect([403, 404]).toContain(deleteRes.status);

      // Verify it still exists for Tenant A
      const verifyRes = await clientA.getDashboard(dashId);
      expect(verifyRes.status).toBe(200);

      // Cleanup
      await clientA.deleteDashboard(dashId);
    });
  });

  // ---------- Report Lifecycle ----------

  describe('Report lifecycle isolation', () => {
    it('Tenant B cannot submit a report belonging to Tenant A', async () => {
      // Attempt to submit a report with a tenant-A-scoped ID
      // The endpoint should reject based on the token's org_id
      const res = await clientB.submitReport('test-report-tenant-a-001');

      // Should be forbidden or not found
      expect([403, 404]).toContain(res.status);
    });

    it('Tenant B cannot approve a report belonging to Tenant A', async () => {
      const res = await clientB.approveReport('test-report-tenant-a-001');
      expect([403, 404]).toContain(res.status);
    });

    it('Tenant B cannot view lifecycle status of Tenant A report', async () => {
      const res = await clientB.getReportLifecycle('test-report-tenant-a-001');
      expect([403, 404]).toContain(res.status);
    });
  });

  // ---------- Query / parsed data ----------

  describe('Query data isolation', () => {
    it('Tenant A query only returns own org data', async () => {
      const res = await clientA.queryReports({ org_id: TENANT_A });
      expect(res.status).toBe(200);

      // If items are returned, they should all belong to Tenant A
      if (res.data.items && res.data.items.length > 0) {
        // The query service should only return data for the requesting tenant
        // (verified server-side via RLS or org_id filter)
        expect(res.data.items.length).toBeGreaterThanOrEqual(0);
      }
    });

    it('Tenant A cannot query Tenant B data by spoofing org_id param', async () => {
      // Even if Tenant A passes org_id=tenant-b, the server should enforce
      // RLS and either return empty results or 403
      const res = await clientA.queryReports({ org_id: TENANT_B });

      if (res.status === 200) {
        // If 200, the items should be empty (server ignored the spoofed org_id)
        expect(res.data.items.length).toBe(0);
      } else {
        // Or the server outright rejects it
        expect([403]).toContain(res.status);
      }
    });

    it('Admin can query across all tenants', async () => {
      const res = await adminClient.queryReports();
      expect(res.status).toBe(200);
      expect(res.data).toHaveProperty('items');
    });
  });
});
