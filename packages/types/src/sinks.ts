import type { PaginationParams } from './common';

/** FS25: Sink Browser types */

/** Correction types for sink data */
export type CorrectionType = 'CELL_VALUE' | 'HEADER_RENAME' | 'ROW_DELETE' | 'ROW_ADD' | 'COLUMN_TYPE';

/** Error categories for extraction learning */
export type ErrorCategory = 'MERGED_CELLS' | 'WRONG_HEADER' | 'MISSING_ROW' | 'VALUE_FORMAT' | 'SPLIT_TABLE';

/** Sink list item (summary for browser list view) */
export interface SinkListItem {
  id: string;
  fileId: string;
  sourceSheet: string;
  rowCount: number;
  columnCount: number;
  metadata: Record<string, unknown>;
  createdAt: string;
  correctionCount: number;
  hasSelections: boolean;
}

/** Paginated sink list response */
export interface SinkListResponse {
  sinks: SinkListItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  nextCursor: string | null;
}

/** Sink correction record */
export interface SinkCorrection {
  id: string;
  parsedTableId: string;
  rowIndex: number | null;
  colIndex: number | null;
  originalValue: string;
  correctedValue: string;
  correctionType: CorrectionType;
  correctedBy: string;
  correctedAt: string;
  metadata: Record<string, unknown>;
}

/** Sink selection record */
export interface SinkSelection {
  id: string;
  parsedTableId: string;
  periodId: string | null;
  reportType: string | null;
  selected: boolean;
  priority: number;
  selectedBy: string;
  selectedAt: string;
  note: string | null;
}

/** Full sink detail with corrections and selections */
export interface SinkDetail {
  id: string;
  fileId: string;
  sourceSheet: string;
  headers: string[];
  rows: string[][];
  metadata: Record<string, unknown>;
  createdAt: string;
  correctionCount: number;
  corrections: SinkCorrection[];
  selections: SinkSelection[];
  correctedHeaders: string[];
  correctedRows: string[][];
}

/** Request to create a correction */
export interface CreateCorrectionRequest {
  rowIndex: number | null;
  colIndex: number | null;
  originalValue: string;
  correctedValue: string;
  correctionType: CorrectionType;
  errorCategory?: ErrorCategory;
  metadata?: Record<string, unknown>;
}

/** Request to upsert a selection */
export interface UpsertSelectionRequest {
  periodId?: string;
  reportType?: string;
  selected: boolean;
  priority: number;
  note?: string;
}

/** Query params for sink list */
export interface SinkListParams extends PaginationParams {
  file_id?: string;
  source_sheet?: string;
}

/** Internal params with size alias (used by frontend hooks) */
export interface SinkListFilters {
  page?: number;
  size?: number;
  file_id?: string;
  source_sheet?: string;
}
