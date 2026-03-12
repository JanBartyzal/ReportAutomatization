/** Report status in lifecycle */
export enum ReportStatus {
  DRAFT = 'DRAFT',
  SUBMITTED = 'SUBMITTED',
  UNDER_REVIEW = 'UNDER_REVIEW',
  APPROVED = 'APPROVED',
  REJECTED = 'REJECTED',
  COMPLETED = 'COMPLETED',
}

/** Report scope */
export enum ReportScope {
  CENTRAL = 'CENTRAL',
  LOCAL = 'LOCAL',
  ALL = 'ALL',
}

/** Report entity */
export interface Report {
  id: string;
  org_id: string;
  period_id: string;
  report_type: string;
  status: ReportStatus;
  scope: ReportScope;
  locked: boolean;
  submitted_by?: string;
  submitted_at?: string;
  reviewed_by?: string;
  reviewed_at?: string;
  approved_by?: string;
  approved_at?: string;
  completed_by?: string;
  completed_at?: string;
  released_by?: string;
  released_at?: string;
  created_by: string;
  created_at: string;
  updated_at: string;
}

/** Report creation request */
export interface ReportCreateRequest {
  org_id: string;
  period_id: string;
  report_type: string;
  scope?: ReportScope;
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
  scope: ReportScope;
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
