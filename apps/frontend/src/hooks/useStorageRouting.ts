import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  listRoutingRules,
  upsertRoutingRule,
  deleteRoutingRule,
  refreshRoutingRules,
  getDataSourceStats,
} from '../api/storageRouting';
import type { UpsertRoutingRuleRequest } from '@reportplatform/types';

/** Fetch all active storage routing rules (admin only). */
export function useStorageRoutingRules() {
  return useQuery({
    queryKey: ['storageRouting', 'rules'],
    queryFn: listRoutingRules,
  });
}

/** Upsert a routing rule. Invalidates the rule list on success. */
export function useUpsertRoutingRule() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: UpsertRoutingRuleRequest) => upsertRoutingRule(request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['storageRouting'] });
    },
  });
}

/** Delete a routing rule. Invalidates the rule list on success. */
export function useDeleteRoutingRule() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => deleteRoutingRule(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['storageRouting'] });
    },
  });
}

/** Force cache refresh on the server. */
export function useRefreshRoutingRules() {
  return useMutation({
    mutationFn: refreshRoutingRules,
  });
}

/**
 * Fetch data-source stats (row counts + availability) for the current org.
 * Refreshes every 60 seconds automatically.
 */
export function useDataSourceStats() {
  return useQuery({
    queryKey: ['storageRouting', 'stats'],
    queryFn: getDataSourceStats,
    refetchInterval: 60_000,
  });
}
