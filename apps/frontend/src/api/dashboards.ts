import apiClient from './axios';
import type {
  DashboardConfig,
  DashboardSummary,
  DashboardQueryParams,
  AggregatedData,
  PeriodComparisonRequest,
  ComparisonData,
  WidgetConfig,
} from '@reportplatform/types';

interface DashboardRequest {
  name: string;
  description?: string;
  config: { widgets: WidgetConfig[] };
  chartType?: string;
  is_public: boolean;
}

interface DashboardResponse {
  id: string;
  orgId: string;
  createdBy: string;
  name: string;
  description?: string;
  config: { widgets: WidgetConfig[] };
  chartType?: string;
  isPublic: boolean;
  createdAt: string;
  updatedAt: string;
}

export async function listDashboards(): Promise<DashboardSummary[]> {
  const { data } = await apiClient.get('/dashboards');
  return Array.isArray(data) ? data : (data?.dashboards ?? []);
}

export async function createDashboard(config: DashboardConfig): Promise<DashboardConfig> {
  const request: DashboardRequest = {
    name: config.name,
    description: config.description,
    config: { widgets: config.widgets },
    chartType: 'bar',
    is_public: config.is_public,
  };
  const { data } = await apiClient.post<DashboardResponse>('/dashboards', request);
  return {
    id: data.id,
    name: data.name,
    description: data.description,
    is_public: data.isPublic,
    widgets: data.config?.widgets || [],
    created_at: data.createdAt,
  };
}

export async function getDashboard(dashboardId: string): Promise<DashboardConfig> {
  const { data } = await apiClient.get<DashboardResponse>(`/dashboards/${dashboardId}`);
  return {
    id: data.id,
    name: data.name,
    description: data.description,
    is_public: data.isPublic,
    widgets: data.config?.widgets || [],
    created_at: data.createdAt,
  };
}

export async function updateDashboard(dashboardId: string, config: DashboardConfig): Promise<DashboardConfig> {
  const request: DashboardRequest = {
    name: config.name,
    description: config.description,
    config: { widgets: config.widgets },
    chartType: 'bar',
    is_public: config.is_public,
  };
  const { data } = await apiClient.put<DashboardResponse>(`/dashboards/${dashboardId}`, request);
  return {
    id: data.id,
    name: data.name,
    description: data.description,
    is_public: data.isPublic,
    widgets: data.config?.widgets || [],
    created_at: data.createdAt,
  };
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

export interface RawSqlResult {
  columns: string[];
  rows: unknown[][];
  totalRows: number;
}

export async function executeRawSql(sql: string): Promise<RawSqlResult> {
  const { data } = await apiClient.post<RawSqlResult>('/dashboards/sql/execute', { sql });
  return data;
}
