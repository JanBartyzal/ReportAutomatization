import apiClient from './axios';
import type {
  FileUploadResponse,
  FileDetails,
  FileListParams,
  PaginatedResponse,
  UploadPurpose,
  FileContent,
  FileContentType,
  ExcelSheet,
  PptxSlide,
  ExtractedTable,
} from '@reportplatform/types';

export async function uploadFile(
  file: File,
  purpose: UploadPurpose = 'PARSE' as UploadPurpose,
  onUploadProgress?: (progressEvent: { loaded: number; total?: number }) => void,
): Promise<FileUploadResponse> {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('upload_purpose', purpose);

  const { data } = await apiClient.post<FileUploadResponse>('/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress,
  });
  return data;
}

export async function listFiles(params: FileListParams = {}): Promise<PaginatedResponse<FileDetails>> {
  const { data } = await apiClient.get<PaginatedResponse<FileDetails>>('/files', { params });
  return data;
}

export async function getFile(fileId: string): Promise<FileDetails> {
  const { data } = await apiClient.get<FileDetails>(`/files/${fileId}`);
  return data;
}

// ---- Internal backend DTO shapes from /api/query/ ----

interface TextBlockDto {
  shapeName?: string;
  shape_name?: string;
  text?: string;
  isTitle?: boolean;
  is_title?: boolean;
}

interface TableRowDto {
  cells?: string[];
}

interface TableDataDto_Slide {
  tableId?: string;
  table_id?: string;
  headers?: string[];
  rows?: TableRowDto[] | string[][];
  confidence?: number;
}

interface SlideDto {
  slideIndex: number;
  title: string;
  texts: (TextBlockDto | string)[];
  tables: TableDataDto_Slide[];
  imageUrl: string;
  notes: string;
}

interface SlideDataResponse {
  fileId: string;
  filename: string;
  slides: SlideDto[];
}

interface TableDataDto {
  id: string;
  fileId: string;
  sourceSheet: string;
  headers: unknown;
  rows: unknown;
  metadata: unknown;
}

interface FileDataResponse {
  fileId: string;
  filename: string;
  mimeType: string;
  tables: TableDataDto[];
  documents: unknown[];
}

// ---- Row conversion helpers ----

/**
 * Convert PPTX table rows from [{cells: [...]}] or [[...]] to Record<header, value>[].
 */
function cellsToRecords(rows: unknown, headers: string[]): Record<string, unknown>[] {
  if (!Array.isArray(rows)) return [];
  return rows.map(row => {
    let cells: string[] = [];
    if (Array.isArray(row)) {
      // Plain string array: ["v1", "v2"]
      cells = row.map(String);
    } else if (row && typeof row === 'object') {
      const r = row as Record<string, unknown>;
      if (Array.isArray(r['cells'])) {
        // SheetRow/TableRow shape: {cells: ["v1", "v2"]} or {row_index, cells}
        cells = (r['cells'] as unknown[]).map(String);
      } else {
        // Already a header-keyed object
        return r as Record<string, unknown>;
      }
    }
    const record: Record<string, unknown> = {};
    headers.forEach((h, i) => { record[h || `col${i}`] = cells[i] ?? ''; });
    return record;
  });
}

/**
 * Safely parse headers: handles string[] or JSON string.
 */
function parseHeaders(h: unknown): string[] {
  if (Array.isArray(h)) return h.map(String);
  if (typeof h === 'string') {
    try { const parsed = JSON.parse(h); if (Array.isArray(parsed)) return parsed.map(String); } catch {}
  }
  return [];
}

/**
 * Safely parse rows: handles array or JSON string.
 */
function parseRows(r: unknown): unknown[] {
  if (Array.isArray(r)) return r;
  if (typeof r === 'string') {
    try { const parsed = JSON.parse(r); if (Array.isArray(parsed)) return parsed; } catch {}
  }
  return [];
}

// ---- Mapping helpers ----

function mapSlideDataToContent(data: SlideDataResponse): FileContent {
  const slides: PptxSlide[] = (data.slides ?? []).map(s => {
    // Extract plain text from TextBlock objects
    const textParts: string[] = [];
    if (s.title) textParts.push(s.title);
    for (const t of s.texts ?? []) {
      if (typeof t === 'string') {
        textParts.push(t);
      } else {
        const text = (t as TextBlockDto).text;
        if (text) textParts.push(text);
      }
    }

    const tables: ExtractedTable[] = (s.tables ?? []).map((tbl, i) => {
      const headers = parseHeaders(tbl.headers);
      const rawRows = parseRows(tbl.rows);
      const rows = cellsToRecords(rawRows, headers);
      return {
        table_id: tbl.tableId ?? tbl.table_id ?? `slide-${s.slideIndex}-tbl-${i}`,
        source_type: 'PPTX' as unknown as FileContentType,
        source_page: s.slideIndex,
        headers,
        rows,
        row_count: rows.length,
      };
    });

    return {
      slide_number: s.slideIndex,
      text: textParts.join('\n'),
      speaker_notes: s.notes || undefined,
      tables,
    };
  });

  return {
    file_id: data.fileId,
    content_type: 'PPTX' as unknown as FileContentType,
    slides,
    tables: slides.flatMap(s => s.tables),
    metadata: {
      total_pages: slides.length,
    },
  };
}

function mapFileDataToContent(data: FileDataResponse): FileContent {
  // Group tables by sourceSheet
  const sheetMap = new Map<string, TableDataDto[]>();
  for (const table of data.tables ?? []) {
    const key = table.sourceSheet || 'Sheet1';
    if (!sheetMap.has(key)) sheetMap.set(key, []);
    sheetMap.get(key)!.push(table);
  }

  const sheets: ExcelSheet[] = Array.from(sheetMap.entries()).map(([sheetName, tables]) => {
    const firstTable = tables[0];
    const headers = parseHeaders(firstTable?.headers);
    const rawRows = parseRows(firstTable?.rows);
    const rows = cellsToRecords(rawRows, headers);
    return {
      sheet_name: sheetName,
      headers,
      rows,
      row_count: rows.length,
    };
  });

  const extractedTables: ExtractedTable[] = (data.tables ?? []).map(t => {
    const headers = parseHeaders(t.headers);
    const rawRows = parseRows(t.rows);
    const rows = cellsToRecords(rawRows, headers);
    return {
      table_id: t.id,
      source_type: 'EXCEL' as unknown as FileContentType,
      source_sheet: t.sourceSheet,
      headers,
      rows,
      row_count: rows.length,
    };
  });

  const totalRows = sheets.reduce((sum, s) => sum + s.row_count, 0);

  return {
    file_id: data.fileId,
    content_type: data.mimeType?.includes('csv')
      ? ('CSV' as unknown as FileContentType)
      : ('EXCEL' as unknown as FileContentType),
    sheets: sheets.length > 0 ? sheets : undefined,
    tables: extractedTables,
    metadata: {
      total_sheets: sheetMap.size || undefined,
      total_rows: totalRows || undefined,
    },
  };
}

// ---- Public API functions ----

/**
 * Get extracted content for a file.
 * Routes to the correct query-service endpoint based on MIME type:
 *   - PPTX  → GET /api/query/files/{id}/slides  → SlideDataResponse
 *   - Other → GET /api/query/files/{id}/data    → FileDataResponse (Excel/CSV/PDF)
 */
export async function getFileContent(fileId: string, mimeType?: string): Promise<FileContent> {
  const isPptx = mimeType?.includes('presentationml');

  if (isPptx) {
    const { data } = await apiClient.get<SlideDataResponse>(`/query/files/${fileId}/slides`);
    return mapSlideDataToContent(data);
  }

  const { data } = await apiClient.get<FileDataResponse>(`/query/files/${fileId}/data`);
  return mapFileDataToContent(data);
}

/** Get extracted tables from a file */
export async function getFileTables(fileId: string): Promise<FileContent['tables']> {
  const { data } = await apiClient.get<FileDataResponse>(`/query/files/${fileId}/data`);
  return mapFileDataToContent(data).tables;
}

/** Trigger reprocessing of a file through the orchestration pipeline */
export async function reprocessFile(fileId: string): Promise<{ status: string; file_id: string }> {
  const { data } = await apiClient.post<{ status: string; file_id: string }>(`/files/${fileId}/reprocess`);
  return data;
}
