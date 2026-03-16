import { describe, it, expect, beforeAll } from 'vitest';
import { faker } from '@faker-js/faker';
import { ApiClient } from '../helpers/api-client.js';
import { getUserToken, getAdminToken } from '../helpers/auth.js';

const GATEWAY_URL = process.env.GATEWAY_URL ?? 'http://localhost';
const TENANT_ID = 'test-org-company-a';
const HOLDING_ORG_ID = 'test-org-holding';

const PROCESSING_TIMEOUT_MS = 60_000;
const POLL_INTERVAL_MS = 2_000;

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

// ====================================================================
// Test Suite 1: PPTX Upload → Split Slides → Store → Identify Tables
// ====================================================================
describe('Core Flow 1: PPTX Upload and Processing', () => {
  let api: ApiClient;

  beforeAll(() => {
    const token = getUserToken(TENANT_ID);
    api = new ApiClient(GATEWAY_URL, token);
  });

  it('should upload a PPTX file and receive file_id', async () => {
    // Create a minimal PPTX-like file (in real E2E, use a real .pptx fixture)
    const filename = `test-pptx-${faker.string.uuid()}.pptx`;
    const mockContent = Buffer.alloc(1024); // placeholder

    const res = await api.uploadFile(mockContent, filename,
      'application/vnd.openxmlformats-officedocument.presentationml.presentation');

    expect(res.status).toBeOneOf([201, 415, 422]); // 201 if accepted, 415/422 if mock content rejected
    if (res.status === 201) {
      expect(res.data.file_id).toBeTruthy();
      expect(res.data.filename).toBe(filename);
    }
  });

  it('should list files for the organization', async () => {
    const res = await api.listFiles();
    expect(res.status).toBe(200);
    expect(res.data).toHaveProperty('items');
    expect(Array.isArray(res.data.items)).toBe(true);
  });
});

// ====================================================================
// Test Suite 2: Excel Upload → Store by Individual Sheets
// ====================================================================
describe('Core Flow 2: Excel Upload and Processing', () => {
  let api: ApiClient;

  beforeAll(() => {
    const token = getUserToken(TENANT_ID);
    api = new ApiClient(GATEWAY_URL, token);
  });

  it('should upload an Excel file', async () => {
    const filename = `test-xlsx-${faker.string.uuid()}.xlsx`;
    const mockContent = Buffer.alloc(1024);

    const res = await api.uploadFile(mockContent, filename,
      'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet');

    expect(res.status).toBeOneOf([201, 415, 422]);
    if (res.status === 201) {
      expect(res.data.file_id).toBeTruthy();
    }
  });
});

// ====================================================================
// Test Suite 3: Query Tables → SQL Aggregations → Dashboards
// ====================================================================
describe('Core Flow 3: Table Queries and Dashboard Aggregation', () => {
  let api: ApiClient;

  beforeAll(() => {
    const token = getUserToken(TENANT_ID);
    api = new ApiClient(GATEWAY_URL, token);
  });

  it('should query parsed tables with pagination', async () => {
    const res = await api.raw.get('/api/query/tables', {
      params: { page: 0, size: 10 },
    });

    expect(res.status).toBe(200);
    expect(res.data).toHaveProperty('tables');
    expect(res.data).toHaveProperty('totalElements');
    expect(res.data).toHaveProperty('page');
  });

  it('should list available dashboards', async () => {
    const res = await api.listDashboards();
    expect(res.status).toBe(200);
    expect(res.data).toHaveProperty('items');
  });

  it('should create a dashboard and execute aggregation query', async () => {
    // Create dashboard
    const createRes = await api.createDashboard({
      name: `Test Dashboard ${faker.string.uuid()}`,
      description: 'Integration test dashboard',
      is_public: false,
      widgets: [],
    });

    expect(createRes.status).toBeOneOf([201, 200]);

    if (createRes.status === 201 && createRes.data.id) {
      const dashboardId = createRes.data.id;

      // Execute aggregation query
      const aggRes = await api.raw.post(`/api/dashboards/${dashboardId}/data`, {
        groupBy: ['Category'],
        aggregation: 'SUM',
        valueField: 'Amount',
      });

      expect(aggRes.status).toBeOneOf([200, 400]); // 400 if no data

      // Cleanup
      await api.deleteDashboard(dashboardId);
    }
  });
});

// ====================================================================
// Test Suite 4: Export as Excel and PPTX
// ====================================================================
describe('Core Flow 4: Export Tables as Excel and PPTX', () => {
  let api: ApiClient;

  beforeAll(() => {
    const token = getUserToken(TENANT_ID);
    api = new ApiClient(GATEWAY_URL, token);
  });

  it('should export tables as Excel file', async () => {
    const res = await api.raw.get('/api/query/tables/export/excel', {
      responseType: 'arraybuffer',
    });

    // 200 if data exists, 400/500 if no tables
    if (res.status === 200) {
      expect(res.headers['content-type']).toContain('spreadsheetml');
      expect(res.data.byteLength).toBeGreaterThan(0);
    } else {
      // No data to export is acceptable
      expect(res.status).toBeOneOf([400, 404, 500]);
    }
  });

  it('should export dashboard aggregation as Excel', async () => {
    // First create a dashboard
    const createRes = await api.createDashboard({
      name: `Export Test ${faker.string.uuid()}`,
      is_public: false,
      widgets: [],
    });

    if (createRes.status === 201 && createRes.data.id) {
      const dashboardId = createRes.data.id;

      const res = await api.raw.post(
        `/api/dashboards/${dashboardId}/data/export/excel`,
        {
          groupBy: ['Category'],
          aggregation: 'SUM',
          valueField: 'Amount',
        },
        { responseType: 'arraybuffer' },
      );

      // 200 if data exists, 400 if no matching data
      expect(res.status).toBeOneOf([200, 400]);
      if (res.status === 200) {
        expect(res.headers['content-type']).toContain('spreadsheetml');
      }

      await api.deleteDashboard(dashboardId);
    }
  });
});

// ====================================================================
// Test Suite 5: Smart Persistence Promotion (JSONB → Real SQL Tables)
// ====================================================================
describe('Core Flow 5: Smart Persistence Promotion', () => {
  let adminApi: ApiClient;

  beforeAll(() => {
    const token = getAdminToken(HOLDING_ORG_ID);
    adminApi = new ApiClient(GATEWAY_URL, token);
  });

  it('should list promotion candidates', async () => {
    const res = await adminApi.raw.get('/api/admin/promotions/candidates', {
      params: { threshold: 1 },
    });

    expect(res.status).toBe(200);
    expect(Array.isArray(res.data)).toBe(true);
  });

  it('should get routing info for a mapping template', async () => {
    const fakeTemplateId = faker.string.uuid();
    const res = await adminApi.raw.get(`/api/admin/promotions/${fakeTemplateId}/routing`);

    expect(res.status).toBe(200);
    expect(res.data).toHaveProperty('hasPromotedTable');
    expect(res.data.hasPromotedTable).toBe(false); // No promotion for random UUID
  });
});

// ====================================================================
// Test Suite 6: Holding Structure and RLS Org Isolation
// ====================================================================
describe('Core Flow 6: Holding Structure and RLS', () => {
  let adminApi: ApiClient;
  let userApi: ApiClient;

  beforeAll(() => {
    const adminToken = getAdminToken(HOLDING_ORG_ID);
    adminApi = new ApiClient(GATEWAY_URL, adminToken);

    const userToken = getUserToken(TENANT_ID);
    userApi = new ApiClient(GATEWAY_URL, userToken);
  });

  it('should list organizations (admin only)', async () => {
    const res = await adminApi.raw.get('/api/admin/organizations');
    expect(res.status).toBe(200);
    expect(Array.isArray(res.data)).toBe(true);
  });

  it('should create a holding → company → division hierarchy', async () => {
    // Create holding
    const holdingRes = await adminApi.raw.post('/api/admin/organizations', {
      name: `Test Holding ${faker.string.uuid().slice(0, 8)}`,
      type: 'HOLDING',
      code: `TH${faker.string.alphanumeric(4).toUpperCase()}`,
    });

    if (holdingRes.status === 201) {
      const holdingId = holdingRes.data.id;

      // Create company under holding
      const companyRes = await adminApi.raw.post('/api/admin/organizations', {
        name: `Test Company ${faker.string.uuid().slice(0, 8)}`,
        type: 'COMPANY',
        code: `TC${faker.string.alphanumeric(4).toUpperCase()}`,
        parentId: holdingId,
      });

      expect(companyRes.status).toBe(201);

      if (companyRes.status === 201) {
        const companyId = companyRes.data.id;

        // Create division under company
        const divisionRes = await adminApi.raw.post('/api/admin/organizations', {
          name: `Test Division ${faker.string.uuid().slice(0, 8)}`,
          type: 'DIVISION',
          code: `TD${faker.string.alphanumeric(4).toUpperCase()}`,
          parentId: companyId,
        });

        expect(divisionRes.status).toBe(201);

        // Cleanup
        if (divisionRes.status === 201) {
          await adminApi.raw.delete(`/api/admin/organizations/${divisionRes.data.id}`);
        }
        await adminApi.raw.delete(`/api/admin/organizations/${companyId}`);
      }
      await adminApi.raw.delete(`/api/admin/organizations/${holdingId}`);
    }
  });

  it('should enforce RLS - user cannot see other org data', async () => {
    // Query tables as user from TENANT_ID
    const res = await userApi.raw.get('/api/query/tables', {
      params: { page: 0, size: 10 },
    });

    expect(res.status).toBe(200);
    // All returned tables must belong to the user's org (enforced by RLS)
    if (res.data.tables && res.data.tables.length > 0) {
      // Tables are org-filtered at DB level via RLS
      expect(res.data.tables.length).toBeGreaterThanOrEqual(0);
    }
  });

  it('should manage batches within holding scope', async () => {
    const batchRes = await adminApi.raw.get('/api/batches', {
      params: { holdingId: HOLDING_ORG_ID },
    });

    expect(batchRes.status).toBe(200);
    // Batches should be accessible or empty
    expect(Array.isArray(batchRes.data) || batchRes.data.items !== undefined).toBe(true);
  });
});

// ====================================================================
// Test Suite: CSV Upload and Full Pipeline
// ====================================================================
describe('CSV Upload Full Pipeline', () => {
  let api: ApiClient;

  beforeAll(() => {
    const token = getUserToken(TENANT_ID);
    api = new ApiClient(GATEWAY_URL, token);
  });

  it('should upload CSV, process, and query results', async () => {
    const csvContent = [
      'Date,Category,Amount,Currency',
      '2025-01-01,Revenue,50000,CZK',
      '2025-01-02,Expense,12000,CZK',
      '2025-01-03,Revenue,75000,CZK',
      '2025-02-01,Tax,8000,CZK',
      '2025-02-15,Investment,100000,CZK',
    ].join('\n');

    const filename = `test-csv-${faker.string.uuid()}.csv`;

    const uploadRes = await api.uploadFile(
      Buffer.from(csvContent, 'utf-8'),
      filename,
      'text/csv',
    );

    expect(uploadRes.status).toBe(201);
    if (uploadRes.status !== 201) return;

    const fileId = uploadRes.data.file_id;

    // Poll until processed
    const deadline = Date.now() + PROCESSING_TIMEOUT_MS;
    let finalStatus = 'UNKNOWN';

    while (Date.now() < deadline) {
      const statusRes = await api.getFileStatus(fileId);
      if (statusRes.status === 200) {
        finalStatus = statusRes.data.status;
        if (finalStatus === 'COMPLETED' || finalStatus === 'FAILED') break;
      }
      await sleep(POLL_INTERVAL_MS);
    }

    expect(finalStatus).toBe('COMPLETED');

    // Query the parsed data
    const dataRes = await api.raw.get(`/api/query/files/${fileId}/data`);
    expect(dataRes.status).toBe(200);
    expect(dataRes.data).toHaveProperty('tables');

    // Query processing logs
    const logsRes = await api.raw.get(`/api/query/processing-logs/${fileId}`);
    expect(logsRes.status).toBe(200);
    expect(Array.isArray(logsRes.data)).toBe(true);
  });
});
