/**
 * Export Flows API Client
 * FS27 – Live Excel Export & External Sync
 */

import apiClient from './axios';
import type {
    ExportFlowDefinition,
    ExportFlowExecution,
    ExportFlowCreateRequest,
    ExportFlowUpdateRequest,
    ExportFlowTestResponse,
} from '@reportplatform/types';

const BASE = '/api/export-flows';

export interface ExportFlowListParams {
    page?: number;
    size?: number;
    search?: string;
    targetType?: string;
    triggerType?: string;
    status?: string;
}

export interface PaginatedExecutions {
    content: ExportFlowExecution[];
    totalElements: number;
    totalPages: number;
    number: number;
    size: number;
}

// =============================================================================
// Flow CRUD
// =============================================================================

export async function listExportFlows(
    params?: ExportFlowListParams
): Promise<ExportFlowDefinition[]> {
    const { data } = await apiClient.get<ExportFlowDefinition[]>(BASE, { params });
    return data;
}

export async function getExportFlow(id: string): Promise<ExportFlowDefinition> {
    const { data } = await apiClient.get<ExportFlowDefinition>(`${BASE}/${id}`);
    return data;
}

export async function createExportFlow(
    payload: ExportFlowCreateRequest
): Promise<ExportFlowDefinition> {
    const { data } = await apiClient.post<ExportFlowDefinition>(BASE, payload);
    return data;
}

export async function updateExportFlow(
    id: string,
    payload: ExportFlowUpdateRequest
): Promise<ExportFlowDefinition> {
    const { data } = await apiClient.put<ExportFlowDefinition>(`${BASE}/${id}`, payload);
    return data;
}

export async function deleteExportFlow(id: string): Promise<void> {
    await apiClient.delete(`${BASE}/${id}`);
}

// =============================================================================
// Execution
// =============================================================================

export async function executeExportFlow(id: string): Promise<ExportFlowExecution> {
    const { data } = await apiClient.post<ExportFlowExecution>(`${BASE}/${id}/execute`);
    return data;
}

export async function testExportFlow(id: string): Promise<ExportFlowTestResponse> {
    const { data } = await apiClient.post<ExportFlowTestResponse>(`${BASE}/${id}/test`);
    return data;
}

export async function getExecutionHistory(
    flowId: string,
    params?: { page?: number; size?: number }
): Promise<PaginatedExecutions> {
    const { data } = await apiClient.get<PaginatedExecutions>(
        `${BASE}/${flowId}/executions`,
        { params }
    );
    return data;
}

export async function getModuleHealth(): Promise<{ status: string; localPath: string; sharepoint: string }> {
    const { data } = await apiClient.get<{ status: string; localPath: string; sharepoint: string }>(
        `${BASE}/health`
    );
    return data;
}
