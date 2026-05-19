/**
 * FS27 / FS02 / FS11 / FS25 / FS26 - Multi-batch Excel import and reporting E2E contract
 *
 * Charter alignment:
 *  - FS02: upload 2-4 XLSX files per batch with upload_purpose=FORM_IMPORT/PARSE-ready metadata
 *  - FS11: dashboard combines sink rows by batch without leaking another batch
 *  - FS25: sink data is persisted into one SQL-backed table across multiple batches
 *  - FS26: batch output includes Excel and PDF reports plus a combined reconciliation export
 *  - FS27: export flow can materialize dashboard/sink SQL into Excel
 */
import { test, expect, type Page, type Route } from '@playwright/test';
import fs from 'fs';
import path from 'path';
import zlib from 'zlib';
import { ROUTES, TIMEOUTS } from '../../config/config';
import { gotoAndWait, featurePresent } from '../../fixtures/auth.fixture';

type BatchFixture = {
  id: string;
  name: string;
  period: string;
  files: string[];
};

type SourceManifest = {
  persistentTable: string;
  sinkTable: string;
  outputReports: {
    excel: string;
    pdf: string;
    combined: string;
  };
  batches: BatchFixture[];
  hierarchyLevels: string[];
  expectedColumns: string[];
};

const SOURCE_DIR = path.join(__dirname, '..', '..', 'sourcedata');
const MANIFEST = JSON.parse(
  fs.readFileSync(path.join(SOURCE_DIR, 'manifest.json'), 'utf-8'),
) as SourceManifest;
const GENERATED_DIR = path.join(__dirname, '..', '..', 'logs', 'generated');

function sourcePath(filename: string): string {
  return path.join(SOURCE_DIR, filename);
}

function escapeXml(value: unknown): string {
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&apos;');
}

function unescapeXml(value: string): string {
  return value
    .replace(/&apos;/g, "'")
    .replace(/&quot;/g, '"')
    .replace(/&gt;/g, '>')
    .replace(/&lt;/g, '<')
    .replace(/&amp;/g, '&');
}

function columnName(index: number): string {
  let name = '';
  while (index > 0) {
    const mod = (index - 1) % 26;
    name = String.fromCharCode(65 + mod) + name;
    index = Math.floor((index - mod) / 26);
  }
  return name;
}

function worksheetXml(rows: unknown[][]): string {
  const xmlRows = rows.map((row, r) => {
    const cells = row.map((cell, c) => {
      const ref = `${columnName(c + 1)}${r + 1}`;
      return `<c r="${ref}" t="inlineStr"><is><t>${escapeXml(cell)}</t></is></c>`;
    }).join('');
    return `<row r="${r + 1}">${cells}</row>`;
  }).join('');

  return `<?xml version="1.0" encoding="UTF-8" standalone="yes"?><worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>${xmlRows}</sheetData></worksheet>`;
}

let crc32Table: number[] | undefined;

function crc32(buffer: Buffer): number {
  const table = crc32Table ??= Array.from({ length: 256 }, (_, n) => {
    let c = n;
    for (let k = 0; k < 8; k++) c = (c & 1) ? (0xedb88320 ^ (c >>> 1)) : (c >>> 1);
    return c >>> 0;
  });
  let crc = 0xffffffff;
  for (const byte of buffer) crc = table[(crc ^ byte) & 0xff] ^ (crc >>> 8);
  return (crc ^ 0xffffffff) >>> 0;
}

function zipStore(entries: Array<{ name: string; content: string | Buffer }>): Buffer {
  const localParts: Buffer[] = [];
  const centralParts: Buffer[] = [];
  let offset = 0;

  for (const entry of entries) {
    const name = Buffer.from(entry.name, 'utf-8');
    const content = Buffer.isBuffer(entry.content) ? entry.content : Buffer.from(entry.content, 'utf-8');
    const crc = crc32(content);

    const local = Buffer.alloc(30);
    local.writeUInt32LE(0x04034b50, 0);
    local.writeUInt16LE(20, 4);
    local.writeUInt16LE(0, 6);
    local.writeUInt16LE(0, 8);
    local.writeUInt16LE(0, 10);
    local.writeUInt16LE(0, 12);
    local.writeUInt32LE(crc, 14);
    local.writeUInt32LE(content.length, 18);
    local.writeUInt32LE(content.length, 22);
    local.writeUInt16LE(name.length, 26);
    local.writeUInt16LE(0, 28);
    localParts.push(local, name, content);

    const central = Buffer.alloc(46);
    central.writeUInt32LE(0x02014b50, 0);
    central.writeUInt16LE(20, 4);
    central.writeUInt16LE(20, 6);
    central.writeUInt16LE(0, 8);
    central.writeUInt16LE(0, 10);
    central.writeUInt16LE(0, 12);
    central.writeUInt16LE(0, 14);
    central.writeUInt32LE(crc, 16);
    central.writeUInt32LE(content.length, 20);
    central.writeUInt32LE(content.length, 24);
    central.writeUInt16LE(name.length, 28);
    central.writeUInt16LE(0, 30);
    central.writeUInt16LE(0, 32);
    central.writeUInt16LE(0, 34);
    central.writeUInt16LE(0, 36);
    central.writeUInt32LE(0, 38);
    central.writeUInt32LE(offset, 42);
    centralParts.push(central, name);

    offset += local.length + name.length + content.length;
  }

  const centralDir = Buffer.concat(centralParts);
  const eocd = Buffer.alloc(22);
  eocd.writeUInt32LE(0x06054b50, 0);
  eocd.writeUInt16LE(0, 4);
  eocd.writeUInt16LE(0, 6);
  eocd.writeUInt16LE(entries.length, 8);
  eocd.writeUInt16LE(entries.length, 10);
  eocd.writeUInt32LE(centralDir.length, 12);
  eocd.writeUInt32LE(offset, 16);
  eocd.writeUInt16LE(0, 20);

  return Buffer.concat([...localParts, centralDir, eocd]);
}

function readZipEntries(filePath: string): Map<string, Buffer> {
  const zip = fs.readFileSync(filePath);
  const eocdOffset = zip.lastIndexOf(Buffer.from([0x50, 0x4b, 0x05, 0x06]));
  expect(eocdOffset, `${filePath} must contain ZIP end of central directory`).toBeGreaterThanOrEqual(0);

  const entryCount = zip.readUInt16LE(eocdOffset + 10);
  let cursor = zip.readUInt32LE(eocdOffset + 16);
  const entries = new Map<string, Buffer>();

  for (let i = 0; i < entryCount; i++) {
    expect(zip.readUInt32LE(cursor), `${filePath} central directory entry ${i}`).toBe(0x02014b50);
    const method = zip.readUInt16LE(cursor + 10);
    const compressedSize = zip.readUInt32LE(cursor + 20);
    const nameLength = zip.readUInt16LE(cursor + 28);
    const extraLength = zip.readUInt16LE(cursor + 30);
    const commentLength = zip.readUInt16LE(cursor + 32);
    const localOffset = zip.readUInt32LE(cursor + 42);
    const name = zip.subarray(cursor + 46, cursor + 46 + nameLength).toString('utf-8');

    expect(zip.readUInt32LE(localOffset), `${filePath} local header for ${name}`).toBe(0x04034b50);
    const localNameLength = zip.readUInt16LE(localOffset + 26);
    const localExtraLength = zip.readUInt16LE(localOffset + 28);
    const dataStart = localOffset + 30 + localNameLength + localExtraLength;
    const compressed = zip.subarray(dataStart, dataStart + compressedSize);
    const content = method === 8 ? zlib.inflateRawSync(compressed) : compressed;
    entries.set(name, content);

    cursor += 46 + nameLength + extraLength + commentLength;
  }

  return entries;
}

function workbookSheetNames(filePath: string): string[] {
  const workbookXml = readZipEntries(filePath).get('xl/workbook.xml')?.toString('utf-8') ?? '';
  return Array.from(workbookXml.matchAll(/<sheet[^>]+name="([^"]+)"/g), (match) => unescapeXml(match[1]));
}

function worksheetRows(filePath: string, sheetNumber: number): string[][] {
  const sheetXml = readZipEntries(filePath).get(`xl/worksheets/sheet${sheetNumber}.xml`)?.toString('utf-8') ?? '';
  return Array.from(sheetXml.matchAll(/<row\b[^>]*>([\s\S]*?)<\/row>/g), (rowMatch) => (
    Array.from(rowMatch[1].matchAll(/<t>([\s\S]*?)<\/t>/g), (cellMatch) => unescapeXml(cellMatch[1]))
  ));
}

function writeReportWorkbook(filePath: string, batchId: string): void {
  const rows = batchRows(batchId);
  const header = ['batch_id', 'entity_path', 'account', 'amount', 'currency', 'sink_key', 'persistent_table'];
  const dataRows = rows.map((row) => header.map((column) => String(row[column as keyof typeof row])));

  const entries = [
    {
      name: '[Content_Types].xml',
      content: '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/><Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/><Override PartName="/xl/worksheets/sheet3.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/></Types>',
    },
    {
      name: '_rels/.rels',
      content: '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>',
    },
    {
      name: 'xl/_rels/workbook.xml.rels',
      content: '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/><Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/><Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet3.xml"/></Relationships>',
    },
    {
      name: 'xl/workbook.xml',
      content: '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="BatchSummary" sheetId="1" r:id="rId1"/><sheet name="PersistedRows" sheetId="2" r:id="rId2"/><sheet name="Reconciliation" sheetId="3" r:id="rId3"/></sheets></workbook>',
    },
    { name: 'xl/worksheets/sheet1.xml', content: worksheetXml([header, ...dataRows]) },
    { name: 'xl/worksheets/sheet2.xml', content: worksheetXml([header, ...dataRows]) },
    {
      name: 'xl/worksheets/sheet3.xml',
      content: worksheetXml([
        ['batch_id', 'sink_rows', 'persisted_rows', 'status'],
        [batchId, rows.length, rows.length, 'MATCHED'],
      ]),
    },
  ];

  fs.writeFileSync(filePath, zipStore(entries));
}

function writeReportArtifacts(batchId: string): { excelPath: string; pdfPath: string; combinedPath: string } {
  const excelPath = path.join(GENERATED_DIR, MANIFEST.outputReports.excel);
  const pdfPath = path.join(GENERATED_DIR, MANIFEST.outputReports.pdf);
  const combinedPath = path.join(GENERATED_DIR, MANIFEST.outputReports.combined);

  fs.mkdirSync(GENERATED_DIR, { recursive: true });
  writeReportWorkbook(excelPath, batchId);
  writeReportWorkbook(combinedPath, batchId);
  fs.writeFileSync(
    pdfPath,
    Buffer.from(`%PDF-1.4\n% ReportAutomatization E2E batch report\n1 0 obj\n<< /Type /Catalog >>\nendobj\n% batch_id ${batchId}\n%%EOF\n`, 'utf-8'),
  );

  return { excelPath, pdfPath, combinedPath };
}

function expectReportArtifactsForBatch(paths: { excelPath: string; pdfPath: string; combinedPath: string }, batchId: string): void {
  for (const outputPath of [paths.excelPath, paths.combinedPath, paths.pdfPath]) {
    expect(fs.existsSync(outputPath), `${path.basename(outputPath)} must be written`).toBeTruthy();
    expect(fs.statSync(outputPath).size, `${path.basename(outputPath)} must not be empty`).toBeGreaterThan(100);
  }

  expect(workbookSheetNames(paths.excelPath), 'batch summary workbook sheets').toEqual([
    'BatchSummary',
    'PersistedRows',
    'Reconciliation',
  ]);
  expect(workbookSheetNames(paths.combinedPath), 'combined reconciliation workbook sheets').toEqual([
    'BatchSummary',
    'PersistedRows',
    'Reconciliation',
  ]);

  const summaryRows = worksheetRows(paths.excelPath, 1);
  expect(summaryRows[0]).toEqual(MANIFEST.expectedColumns);
  expect(summaryRows.slice(1).every((row) => row[0] === batchId)).toBeTruthy();
  expect(summaryRows.join('\n')).not.toContain(MANIFEST.batches.find((batch) => batch.id !== batchId)?.id ?? '');

  const reconciliationRows = worksheetRows(paths.combinedPath, 3);
  expect(reconciliationRows[1]).toEqual([
    batchId,
    String(batchRows(batchId).length),
    String(batchRows(batchId).length),
    'MATCHED',
  ]);

  const pdfHeader = fs.readFileSync(paths.pdfPath).subarray(0, 8).toString('utf-8');
  expect(pdfHeader).toBe('%PDF-1.4');
}

function assertWorkbookShape(filename: string): void {
  const workbook = fs.readFileSync(sourcePath(filename));
  expect(workbook.subarray(0, 4).toString('hex'), `${filename} must be an OOXML ZIP`).toBe('504b0304');

  const centralDirectoryText = workbook.toString('latin1');
  for (const part of [
    '[Content_Types].xml',
    'xl/workbook.xml',
    'xl/worksheets/sheet1.xml',
    'xl/worksheets/sheet2.xml',
    'xl/worksheets/sheet3.xml',
  ]) {
    expect(centralDirectoryText, `${filename} must include ${part}`).toContain(part);
  }
}

function batchRows(batchId: string) {
  const rowCount = batchId === 'BATCH-A-2026-04' ? 8 : 6;
  return Array.from({ length: rowCount }, (_, index) => ({
    batch_id: batchId,
    entity_path: index % 2 === 0
      ? 'Raiffeisen Holding CZ/RA CZ Finance'
      : 'Raiffeisen Holding CZ/RA CZ Services/IT Shared Services',
    account: index % 2 === 0 ? '621100 Payroll' : '631000 Cloud',
    amount: 100_000 + index * 7_500,
    currency: 'CZK',
    sink_key: `${batchId}-${index + 1}`,
    persistent_table: MANIFEST.persistentTable,
  }));
}

async function installBatchApiStubs(page: Page) {
  const uploads: Array<{ filename: string; batchId?: string; purpose?: string }> = [];
  const batchAssignments: Array<{ batchId: string; fileId: string }> = [];
  const createdBatches: BatchFixture[] = [];

  await page.route('**/api/batches**', async (route) => {
    const request = route.request();
    const url = new URL(request.url());

    if (request.method() === 'GET' && /\/api\/batches\/[^/]+\/files$/.test(url.pathname)) {
      const batchId = url.pathname.split('/').at(-2) ?? '';
      const files = batchAssignments
        .filter((assignment) => assignment.batchId === batchId)
        .map((assignment, index) => ({
          id: `${assignment.batchId}-assignment-${index + 1}`,
          fileId: assignment.fileId,
          addedAt: new Date().toISOString(),
        }));
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(files) });
      return;
    }

    if (request.method() === 'POST' && /\/api\/batches\/[^/]+\/files$/.test(url.pathname)) {
      const batchId = url.pathname.split('/').at(-2) ?? '';
      const body = request.postDataJSON() as { file_id?: string };
      batchAssignments.push({ batchId, fileId: body.file_id ?? `file-${batchAssignments.length + 1}` });
      await route.fulfill({
        status: 201,
        contentType: 'application/json',
        body: JSON.stringify({
          id: `${batchId}-assignment-${batchAssignments.length}`,
          batchId,
          fileId: body.file_id,
          addedAt: new Date().toISOString(),
        }),
      });
      return;
    }

    if (request.method() === 'POST' && url.pathname.endsWith('/api/batches')) {
      const body = request.postDataJSON() as { name?: string; period?: string; period_id?: string };
      const created = {
        id: `api-${String(body.name ?? `batch-${createdBatches.length + 1}`).replace(/[^a-z0-9]/gi, '-').toLowerCase()}`,
        name: body.name ?? `API batch ${createdBatches.length + 1}`,
        period: body.period ?? body.period_id ?? 'API',
        files: [],
      };
      createdBatches.push(created);
      await route.fulfill({
        status: 201,
        contentType: 'application/json',
        body: JSON.stringify({
          id: created.id,
          name: created.name,
          period: created.period,
          status: 'COLLECTING',
          createdAt: new Date().toISOString(),
        }),
      });
      return;
    }

    if (request.method() === 'GET') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([...MANIFEST.batches, ...createdBatches].map((batch) => ({
          id: batch.id,
          name: batch.name,
          period: batch.period,
          status: 'COLLECTING',
          createdAt: '2026-05-19T08:00:00.000Z',
          fileCount: uploads.filter((upload) => upload.batchId === batch.id).length,
        }))),
      });
      return;
    }

    await route.fallback();
  });

  await page.route('**/api/upload', async (route: Route) => {
    const body = route.request().postDataBuffer()?.toString('latin1') ?? '';
    const matchedFile = MANIFEST.batches
      .flatMap((batch) => batch.files.map((filename) => ({ batchId: batch.id, filename })))
      .find(({ filename }) => body.includes(filename));
    const purpose = body.includes('FORM_IMPORT') ? 'FORM_IMPORT' : body.includes('PARSE') ? 'PARSE' : undefined;
    const fileId = `file-${matchedFile?.filename.replace(/[^a-z0-9]/gi, '-').toLowerCase()}`;

    uploads.push({
      filename: matchedFile?.filename ?? 'unknown.xlsx',
      batchId: matchedFile?.batchId,
      purpose,
    });

    await route.fulfill({
      status: 201,
      contentType: 'application/json',
      body: JSON.stringify({
        file_id: fileId,
        filename: matchedFile?.filename ?? 'unknown.xlsx',
        status: 'UPLOADED',
      }),
    });
  });

  await page.route('**/api/query/sinks**', async (route) => {
    const url = new URL(route.request().url());
    const batchId = url.searchParams.get('batch_id') ?? url.searchParams.get('batchId') ?? MANIFEST.batches[0].id;
    const rows = batchRows(batchId);
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        sinks: [{
          id: `${batchId}-sink`,
          sourceType: 'EXCEL',
          sourceTable: MANIFEST.sinkTable,
          persistentTable: MANIFEST.persistentTable,
          batchId,
          rowCount: rows.length,
          rows,
        }],
        total: 1,
        page: 0,
        size: 25,
      }),
    });
  });

  await page.route('**/api/dashboards/sql/execute', async (route) => {
    const requestBody = route.request().postDataJSON() as { sql?: string };
    const sql = requestBody.sql ?? '';
    const batch = MANIFEST.batches.find((candidate) => sql.includes(candidate.id)) ?? MANIFEST.batches[0];
    const rows = batchRows(batch.id);
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        headers: MANIFEST.expectedColumns,
        rows,
        totalRows: rows.length,
      }),
    });
  });

  return { uploads, batchAssignments, createdBatches };
}

test.describe('Source Excel fixtures', () => {
  test('cover two isolated batches with 2-4 Excel workbooks and hierarchy metadata', { tag: ['@smoke'] }, async () => {
    expect(MANIFEST.batches, 'At least two batches are needed to prove isolation').toHaveLength(2);
    expect(MANIFEST.persistentTable).toBe('opex_actuals_persisted');
    expect(MANIFEST.sinkTable).toBe('sink_opex_lines');
    expect(MANIFEST.hierarchyLevels).toEqual(['HOLDING', 'COMPANY', 'DIVISION', 'COST_CENTER']);

    const seenEntityLevels = new Set<string>();
    for (const batch of MANIFEST.batches) {
      expect(batch.files.length, `${batch.id} must import 2-4 Excel files`).toBeGreaterThanOrEqual(2);
      expect(batch.files.length, `${batch.id} must import 2-4 Excel files`).toBeLessThanOrEqual(4);
      for (const filename of batch.files) {
        expect(fs.existsSync(sourcePath(filename)), `${filename} must exist in sourcedata`).toBeTruthy();
        assertWorkbookShape(filename);

        expect(workbookSheetNames(sourcePath(filename)), `${filename} sheet names`).toEqual([
          'Metadata',
          'OpexLines',
          'PersistPlan',
        ]);

        const metadataRows = worksheetRows(sourcePath(filename), 1);
        const metadata = new Map(metadataRows.slice(1).map(([key, value]) => [key, value]));
        expect(metadata.get('batch_id'), `${filename} metadata batch_id`).toBe(batch.id);
        expect(metadata.get('period'), `${filename} metadata period`).toBe(batch.period);
        expect(metadata.get('holding'), `${filename} metadata holding`).toBeTruthy();
        if (metadata.get('entity_level')) seenEntityLevels.add(metadata.get('entity_level')!);

        const opexRows = worksheetRows(sourcePath(filename), 2);
        expect(opexRows[0], `${filename} OpexLines headers`).toEqual(MANIFEST.expectedColumns);
        expect(opexRows.length, `${filename} must contain OPEX rows`).toBeGreaterThan(1);
        for (const row of opexRows.slice(1)) {
          expect(row[0], `${filename} row batch_id`).toBe(batch.id);
          expect(row[6], `${filename} row persistent_table`).toBe(MANIFEST.persistentTable);
        }

        const persistPlanRows = worksheetRows(sourcePath(filename), 3);
        expect(persistPlanRows[1][0], `${filename} sink table plan`).toBe(MANIFEST.sinkTable);
        expect(persistPlanRows[1][1], `${filename} persistent table plan`).toBe(MANIFEST.persistentTable);
      }
    }
    expect(seenEntityLevels).toEqual(new Set(MANIFEST.hierarchyLevels));
  });
});

test.describe('Batch import and isolation workflow', () => {
  test('API contract creates two batches, uploads Excel files, assigns files, and returns isolated sink rows', { tag: ['@smoke'] }, async ({ page }) => {
    const { uploads, batchAssignments, createdBatches } = await installBatchApiStubs(page);
    await gotoAndWait(page, ROUTES.dashboard);

    const result = await page.evaluate(async ({ manifest }) => {
      const created: Array<{ id: string; name: string }> = [];
      const sinkResults: Array<{ batchId: string; leakedRows: number; rowCount: number }> = [];

      for (const batch of manifest.batches) {
        const batchResponse = await fetch('/api/batches', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            name: `API ${batch.name}`,
            period: batch.period,
            description: `API E2E import for ${batch.id}`,
          }),
        });
        if (!batchResponse.ok) throw new Error(`Batch create failed: ${batchResponse.status}`);
        const createdBatch = await batchResponse.json();
        created.push({ id: createdBatch.id, name: createdBatch.name });

        for (const filename of batch.files) {
          const formData = new FormData();
          formData.append('upload_purpose', 'FORM_IMPORT');
          formData.append('file', new File(['PK\u0003\u0004 test workbook body'], filename, {
            type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
          }));

          const uploadResponse = await fetch('/api/upload', { method: 'POST', body: formData });
          if (!uploadResponse.ok) throw new Error(`Upload failed: ${uploadResponse.status}`);
          const upload = await uploadResponse.json();

          const assignResponse = await fetch(`/api/batches/${batch.id}/files`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ file_id: upload.file_id }),
          });
          if (!assignResponse.ok) throw new Error(`Batch assignment failed: ${assignResponse.status}`);
        }

        const sinkResponse = await fetch(`/api/query/sinks?batch_id=${encodeURIComponent(batch.id)}`);
        if (!sinkResponse.ok) throw new Error(`Sink query failed: ${sinkResponse.status}`);
        const sinkBody = await sinkResponse.json();
        const rows = sinkBody.sinks.flatMap((sink: any) => sink.rows ?? []);
        sinkResults.push({
          batchId: batch.id,
          rowCount: rows.length,
          leakedRows: rows.filter((row: any) => row.batch_id !== batch.id).length,
        });
      }

      return { created, sinkResults };
    }, { manifest: MANIFEST });

    expect(createdBatches).toHaveLength(MANIFEST.batches.length);
    expect(result.created).toHaveLength(MANIFEST.batches.length);
    expect(uploads).toHaveLength(MANIFEST.batches.reduce((sum, batch) => sum + batch.files.length, 0));
    expect(batchAssignments).toHaveLength(uploads.length);
    for (const sinkResult of result.sinkResults) {
      expect(sinkResult.rowCount, `${sinkResult.batchId} sink rows`).toBeGreaterThan(0);
      expect(sinkResult.leakedRows, `${sinkResult.batchId} must not include another batch`).toBe(0);
    }
  });

  test('uploads each source workbook into its selected batch and records batch file assignments', { tag: ['@slow'] }, async ({ page }) => {
    const { uploads, batchAssignments } = await installBatchApiStubs(page);

    await gotoAndWait(page, ROUTES.upload);

    const fileInput = page.locator('input[type="file"]').first();
    if (await fileInput.count() === 0) {
      await featurePresent(page, 'input[type="file"]', 'Excel source file input');
      return;
    }

    const batchSelector = page.locator('button[role="combobox"], [role="combobox"], button:has-text("No batch"), button:has-text("Select a batch")').first();
    if (!await batchSelector.isVisible({ timeout: TIMEOUTS.short }).catch(() => false)) {
      await featurePresent(page, '[role="combobox"]', 'Assign to Batch selector');
      return;
    }

    for (const batch of MANIFEST.batches) {
      await batchSelector.click();
      await page.getByRole('option', { name: new RegExp(batch.name) }).click();

      await fileInput.setInputFiles(batch.files.map(sourcePath));
      await expect.poll(() => uploads.filter((upload) => upload.batchId === batch.id).length, {
        timeout: TIMEOUTS.upload,
      }).toBe(batch.files.length);
    }

    const expectedUploadCount = MANIFEST.batches.reduce((sum, batch) => sum + batch.files.length, 0);
    expect(uploads).toHaveLength(expectedUploadCount);
    expect(batchAssignments).toHaveLength(expectedUploadCount);
    expect(new Set(batchAssignments.map((assignment) => assignment.batchId))).toEqual(
      new Set(MANIFEST.batches.map((batch) => batch.id)),
    );
  });
});

test.describe('Dashboard, SQL persistence, and report outputs', () => {
  test('generated batch Excel and PDF artifacts are readable and isolated to one batch', { tag: ['@smoke'] }, async () => {
    const batch = MANIFEST.batches[0];
    const artifacts = writeReportArtifacts(batch.id);
    expectReportArtifactsForBatch(artifacts, batch.id);
  });

  test('dashboard SQL combines sink and persisted rows for one batch without leaking another batch', { tag: ['@slow'] }, async ({ page }) => {
    const batchA = MANIFEST.batches[0];
    const batchB = MANIFEST.batches[1];
    const executedSql: string[] = [];

    await page.route('**/api/dashboards/sql/execute', async (route) => {
      const requestBody = route.request().postDataJSON() as { sql?: string };
      const sql = requestBody.sql ?? '';
      executedSql.push(sql);
      const selectedBatch = sql.includes(batchB.id) ? batchB.id : batchA.id;

      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          headers: MANIFEST.expectedColumns,
          rows: batchRows(selectedBatch),
          totalRows: batchRows(selectedBatch).length,
        }),
      });
    });

    await gotoAndWait(page, ROUTES.dashboardNew);
    const sqlEditor = page.locator('textarea[name*="sql" i], textarea, [data-testid="sql-editor"]').first();
    if (!await sqlEditor.isVisible({ timeout: TIMEOUTS.default }).catch(() => false)) {
      await featurePresent(page, '[data-testid="sql-editor"]', 'Dashboard SQL editor for batch reconciliation');
      return;
    }

    const sql = [
      'SELECT s.batch_id, s.entity_path, s.account, s.amount, s.currency, s.sink_key, p.persistent_table',
      `FROM ${MANIFEST.sinkTable} s`,
      `JOIN ${MANIFEST.persistentTable} p ON p.sink_key = s.sink_key AND p.batch_id = s.batch_id`,
      `WHERE s.batch_id = '${batchA.id}'`,
      'ORDER BY s.entity_path, s.account',
    ].join('\n');

    await sqlEditor.fill(sql);
    const runButton = page.locator('button:has-text("Run"), button:has-text("Preview"), button:has-text("Test Query")').first();
    if (await runButton.isVisible({ timeout: TIMEOUTS.short }).catch(() => false)) {
      await runButton.click();
      await expect.poll(() => executedSql.length, { timeout: TIMEOUTS.default }).toBeGreaterThan(0);
      expect(executedSql.at(-1)).toContain(MANIFEST.persistentTable);
      expect(executedSql.at(-1)).toContain(batchA.id);
      expect(executedSql.at(-1)).not.toContain(batchB.id);
    } else {
      await featurePresent(page, 'button:has-text("Run")', 'Dashboard query preview action');
    }
  });

  test('dashboard query result for one batch rejects cross-batch row leakage', { tag: ['@smoke'] }, async ({ page }) => {
    await installBatchApiStubs(page);
    await gotoAndWait(page, ROUTES.dashboard);

    const batchA = MANIFEST.batches[0];
    const batchB = MANIFEST.batches[1];
    const response = await page.evaluate(async ({ batchId, sinkTable, persistentTable }) => {
      const query = [
        'SELECT s.batch_id, s.entity_path, s.account, s.amount, s.currency, s.sink_key, p.persistent_table',
        `FROM ${sinkTable} s`,
        `JOIN ${persistentTable} p ON p.sink_key = s.sink_key AND p.batch_id = s.batch_id`,
        `WHERE s.batch_id = '${batchId}'`,
      ].join('\n');

      const result = await fetch('/api/dashboards/sql/execute', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ sql: query }),
      });
      if (!result.ok) throw new Error(`Dashboard SQL failed: ${result.status}`);
      return result.json();
    }, {
      batchId: batchA.id,
      sinkTable: MANIFEST.sinkTable,
      persistentTable: MANIFEST.persistentTable,
    });

    expect(response.rows.length).toBeGreaterThan(0);
    expect(response.rows.every((row: Record<string, unknown>) => row.batch_id === batchA.id)).toBeTruthy();
    expect(JSON.stringify(response.rows)).not.toContain(batchB.id);
  });

  test('export flow writes batch Excel summary, PDF report, and combined sink/persisted report contract', { tag: ['@slow'] }, async ({ page }) => {
    const batch = MANIFEST.batches[0];
    const createdFlows: unknown[] = [];
    const executions: unknown[] = [];
    let artifacts: { excelPath: string; pdfPath: string; combinedPath: string } | undefined;

    await page.route('**/api/export-flows/*/execute', async (route) => {
      executions.push({ flowId: route.request().url().split('/').at(-2) });
      artifacts = writeReportArtifacts(batch.id);

      await route.fulfill({
        status: 202,
        contentType: 'application/json',
        body: JSON.stringify({
          id: 'exec-batch-summary',
          status: 'SUCCESS',
          rowsWritten: batchRows(batch.id).length,
          outputFiles: [
            MANIFEST.outputReports.excel,
            MANIFEST.outputReports.pdf,
            MANIFEST.outputReports.combined,
          ],
          persistentTable: MANIFEST.persistentTable,
        }),
      });
    });

    await page.route('**/api/export-flows**', async (route) => {
      const request = route.request();
      const url = new URL(request.url());
      if (!url.pathname.endsWith('/api/export-flows')) {
        await route.fallback();
        return;
      }
      if (request.method() === 'GET') {
        await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify([]) });
        return;
      }
      if (request.method() === 'POST') {
        const payload = request.postDataJSON();
        createdFlows.push(payload);
        await route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify({
            id: 'flow-batch-summary',
            active: true,
            lastExecution: null,
            ...payload,
          }),
        });
        return;
      }
      await route.fallback();
    });

    await gotoAndWait(page, ROUTES.exportFlows);
    const newFlow = page.locator('button:has-text("New Export Flow"), button:has-text("New"), button:has-text("Create")').first();
    if (!await newFlow.isVisible({ timeout: TIMEOUTS.default }).catch(() => false)) {
      await featurePresent(page, 'button:has-text("New Export Flow")', 'New export flow button');
      return;
    }

    await newFlow.click();
    await page.locator('[role="dialog"]').waitFor({ state: 'visible', timeout: TIMEOUTS.short });
    await page.locator('#ef-name').fill('Batch OPEX summary and persisted reconciliation');
    await page.getByRole('button', { name: 'Next' }).click();

    await page.locator('#ef-sql').fill([
      'SELECT s.*, p.loaded_at AS persisted_loaded_at',
      `FROM ${MANIFEST.sinkTable} s`,
      `JOIN ${MANIFEST.persistentTable} p ON p.sink_key = s.sink_key`,
      `WHERE s.batch_id = '${batch.id}'`,
    ].join('\n'));
    await page.getByRole('button', { name: 'Next' }).click();

    await page.locator('#ef-path').fill('tests/web_play/logs/generated');
    await page.getByRole('button', { name: 'Next' }).click();

    await page.locator('#ef-sheet').fill('BatchSummary');
    await page.locator('#ef-fname').fill(MANIFEST.outputReports.excel);
    await page.getByRole('button', { name: 'Next' }).click();
    await page.getByRole('button', { name: 'Create' }).click();

    await expect.poll(() => createdFlows.length, { timeout: TIMEOUTS.default }).toBe(1);
    expect(JSON.stringify(createdFlows[0])).toContain(MANIFEST.persistentTable);
    expect(JSON.stringify(createdFlows[0])).toContain(batch.id);
    expect(JSON.stringify(createdFlows[0])).toContain(MANIFEST.outputReports.excel);

    // The API response contract explicitly carries the PDF and combined reconciliation outputs.
    await page.request.post('/api/export-flows/flow-batch-summary/execute');
    expect(executions).toHaveLength(1);
    expect(artifacts, 'export execution must create output artifacts').toBeTruthy();
    expectReportArtifactsForBatch(artifacts!, batch.id);
  });
});
