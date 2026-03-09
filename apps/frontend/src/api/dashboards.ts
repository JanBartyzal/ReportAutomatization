import apiClient from './axios';
import type {
  DashboardConfig,
  DashboardSummary,
  DashboardQueryParams,
  AggregatedData,
  PeriodComparisonRequest,
  ComparisonData,
} from '@reportplatform/types';

export async function listDashboards(): Promise<DashboardSummary[]> {
  const { data } = await apiClient.get<DashboardSummary[]>('/dashboards');
  return data;
}

export async function createDashboard(config: DashboardConfig): Promise<DashboardConfig> {
  const { data } = await apiClient.post<DashboardConfig>('/dashboards', config);
  return data;
}

export async function getDashboard(dashboardId: string): Promise<DashboardConfig> {
  const { data } = await apiClient.get<DashboardConfig>(`/dashboards/${dashboardId}`);
  return data;
}

export async function updateDashboard(dashboardId: string, config: DashboardConfig): Promise<DashboardConfig> {
  const { data } = await apiClient.put<DashboardConfig>(`/dashboards/${dashboardId}`, config);
  return data;
}

export async function deleteDashboard(dashboardId: string): Promise<void> {
  await apiClient.delete(`/dashboards/${dashboardId}`);
}

export async function executeDashboardQuery(dashboardId: string, params: DashboardQueryParams): Promise<AggregatedData> {
  const { data } = await apiClient.post<AggregatedData>(`/dashboards/${dashboardId}/data`, params);
  return data;
}

export async function comparePeriods(params: PeriodComparisonRequest): Promise<ComparisonData> {
  const { data } = await apiClient.post<ComparisonData>('/dashboards/period-comparison', params);
  return data;
}
