import apiClient from './axios';
import type {
  SnowProject,
  SnowProjectPage,
  ProjectSyncConfig,
  UpsertProjectSyncConfigRequest,
  ProjectListFilters,
} from '@reportplatform/types';

// ── Project data (engine-data) ──────────────────────────────────────────────

export async function listProjects(filters: ProjectListFilters = {}): Promise<SnowProjectPage> {
  const { data } = await apiClient.get<SnowProjectPage>('/v1/data/snow/projects', {
    params: filters,
  });
  return data;
}

export async function getProject(id: string): Promise<SnowProject> {
  const { data } = await apiClient.get<SnowProject>(`/v1/data/snow/projects/${id}`);
  return data;
}

// ── Project sync config (engine-integrations admin) ──────────────────────────

export async function getProjectSyncConfig(connectionId: string): Promise<ProjectSyncConfig> {
  const { data } = await apiClient.get<ProjectSyncConfig>(
    `/admin/integrations/servicenow/${connectionId}/project-sync`,
  );
  return data;
}

export async function upsertProjectSyncConfig(
  connectionId: string,
  req: UpsertProjectSyncConfigRequest,
): Promise<ProjectSyncConfig> {
  const { data } = await apiClient.post<ProjectSyncConfig>(
    `/admin/integrations/servicenow/${connectionId}/project-sync`,
    req,
  );
  return data;
}

export async function triggerProjectSync(
  connectionId: string,
): Promise<{ projects_fetched: number; projects_stored: number; status: string }> {
  const { data } = await apiClient.post(
    `/admin/integrations/servicenow/${connectionId}/project-sync/trigger`,
  );
  return data;
}
