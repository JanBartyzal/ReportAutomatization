/**
 * Export Flows React Query Hooks
 * FS27 – Live Excel Export & External Sync
 */

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
    listExportFlows,
    getExportFlow,
    createExportFlow,
    updateExportFlow,
    deleteExportFlow,
    executeExportFlow,
    testExportFlow,
    getExecutionHistory,
    type ExportFlowListParams,
} from '../api/exportFlows';
import type { ExportFlowCreateRequest, ExportFlowUpdateRequest } from '@reportplatform/types';

const QUERY_KEYS = {
    all: ['export-flows'] as const,
    lists: () => [...QUERY_KEYS.all, 'list'] as const,
    list: (params?: ExportFlowListParams) => [...QUERY_KEYS.lists(), params] as const,
    details: () => [...QUERY_KEYS.all, 'detail'] as const,
    detail: (id: string) => [...QUERY_KEYS.details(), id] as const,
    executions: (flowId: string, params?: object) =>
        [...QUERY_KEYS.detail(flowId), 'executions', params] as const,
};

// =============================================================================
// Flow Queries
// =============================================================================

export function useExportFlows(params?: ExportFlowListParams) {
    return useQuery({
        queryKey: QUERY_KEYS.list(params),
        queryFn: () => listExportFlows(params),
        staleTime: 30_000,
    });
}

export function useExportFlow(id: string) {
    return useQuery({
        queryKey: QUERY_KEYS.detail(id),
        queryFn: () => getExportFlow(id),
        enabled: !!id,
        staleTime: 10_000,
    });
}

// =============================================================================
// Flow Mutations
// =============================================================================

export function useCreateExportFlow() {
    const qc = useQueryClient();
    return useMutation({
        mutationFn: (payload: ExportFlowCreateRequest) => createExportFlow(payload),
        onSuccess: () => qc.invalidateQueries({ queryKey: QUERY_KEYS.lists() }),
    });
}

export function useUpdateExportFlow() {
    const qc = useQueryClient();
    return useMutation({
        mutationFn: ({ id, payload }: { id: string; payload: ExportFlowUpdateRequest }) =>
            updateExportFlow(id, payload),
        onSuccess: (_, { id }) => {
            qc.invalidateQueries({ queryKey: QUERY_KEYS.detail(id) });
            qc.invalidateQueries({ queryKey: QUERY_KEYS.lists() });
        },
    });
}

export function useDeleteExportFlow() {
    const qc = useQueryClient();
    return useMutation({
        mutationFn: (id: string) => deleteExportFlow(id),
        onSuccess: () => qc.invalidateQueries({ queryKey: QUERY_KEYS.all }),
    });
}

// =============================================================================
// Execution Hooks
// =============================================================================

export function useExecuteExportFlow() {
    const qc = useQueryClient();
    return useMutation({
        mutationFn: (id: string) => executeExportFlow(id),
        onSuccess: (_data, id) => {
            qc.invalidateQueries({ queryKey: QUERY_KEYS.executions(id) });
            // Refresh list to pick up new last execution status
            qc.invalidateQueries({ queryKey: QUERY_KEYS.lists() });
        },
    });
}

export function useTestExportFlow() {
    return useMutation({
        mutationFn: (id: string) => testExportFlow(id),
    });
}

export function useExecutionHistory(
    flowId: string,
    params?: { page?: number; size?: number }
) {
    const { data, ...rest } = useQuery({
        queryKey: QUERY_KEYS.executions(flowId, params),
        queryFn: () => getExecutionHistory(flowId, params),
        enabled: !!flowId,
        staleTime: 10_000,
        refetchInterval: (query) => {
            // Auto-refresh when any execution is still RUNNING
            const executions = query.state.data?.content ?? [];
            const hasRunning = executions.some((e) => e.status === 'RUNNING');
            return hasRunning ? 5_000 : false;
        },
    });

    return { data, ...rest };
}

export { QUERY_KEYS as EXPORT_FLOW_KEYS };
