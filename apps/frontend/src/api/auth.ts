import apiClient from './axios';
import type { UserContext } from '@reportplatform/types';

export async function getMe(): Promise<UserContext> {
  const { data } = await apiClient.get<UserContext>('/auth/me');
  return data;
}

export async function switchOrg(orgId: string): Promise<UserContext> {
  const { data } = await apiClient.post<UserContext>('/auth/switch-org', { org_id: orgId });
  return data;
}
