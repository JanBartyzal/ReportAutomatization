import apiClient from './axios';
import type {
  NamedQuery,
  CreateNamedQueryRequest,
  UpdateNamedQueryRequest,
  NamedQueryExecuteRequest,
  NamedQueryResult,
} from '@reportplatform/types';

export async function listNamedQueries(dataSourceHint?: string): Promise<NamedQuery[]> {
  const { data } = await apiClient.get<NamedQuery[]>('/v1/data/named-queries', {
    params: dataSourceHint ? { dataSourceHint } : undefined,
  });
  return data;
}

export async function getNamedQuery(id: string): Promise<NamedQuery> {
  const { data } = await apiClient.get<NamedQuery>(`/v1/data/named-queries/${id}`);
  return data;
}

export async function createNamedQuery(req: CreateNamedQueryRequest): Promise<NamedQuery> {
  const { data } = await apiClient.post<NamedQuery>('/v1/data/named-queries', req);
  return data;
}

export async function updateNamedQuery(
  id: string,
  req: UpdateNamedQueryRequest,
): Promise<NamedQuery> {
  const { data } = await apiClient.patch<NamedQuery>(`/v1/data/named-queries/${id}`, req);
  return data;
}

export async function deleteNamedQuery(id: string): Promise<void> {
  await apiClient.delete(`/v1/data/named-queries/${id}`);
}

export async function executeNamedQuery(
  id: string,
  req?: NamedQueryExecuteRequest,
): Promise<NamedQueryResult> {
  const { data } = await apiClient.post<NamedQueryResult>(
    `/v1/data/named-queries/${id}/execute`,
    req ?? {},
  );
  return data;
}
