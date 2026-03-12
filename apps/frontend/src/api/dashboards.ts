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

export async function cloneDashboard(dashboardId: string, newName?: string): Promise<DashboardConfig> {
  const { data } = await apiClient.post<DashboardConfig>(`/dashboards/${dashboardId}/clone`, { name: newName });
  return data;
}

export async function updateDashboardSharing(
  dashboardId: string,
  isPublic: boolean,
  shareLink?: string
): Promise<{ share_url: string }> {
  const { data } = await apiClient.put<{ share_url: string }>(`/dashboards/${dashboardId}/sharing`, {
    is_public: isPublic,
    share_link: shareLink,
  });
  return data;
}

export async function executeRawSql(sql: string): Promise<{ columns: string[]; rows: unknown[][] }> {
  const { data } = await apiClient.post<{ columns: string[]; rows: unknown[][] }>('/dashboards/sql/execute', { sql });
  return data;
}
