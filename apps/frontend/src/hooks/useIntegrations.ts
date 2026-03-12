/**
 * Service-Now Integration React Query Hooks
 * P7 - External Integrations & Data Optimization
 */

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
    // Connection hooks
    listConnections,
    getConnection,
    createConnection,
    updateConnection,
    deleteConnection,
    testConnection,
    // Schedule hooks
    listSchedules,
    listAllSchedules,
    createSchedule,
    updateSchedule,
    deleteSchedule,
    triggerSync,
    // History hooks
    getSyncHistory,
    getAllSyncHistory,
    // Distribution hooks
    listDistributionRules,
    listDistributionRulesBySchedule,
    createDistributionRule,
    updateDistributionRule,
    deleteDistributionRule,
    getDistributionHistory,
    getAllDistributionHistory,
} from '../api/integrations';
import type {
    CreateServiceNowConnectionRequest,
    TestConnectionRequest,
    CreateSyncScheduleRequest,
    CreateDistributionRuleRequest,
    PaginationParams,
} from '@reportplatform/types';

// =============================================================================
// Connection Hooks
// =============================================================================

/** Get all Service-Now connections */
export function useConnections() {
    return useQuery({
        queryKey: ['integrations', 'connections'],
        queryFn: listConnections,
    });
}

/** Get a single connection by ID */
export function useConnection(connectionId: string) {
    return useQuery({
        queryKey: ['integrations', 'connections', connectionId],
        queryFn: () => getConnection(connectionId),
        enabled: !!connectionId,
    });
}

/** Create a new connection */
export function useCreateConnection() {
    const qc = useQueryClient();
    return useMutation({
        mutationFn: (connection: CreateServiceNowConnectionRequest) =>
            createConnection(connection),
        onSuccess: () => {
            qc.invalidateQueries({ queryKey: ['integrations', 'connections'] });
        },
    });
}

/** Update a connection */
export function useUpdateConnection() {
    const qc = useQueryClient();
    return useMutation({
        mutationFn: ({
            connectionId,
            connection,
        }: {
            connectionId: string;
            connection: Partial<CreateServiceNowConnectionRequest>;
        }) => updateConnection(connectionId, connection),
        onSuccess: (_, { connectionId }) => {
            qc.invalidateQueries({ queryKey: ['integrations', 'connections', connectionId] });
            qc.invalidateQueries({ queryKey: ['integrations', 'connections'] });
        },
    });
}

/** Delete a connection */
export function useDeleteConnection() {
    const qc = useQueryClient();
    return useMutation({
        mutationFn: (connectionId: string) => deleteConnection(connectionId),
        onSuccess: () => {
            qc.invalidateQueries({ queryKey: ['integrations', 'connections'] });
        },
    });
}

/** Test a connection */
export function useTestConnection() {
    return useMutation({
        mutationFn: (testRequest: TestConnectionRequest) => testConnection(testRequest),
    });
}

// =============================================================================
// Schedule Hooks
// =============================================================================

/** Get all schedules for a connection */
export function useSchedules(connectionId: string) {
    return useQuery({
        queryKey: ['integrations', 'schedules', connectionId],
        queryFn: () => listSchedules(connectionId),
        enabled: !!connectionId,
    });
}

/** Get all schedules (global) */
export function useAllSchedules() {
    return useQuery({
        queryKey: ['integrations', 'schedules'],
        queryFn: listAllSchedules,
    });
}

/** Create a new schedule */
export function useCreateSchedule() {
    const qc = useQueryClient();
    return useMutation({
        mutationFn: (schedule: CreateSyncScheduleRequest) => createSchedule(schedule),
        onSuccess: () => {
            qc.invalidateQueries({ queryKey: ['integrations', 'schedules'] });
        },
    });
}

/** Update a schedule */
export function useUpdateSchedule() {
    const qc = useQueryClient();
    return useMutation({
        mutationFn: ({
            scheduleId,
            schedule,
        }: {
            scheduleId: string;
            schedule: Partial<CreateSyncScheduleRequest>;
        }) => updateSchedule(scheduleId, schedule),
        onSuccess: () => {
            qc.invalidateQueries({ queryKey: ['integrations', 'schedules'] });
        },
    });
}

/** Delete a schedule */
export function useDeleteSchedule() {
    const qc = useQueryClient();
    return useMutation({
        mutationFn: (scheduleId: string) => deleteSchedule(scheduleId),
        onSuccess: () => {
            qc.invalidateQueries({ queryKey: ['integrations', 'schedules'] });
        },
    });
}

/** Trigger a manual sync */
export function useTriggerSync() {
    const qc = useQueryClient();
    return useMutation({
        mutationFn: (scheduleId: string) => triggerSync(scheduleId),
        onSuccess: () => {
            qc.invalidateQueries({ queryKey: ['integrations', 'sync-history'] });
        },
    });
}

// =============================================================================
// Sync History Hooks
// =============================================================================

/** Get sync history for a schedule */
export function useSyncHistory(scheduleId: string, params?: PaginationParams) {
    return useQuery({
        queryKey: ['integrations', 'sync-history', scheduleId, params],
        queryFn: () => getSyncHistory(scheduleId, params),
        enabled: !!scheduleId,
    });
}

/** Get all sync history */
export function useAllSyncHistory(params?: PaginationParams) {
    return useQuery({
        queryKey: ['integrations', 'sync-history', params],
        queryFn: () => getAllSyncHistory(params),
    });
}

// =============================================================================
// Distribution Rule Hooks
// =============================================================================

/** Get all distribution rules */
export function useDistributionRules() {
    return useQuery({
        queryKey: ['integrations', 'distribution-rules'],
        queryFn: listDistributionRules,
    });
}

/** Get distribution rules for a schedule */
export function useDistributionRulesBySchedule(scheduleId: string) {
    return useQuery({
        queryKey: ['integrations', 'distribution-rules', scheduleId],
        queryFn: () => listDistributionRulesBySchedule(scheduleId),
        enabled: !!scheduleId,
    });
}

/** Create a distribution rule */
export function useCreateDistributionRule() {
    const qc = useQueryClient();
    return useMutation({
        mutationFn: (rule: CreateDistributionRuleRequest) => createDistributionRule(rule),
        onSuccess: () => {
            qc.invalidateQueries({ queryKey: ['integrations', 'distribution-rules'] });
        },
    });
}

/** Update a distribution rule */
export function useUpdateDistributionRule() {
    const qc = useQueryClient();
    return useMutation({
        mutationFn: ({
            ruleId,
            rule,
        }: {
            ruleId: string;
            rule: Partial<CreateDistributionRuleRequest>;
        }) => updateDistributionRule(ruleId, rule),
        onSuccess: () => {
            qc.invalidateQueries({ queryKey: ['integrations', 'distribution-rules'] });
        },
    });
}

/** Delete a distribution rule */
export function useDeleteDistributionRule() {
    const qc = useQueryClient();
    return useMutation({
        mutationFn: (ruleId: string) => deleteDistributionRule(ruleId),
        onSuccess: () => {
            qc.invalidateQueries({ queryKey: ['integrations', 'distribution-rules'] });
        },
    });
}

/** Get distribution history */
export function useDistributionHistory(ruleId: string, params?: PaginationParams) {
    return useQuery({
        queryKey: ['integrations', 'distribution-history', ruleId, params],
        queryFn: () => getDistributionHistory(ruleId, params),
        enabled: !!ruleId,
    });
}

/** Get all distribution history */
export function useAllDistributionHistory(params?: PaginationParams) {
    return useQuery({
        queryKey: ['integrations', 'distribution-history', params],
        queryFn: () => getAllDistributionHistory(params),
    });
}
