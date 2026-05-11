import apiClient from './axios';
import type {
  DrilldownReportCreateRequest,
  DrilldownReportDefinition,
  DrilldownReportQueryResponse,
  DrilldownReportUpdateRequest,
  DrilldownRequest,
  DrilldownResult,
} from '@reportplatform/types';

const BASE = '/drilldown-reports';

interface DrilldownReportListResponse {
  reports: DrilldownReportDefinition[];
}

export async function listDrilldownReports(): Promise<DrilldownReportDefinition[]> {
  const { data } = await apiClient.get<DrilldownReportListResponse>(BASE);
  return data.reports ?? [];
}

export async function getDrilldownReport(id: string): Promise<DrilldownReportDefinition> {
  const { data } = await apiClient.get<DrilldownReportDefinition>(`${BASE}/${id}`);
  return data;
}

export async function createDrilldownReport(
  payload: DrilldownReportCreateRequest
): Promise<DrilldownReportDefinition> {
  const { data } = await apiClient.post<DrilldownReportDefinition>(BASE, payload);
  return data;
}

export async function updateDrilldownReport(
  id: string,
  payload: DrilldownReportUpdateRequest
): Promise<DrilldownReportDefinition> {
  const { data } = await apiClient.put<DrilldownReportDefinition>(`${BASE}/${id}`, payload);
  return data;
}

export async function deleteDrilldownReport(id: string): Promise<void> {
  await apiClient.delete(`${BASE}/${id}`);
}

export async function queryDrilldownReport(
  id: string,
  filters: Record<string, unknown> = {}
): Promise<DrilldownReportQueryResponse> {
  const { data } = await apiClient.post<DrilldownReportQueryResponse>(`${BASE}/${id}/query`, {
    filters,
  });
  return data;
}

export async function drillDrilldownReport(
  id: string,
  payload: DrilldownRequest
): Promise<DrilldownResult> {
  const { data } = await apiClient.post<DrilldownResult>(`${BASE}/${id}/drill`, payload);
  return data;
}
