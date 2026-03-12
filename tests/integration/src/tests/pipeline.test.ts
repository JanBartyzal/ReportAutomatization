import { describe, it, expect, beforeAll } from 'vitest';
import { faker } from '@faker-js/faker';
import { ApiClient } from '../helpers/api-client.js';
import { getUserToken } from '../helpers/auth.js';

const GATEWAY_URL = process.env.GATEWAY_URL ?? 'http://localhost';
const TENANT_ID = 'test-org-company-a';

/** Max time to wait for file processing to complete (ms). */
const PROCESSING_TIMEOUT_MS = 45_000;
/** Interval between processing status polls (ms). */
const POLL_INTERVAL_MS = 2_000;

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

describe('Pipeline Integration Test', () => {
  let api: ApiClient;

  beforeAll(() => {
    const token = getUserToken(TENANT_ID);
    api = new ApiClient(GATEWAY_URL, token);
  });

  /**
   * End-to-end flow:
   *   1. Upload a CSV file
   *   2. Verify it appears in the file list
   *   3. Poll processing status until completion
   *   4. Query parsed data and verify results
   */
  it('should process an uploaded file end-to-end', async () => {
    // ---- Step 1: Upload a file ----
    const csvContent = generateTestCsv();
    const filename = `test-${faker.string.uuid()}.csv`;

    const uploadRes = await api.uploadFile(
      Buffer.from(csvContent, 'utf-8'),
      filename,
      'text/csv',
    );

    expect(uploadRes.status).toBe(201);
    expect(uploadRes.data.file_id).toBeTruthy();
    expect(uploadRes.data.filename).toBe(filename);
    expect(uploadRes.data.status).toMatch(/UPLOADED|PROCESSING/);

    const fileId = uploadRes.data.file_id;

    // ---- Step 2: Verify the file appears in the list ----
    const listRes = await api.listFiles();
    expect(listRes.status).toBe(200);

    const uploadedFile = listRes.data.items.find((f) => f.file_id === fileId);
    expect(uploadedFile).toBeDefined();
    expect(uploadedFile!.filename).toBe(filename);

    // ---- Step 3: Poll processing status ----
    const finalStatus = await pollProcessingStatus(api, fileId);
    expect(finalStatus).toBe('COMPLETED');

    // ---- Step 4: Verify file details show completed status ----
    const detailRes = await api.getFile(fileId);
    expect(detailRes.status).toBe(200);
    expect(detailRes.data.status).toBe('COMPLETED');

    // ---- Step 5: Query parsed data ----
    const queryRes = await api.queryReports({ org_id: TENANT_ID });
    expect(queryRes.status).toBe(200);
    // At minimum the response should be a valid list
    expect(queryRes.data).toHaveProperty('items');
    expect(Array.isArray(queryRes.data.items)).toBe(true);
  });

  it('should upload and list multiple files', async () => {
    const files = Array.from({ length: 3 }, (_, i) => ({
      name: `test-multi-${i}-${faker.string.uuid()}.csv`,
      content: generateTestCsv(5 + i),
    }));

    const fileIds: string[] = [];

    for (const file of files) {
      const res = await api.uploadFile(
        Buffer.from(file.content, 'utf-8'),
        file.name,
        'text/csv',
      );
      expect(res.status).toBe(201);
      fileIds.push(res.data.file_id);
    }

    // Verify all files are present in the list
    const listRes = await api.listFiles({ page_size: 50 });
    expect(listRes.status).toBe(200);

    for (const id of fileIds) {
      const found = listRes.data.items.find((f) => f.file_id === id);
      expect(found).toBeDefined();
    }
  });

  it('should return file details for a known file', async () => {
    const csvContent = generateTestCsv();
    const filename = `test-detail-${faker.string.uuid()}.csv`;

    const uploadRes = await api.uploadFile(
      Buffer.from(csvContent, 'utf-8'),
      filename,
      'text/csv',
    );
    expect(uploadRes.status).toBe(201);

    const fileId = uploadRes.data.file_id;

    const detailRes = await api.getFile(fileId);
    expect(detailRes.status).toBe(200);
    expect(detailRes.data.file_id).toBe(fileId);
    expect(detailRes.data.filename).toBe(filename);
    expect(detailRes.data.mime_type).toBe('text/csv');
    expect(detailRes.data.org_id).toBeTruthy();
  });

  it('should return 404 for non-existent file', async () => {
    const res = await api.getFile('test-nonexistent-id');
    expect(res.status).toBe(404);
  });
});

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

/**
 * Generate a simple CSV string with realistic-looking financial data.
 */
function generateTestCsv(rows = 10): string {
  const headers = ['Date', 'Category', 'Amount', 'Currency', 'Description'];
  const lines = [headers.join(',')];

  for (let i = 0; i < rows; i++) {
    lines.push(
      [
        faker.date.recent({ days: 90 }).toISOString().split('T')[0],
        faker.helpers.arrayElement(['Revenue', 'Expense', 'Tax', 'Investment']),
        faker.finance.amount({ min: 100, max: 100_000, dec: 2 }),
        faker.helpers.arrayElement(['EUR', 'USD', 'CZK']),
        faker.finance.transactionDescription(),
      ].join(','),
    );
  }

  return lines.join('\n');
}

/**
 * Poll the processing status endpoint until the file reaches a terminal
 * state (COMPLETED or FAILED) or the timeout expires.
 */
async function pollProcessingStatus(
  api: ApiClient,
  fileId: string,
): Promise<string> {
  const deadline = Date.now() + PROCESSING_TIMEOUT_MS;

  while (Date.now() < deadline) {
    const res = await api.getFileStatus(fileId);

    if (res.status === 200) {
      const status = res.data.status;
      if (status === 'COMPLETED' || status === 'FAILED') {
        return status;
      }
    }

    await sleep(POLL_INTERVAL_MS);
  }

  throw new Error(
    `File ${fileId} did not reach terminal status within ${PROCESSING_TIMEOUT_MS / 1000}s`,
  );
}
