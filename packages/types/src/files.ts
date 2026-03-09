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
}
