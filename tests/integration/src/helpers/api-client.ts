import axios, { AxiosInstance, AxiosResponse } from 'axios';

// ---- Response types (mirrors packages/types) ----

export interface FileUploadResponse {
  file_id: string;
  filename: string;
  size_bytes: number;
  mime_type: string;
  status: string;
  blob_url: string;
}

export interface FileDetails {
  file_id: string;
  filename: string;
  size_bytes: number;
  mime_type: string;
  status: string;
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

export interface FileListResponse {
  items: FileDetails[];
  total: number;
  page: number;
  page_size: number;
}

export interface ProcessingStatusResponse {
  file_id: string;
  status: string;
  progress_percent: number;
  current_step?: string;
  error_detail?: string;
}

export interface ParsedReport {
  file_id: string;
  tables: unknown[];
  documents: unknown[];
}

export interface ReportListResponse {
  items: ParsedReport[];
  total: number;
  page: number;
  page_size: number;
}

export interface DashboardConfig {
  id?: string;
  name: string;
  description?: string;
  is_public: boolean;
  widgets: unknown[];
  created_at?: string;
}

export interface DashboardSummary {
  id: string;
  name: string;
  is_public: boolean;
  created_at: string;
}

export interface DashboardListResponse {
  items: DashboardSummary[];
  total: number;
  page: number;
  page_size: number;
}

export interface ReportLifecycleStatus {
  report_id: string;
  status: string;
  submitted_by?: string;
  submitted_at?: string;
  reviewed_by?: string;
  reviewed_at?: string;
  approved_by?: string;
  approved_at?: string;
}

export interface AuthValidateResponse {
  valid: boolean;
  user_id?: string;
  roles?: string[];
  org_id?: string;
}

export interface LifecycleActionResponse {
  report_id: string;
  status: string;
  message?: string;
}

// ---- Query parameters ----

export interface FileListParams {
  page?: number;
  page_size?: number;
  status?: string;
  mime_type?: string;
  sort_by?: string;
  sort_order?: 'asc' | 'desc';
}

export interface ReportQueryParams {
  page?: number;
  page_size?: number;
  org_id?: string;
  period?: string;
  source_type?: string;
}

export interface DashboardListParams {
  page?: number;
  page_size?: number;
}

/**
 * Typed API client wrapping axios for the ReportPlatform gateway.
 * All requests go through the Nginx gateway (http://localhost by default).
 */
export class ApiClient {
  private readonly http: AxiosInstance;

  constructor(baseURL: string, authToken?: string) {
    this.http = axios.create({
      baseURL,
      timeout: 30_000,
      headers: {
        'Content-Type': 'application/json',
        ...(authToken ? { Authorization: `Bearer ${authToken}` } : {}),
        'X-Test-Auth': 'true',
      },
      // Do not throw on 4xx/5xx so tests can assert status codes
      validateStatus: () => true,
    });
  }

  /** Replace the auth token for subsequent requests. */
  setToken(token: string): void {
    this.http.defaults.headers.common['Authorization'] = `Bearer ${token}`;
  }

  /** Remove auth token. */
  clearToken(): void {
    delete this.http.defaults.headers.common['Authorization'];
  }

  // ---------- Auth ----------

  async validateToken(token?: string): Promise<AxiosResponse<AuthValidateResponse>> {
    const headers: Record<string, string> = {};
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }
    return this.http.post<AuthValidateResponse>('/api/auth/validate', {}, { headers });
  }

  // ---------- Files ----------

  async uploadFile(
    file: Buffer,
    filename: string,
    mimeType: string,
  ): Promise<AxiosResponse<FileUploadResponse>> {
    const formData = new FormData();
    const blob = new Blob([file], { type: mimeType });
    formData.append('file', blob, filename);

    return this.http.post<FileUploadResponse>('/api/files/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  }

  async listFiles(params?: FileListParams): Promise<AxiosResponse<FileListResponse>> {
    return this.http.get<FileListResponse>('/api/files', { params });
  }

  async getFile(fileId: string): Promise<AxiosResponse<FileDetails>> {
    return this.http.get<FileDetails>(`/api/files/${fileId}`);
  }

  async getFileStatus(fileId: string): Promise<AxiosResponse<ProcessingStatusResponse>> {
    return this.http.get<ProcessingStatusResponse>(`/api/files/${fileId}/status`);
  }

  // ---------- Query / Reports ----------

  async queryReports(params?: ReportQueryParams): Promise<AxiosResponse<ReportListResponse>> {
    return this.http.get<ReportListResponse>('/api/query/reports', { params });
  }

  async getReportDetail(reportId: string): Promise<AxiosResponse<ParsedReport>> {
    return this.http.get<ParsedReport>(`/api/query/reports/${reportId}`);
  }

  // ---------- Dashboards ----------

  async createDashboard(
    config: Omit<DashboardConfig, 'id' | 'created_at'>,
  ): Promise<AxiosResponse<DashboardConfig>> {
    return this.http.post<DashboardConfig>('/api/dashboards', config);
  }

  async listDashboards(params?: DashboardListParams): Promise<AxiosResponse<DashboardListResponse>> {
    return this.http.get<DashboardListResponse>('/api/dashboards', { params });
  }

  async getDashboard(dashboardId: string): Promise<AxiosResponse<DashboardConfig>> {
    return this.http.get<DashboardConfig>(`/api/dashboards/${dashboardId}`);
  }

  async deleteDashboard(dashboardId: string): Promise<AxiosResponse<void>> {
    return this.http.delete(`/api/dashboards/${dashboardId}`);
  }

  // ---------- Lifecycle ----------

  async submitReport(
    reportId: string,
    body?: { comment?: string },
  ): Promise<AxiosResponse<LifecycleActionResponse>> {
    return this.http.post<LifecycleActionResponse>(
      `/api/lifecycle/reports/${reportId}/submit`,
      body ?? {},
    );
  }

  async approveReport(
    reportId: string,
    body?: { comment?: string },
  ): Promise<AxiosResponse<LifecycleActionResponse>> {
    return this.http.post<LifecycleActionResponse>(
      `/api/lifecycle/reports/${reportId}/approve`,
      body ?? {},
    );
  }

  async rejectReport(
    reportId: string,
    body?: { comment?: string; reason?: string },
  ): Promise<AxiosResponse<LifecycleActionResponse>> {
    return this.http.post<LifecycleActionResponse>(
      `/api/lifecycle/reports/${reportId}/reject`,
      body ?? {},
    );
  }

  async getReportLifecycle(reportId: string): Promise<AxiosResponse<ReportLifecycleStatus>> {
    return this.http.get<ReportLifecycleStatus>(`/api/lifecycle/reports/${reportId}`);
  }

  // ---------- Raw request (escape hatch) ----------

  get raw(): AxiosInstance {
    return this.http;
  }
}
