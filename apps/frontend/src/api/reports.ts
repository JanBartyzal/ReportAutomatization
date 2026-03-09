import apiClient from './axios';
import type {
  Report,
  ReportMatrix,
  StatusTransition,
  BulkOperationResult,
  PaginatedResponse,
  PaginationParams,
} from '@reportplatform/types';

export interface ReportListParams extends PaginationParams {
  org_id?: string;
  period_id?: string;
  status?: string;
}

export async function listReports(params: ReportListParams = {}): Promise<PaginatedResponse<Report>> {
  const { data } = await apiClient.get<PaginatedResponse<Report>>('/reports', { params });
  return data;
}

export async function createReport(body: { org_id: string; period_id: string; report_type: string }): Promise<Report> {
  const { data } = await apiClient.post<Report>('/reports', body);
  return data;
}

export async function getReport(reportId: string): Promise<Report> {
  const { data } = await apiClient.get<Report>(`/reports/${reportId}`);
  return data;
}

export async function submitReport(reportId: string): Promise<Report> {
  const { data } = await apiClient.post<Report>(`/reports/${reportId}/submit`);
  return data;
}

export async function reviewReport(reportId: string): Promise<Report> {
  const { data } = await apiClient.post<Report>(`/reports/${reportId}/review`);
  return data;
}

export async function approveReport(reportId: string): Promise<Report> {
  const { data } = await apiClient.post<Report>(`/reports/${reportId}/approve`);
  return data;
}

export async function rejectReport(reportId: string, comment: string): Promise<Report> {
  const { data } = await apiClient.post<Report>(`/reports/${reportId}/reject`, { comment });
  return data;
}

export async function getReportHistory(reportId: string): Promise<StatusTransition[]> {
  const { data } = await apiClient.get<StatusTransition[]>(`/reports/${reportId}/history`);
  return data;
}

export async function bulkApprove(reportIds: string[]): Promise<BulkOperationResult> {
  const { data } = await apiClient.post<BulkOperationResult>('/reports/bulk-approve', { report_ids: reportIds });
  return data;
}

export async function bulkReject(reportIds: string[], comment: string): Promise<BulkOperationResult> {
  const { data } = await apiClient.post<BulkOperationResult>('/reports/bulk-reject', { report_ids: reportIds, comment });
  return data;
}

export async function getReportMatrix(): Promise<ReportMatrix> {
  const { data } = await apiClient.get<ReportMatrix>('/reports/matrix');
  return data;
}
