import apiClient from './axios';
import type {
  OrganizationAdmin,
  UserRole,
  ApiKey,
  ApiKeyCreated,
  FailedJob,
  PaginatedResponse,
  PaginationParams,
  Role,
} from '@reportplatform/types';

// --- Organizations ---

export async function listOrganizations(): Promise<OrganizationAdmin[]> {
  const { data } = await apiClient.get<OrganizationAdmin[]>('/admin/organizations');
  return data;
}

export async function createOrganization(org: { name: string; type: string; parent_id?: string }): Promise<OrganizationAdmin> {
  const { data } = await apiClient.post<OrganizationAdmin>('/admin/organizations', org);
  return data;
}

export async function getOrganization(orgId: string): Promise<OrganizationAdmin> {
  const { data } = await apiClient.get<OrganizationAdmin>(`/admin/organizations/${orgId}`);
  return data;
}

export async function updateOrganization(orgId: string, org: Partial<OrganizationAdmin>): Promise<OrganizationAdmin> {
  const { data } = await apiClient.put<OrganizationAdmin>(`/admin/organizations/${orgId}`, org);
  return data;
}

export async function deleteOrganization(orgId: string): Promise<void> {
  await apiClient.delete(`/admin/organizations/${orgId}`);
}

// --- Users & Roles ---

export interface UserListParams extends PaginationParams {
  org_id?: string;
}

export async function listUsers(params: UserListParams = {}): Promise<PaginatedResponse<UserRole>> {
  const { data } = await apiClient.get<PaginatedResponse<UserRole>>('/admin/users', { params });
  return data;
}

export async function assignRole(userId: string, role: Role, orgId: string): Promise<void> {
  await apiClient.post(`/admin/users/${userId}/roles`, { role, org_id: orgId });
}

export async function removeRole(userId: string, role: Role, orgId: string): Promise<void> {
  await apiClient.delete(`/admin/users/${userId}/roles`, { data: { role, org_id: orgId } });
}

// --- API Keys ---

export async function listApiKeys(): Promise<ApiKey[]> {
  const { data } = await apiClient.get<ApiKey[]>('/admin/api-keys');
  return data;
}

export async function createApiKey(body: { name: string; scopes: string[] }): Promise<ApiKeyCreated> {
  const { data } = await apiClient.post<ApiKeyCreated>('/admin/api-keys', body);
  return data;
}

export async function revokeApiKey(keyId: string): Promise<void> {
  await apiClient.delete(`/admin/api-keys/${keyId}`);
}

// --- Failed Jobs ---

export interface FailedJobListParams extends PaginationParams {
  org_id?: string;
}

export async function listFailedJobs(params: FailedJobListParams = {}): Promise<PaginatedResponse<FailedJob>> {
  const { data } = await apiClient.get<PaginatedResponse<FailedJob>>('/admin/failed-jobs', { params });
  return data;
}

export async function reprocessFailedJob(jobId: string): Promise<void> {
  await apiClient.post(`/admin/failed-jobs/${jobId}/reprocess`);
}
