import apiClient from './axios';
import type {
  StorageRoutingRule,
  UpsertRoutingRuleRequest,
  DataSourceStats,
} from '@reportplatform/types';

/** List all active storage routing rules. */
export async function listRoutingRules(): Promise<StorageRoutingRule[]> {
  const { data } = await apiClient.get<StorageRoutingRule[]>('/v1/admin/storage-routing');
  return data;
}

/** Upsert (create or replace) a routing rule. */
export async function upsertRoutingRule(
  request: UpsertRoutingRuleRequest,
): Promise<StorageRoutingRule> {
  const { data } = await apiClient.put<StorageRoutingRule>('/v1/admin/storage-routing', request);
  return data;
}

/** Delete a routing rule by ID. The global default rule cannot be deleted. */
export async function deleteRoutingRule(id: string): Promise<void> {
  await apiClient.delete(`/v1/admin/storage-routing/${id}`);
}

/** Force immediate refresh of the in-memory rule cache on the server. */
export async function refreshRoutingRules(): Promise<void> {
  await apiClient.post('/v1/admin/storage-routing/refresh');
}

/** Get storage backend statistics (record counts + availability) for the current org. */
export async function getDataSourceStats(): Promise<DataSourceStats> {
  const { data } = await apiClient.get<DataSourceStats>('/query/sinks/data-source-stats');
  return data;
}
