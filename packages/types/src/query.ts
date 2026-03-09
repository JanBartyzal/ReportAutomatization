import type { PaginationParams } from './common';

/** Parsed data for a file (tables + documents) */
export interface ParsedData {
  file_id: string;
  tables: TableData[];
  documents: DocumentSummary[];
}

/** Structured table data */
export interface TableData {
  table_id: string;
  source_sheet: string;
  headers: string[];
  rows: TableRow[];
  metadata: Record<string, string>;
}

export interface TableRow {
  cells: string[];
}

export interface DocumentSummary {
  document_id: string;
  document_type: string;
  content_preview: string;
}

/** Full document content */
export interface DocumentContent {
  document_id: string;
  file_id: string;
  document_type: string;
  content: string;
  metadata: Record<string, string>;
}

/** Slide content for PPTX viewer */
export interface SlideContent {
  slide_index: number;
  title: string;
  texts: TextBlock[];
  tables: SlideTable[];
  notes: string;
  image_url: string;
}

export interface TextBlock {
  shape_name: string;
  text: string;
  is_title: boolean;
}

export interface SlideTable {
  table_id: string;
  headers: string[];
  rows: TableRow[];
  confidence: number;
}

/** Processing log entry */
export interface ProcessingLog {
  log_id: string;
  step_name: string;
  status: string;
  duration_ms: number;
  error_detail?: string;
  recorded_at: string;
}

/** Query params for table data */
export interface TableQueryParams extends PaginationParams {
  org_id?: string;
  period?: string;
  source_type?: 'FILE' | 'FORM';
}
