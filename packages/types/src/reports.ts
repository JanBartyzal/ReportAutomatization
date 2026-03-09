/** Report status in lifecycle */
export enum ReportStatus {
  DRAFT = 'DRAFT',
  SUBMITTED = 'SUBMITTED',
  UNDER_REVIEW = 'UNDER_REVIEW',
  APPROVED = 'APPROVED',
  REJECTED = 'REJECTED',
}

/** Report entity */
export interface Report {
  id: string;
  org_id: string;
  period_id: string;
  report_type: string;
  status: ReportStatus;
  created_at: string;
  updated_at: string;
}

/** Status transition in report history */
export interface StatusTransition {
  from_status: ReportStatus;
  to_status: ReportStatus;
  changed_by: string;
  comment?: string;
  changed_at: string;
}

/** Holding admin report matrix */
export interface ReportMatrix {
  rows: MatrixRow[];
}

export interface MatrixRow {
  org_id: string;
  org_name: string;
  periods: Record<string, ReportStatus>;
}

/** Bulk operation result */
export interface BulkOperationResult {
  succeeded: string[];
  failed: BulkOperationError[];
}

export interface BulkOperationError {
  report_id: string;
  error: string;
}
