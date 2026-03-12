import type { ProcessingStatus, PaginationParams } from './common';

/** Purpose of file upload */
export enum UploadPurpose {
  PARSE = 'PARSE',
  FORM_IMPORT = 'FORM_IMPORT',
}

/** File status in the system */
export enum FileStatus {
  UPLOADED = 'UPLOADED',
  PROCESSING = 'PROCESSING',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED',
  PARTIAL = 'PARTIAL',
}

/** Response from POST /api/upload */
export interface FileUploadResponse {
  file_id: string;
  filename: string;
  size_bytes: number;
  mime_type: string;
  status: FileStatus;
  blob_url: string;
}

/** File details from GET /api/files/{file_id} */
export interface FileDetails {
  file_id: string;
  filename: string;
  size_bytes: number;
  mime_type: string;
  status: FileStatus;
  blob_url: string;
  uploaded_at: string;
  org_id: string;
  workflow_status?: WorkflowStatus;
}

export interface WorkflowStatus {
  workflow_id: string;
  status: string;
  steps: WorkflowStep[];
  started_at: string;
  completed_at?: string;
  error_detail?: string;
}

export interface WorkflowStep {
  step_name: string;
  status: string;
  duration_ms: number;
  error_detail?: string;
  started_at: string;
  completed_at?: string;
}

/** Query parameters for listing files */
export interface FileListParams extends PaginationParams {
  status?: FileStatus;
  mime_type?: string;
  sort_by?: 'filename' | 'size_bytes' | 'uploaded_at';
  sort_order?: 'asc' | 'desc';
}

export enum FileContentType {
  EXCEL = 'EXCEL',
  PDF = 'PDF',
  CSV = 'CSV',
  PPTX = 'PPTX',
}

/** Column data type information */
export interface ColumnDataType {
  col_index: number;
  column_name: string;
  detected_type: 'STRING' | 'NUMBER' | 'DATE' | 'CURRENCY' | 'PERCENTAGE';
}

/** Excel sheet content */
export interface ExcelSheet {
  sheet_name: string;
  headers: string[];
  rows: Record<string, unknown>[];
  row_count: number;
  data_types?: ColumnDataType[];
}

/** PDF page content */
export interface PdfPage {
  page_number: number;
  text: string;
  tables: ExtractedTable[];
  ocr_confidence?: number;
  is_ocr: boolean;
}

/** CSV content */
export interface CsvContent {
  headers: string[];
  rows: Record<string, unknown>[];
  delimiter: string;
  encoding: string;
  row_count: number;
  data_types?: ColumnDataType[];
}

/** PPTX slide content */
export interface PptxSlide {
  slide_number: number;
  text: string;
  speaker_notes?: string;
  tables: ExtractedTable[];
}

/** Extracted table from any source */
export interface ExtractedTable {
  table_id: string;
  source_type: FileContentType;
  source_page?: number;
  source_sheet?: string;
  headers: string[];
  rows: Record<string, unknown>[];
  row_count: number;
  data_types?: ColumnDataType[];
}

/** Full extracted file content */
export interface FileContent {
  file_id: string;
  content_type: FileContentType;
  sheets?: ExcelSheet[];
  pages?: PdfPage[];
  csv?: CsvContent;
  slides?: PptxSlide[];
  tables: ExtractedTable[];
  metadata: {
    total_pages?: number;
    total_sheets?: number;
    total_rows?: number;
    has_ocr?: boolean;
    ocr_languages?: string[];
  };
}
