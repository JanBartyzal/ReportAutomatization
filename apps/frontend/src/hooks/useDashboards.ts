import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
    listDashboards,
    getDashboard,
    createDashboard,
    updateDashboard,
    deleteDashboard,
    executeDashboardQuery,
    comparePeriods,
} from '../api/dashboards';
import type {
    DashboardConfig,
    DashboardQueryParams,
    PeriodComparisonRequest,
} from '@reportplatform/types';

/**
 * Get all dashboards (for current user)
 */
export function useDashboards() {
    return useQuery({
        queryKey: ['dashboards'],
        queryFn: () => listDashboards(),
    });
}

/**
 * Get a single dashboard by ID
 */
export function useDashboard(dashboardId: string) {
    return useQuery<DashboardConfig>({
        queryKey: ['dashboards', dashboardId],
        queryFn: () => getDashboard(dashboardId),
        enabled: !!dashboardId,
    });
}

/**
 * Create a new dashboard
 */
export function useCreateDashboard() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: (config: DashboardConfig) => createDashboard(config),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['dashboards'] });
        },
    });
}

/**
 * Update an existing dashboard
 */
export function useUpdateDashboard() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: ({ dashboardId, config }: { dashboardId: string; config: DashboardConfig }) =>
            updateDashboard(dashboardId, config),
        onSuccess: (_, variables) => {
            queryClient.invalidateQueries({ queryKey: ['dashboards'] });
            queryClient.invalidateQueries({ queryKey: ['dashboards', variables.dashboardId] });
        },
    });
}

/**
 * Delete a dashboard
 */
export function useDeleteDashboard() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: (dashboardId: string) => deleteDashboard(dashboardId),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['dashboards'] });
        },
    });
}

/**
 * Execute a dashboard query to get widget data
 */
export function useDashboardQuery(dashboardId: string) {
    return useMutation({
        mutationFn: (params: DashboardQueryParams) => executeDashboardQuery(dashboardId, params),
    });
}

/**
 * Compare periods for a metric
 */
export function usePeriodComparison() {
    return useMutation({
        mutationFn: (params: PeriodComparisonRequest) => comparePeriods(params),
    });
}
