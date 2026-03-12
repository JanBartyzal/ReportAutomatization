/**
 * Service-Now Integration API Client
 * P7 - External Integrations & Data Optimization
 */

import apiClient from './axios';
import type {
    ServiceNowConnection,
    CreateServiceNowConnectionRequest,
    TestConnectionRequest,
    TestConnectionResponse,
    SyncSchedule,
    CreateSyncScheduleRequest,
    SyncJobHistory,
    DistributionRule,
    CreateDistributionRuleRequest,
    DistributionHistory,
    PaginatedResponse,
    PaginationParams,
} from '@reportplatform/types';

// =============================================================================
// Connection Management
// =============================================================================

/** Get all Service-Now connections */
export async function listConnections(): Promise<ServiceNowConnection[]> {
    const { data } = await apiClient.get<ServiceNowConnection[]>(
        '/admin/integrations/servicenow'
    );
    return data;
}

/** Get a single connection by ID */
export async function getConnection(connectionId: string): Promise<ServiceNowConnection> {
    const { data } = await apiClient.get<ServiceNowConnection>(
        `/admin/integrations/servicenow/${connectionId}`
    );
    return data;
}

/** Create a new Service-Now connection */
export async function createConnection(
    connection: CreateServiceNowConnectionRequest
): Promise<ServiceNowConnection> {
    const { data } = await apiClient.post<ServiceNowConnection>(
        '/admin/integrations/servicenow',
        connection
    );
    return data;
}

/** Update an existing connection */
export async function updateConnection(
    connectionId: string,
    connection: Partial<CreateServiceNowConnectionRequest>
): Promise<ServiceNowConnection> {
    const { data } = await apiClient.put<ServiceNowConnection>(
        `/admin/integrations/servicenow/${connectionId}`,
        connection
    );
    return data;
}

/** Delete a connection */
export async function deleteConnection(connectionId: string): Promise<void> {
    await apiClient.delete(`/admin/integrations/servicenow/${connectionId}`);
}

/** Test a connection configuration */
export async function testConnection(
    testRequest: TestConnectionRequest
): Promise<TestConnectionResponse> {
    const { data } = await apiClient.post<TestConnectionResponse>(
        '/admin/integrations/servicenow/test',
        testRequest
    );
    return data;
}

// =============================================================================
// Sync Schedule Management
// =============================================================================

/** Get all sync schedules for a connection */
export async function listSchedules(connectionId: string): Promise<SyncSchedule[]> {
    const { data } = await apiClient.get<SyncSchedule[]>(
        `/admin/integrations/servicenow/${connectionId}/schedules`
    );
    return data;
}

/** Get all sync schedules (global) */
export async function listAllSchedules(): Promise<SyncSchedule[]> {
    const { data } = await apiClient.get<SyncSchedule[]>(
        '/admin/integrations/schedules'
    );
    return data;
}

/** Create a new sync schedule */
export async function createSchedule(
    schedule: CreateSyncScheduleRequest
): Promise<SyncSchedule> {
    const { data } = await apiClient.post<SyncSchedule>(
        '/admin/integrations/schedules',
        schedule
    );
    return data;
}

/** Update a sync schedule */
export async function updateSchedule(
    scheduleId: string,
    schedule: Partial<CreateSyncScheduleRequest>
): Promise<SyncSchedule> {
    const { data } = await apiClient.put<SyncSchedule>(
        `/admin/integrations/schedules/${scheduleId}`,
        schedule
    );
    return data;
}

/** Delete a sync schedule */
export async function deleteSchedule(scheduleId: string): Promise<void> {
    await apiClient.delete(`/admin/integrations/schedules/${scheduleId}`);
}

/** Trigger a manual sync */
export async function triggerSync(scheduleId: string): Promise<SyncJobHistory> {
    const { data } = await apiClient.post<SyncJobHistory>(
        `/admin/integrations/schedules/${scheduleId}/sync`
    );
    return data;
}

// =============================================================================
// Sync Job History
// =============================================================================

/** Get sync job history for a schedule */
export async function getSyncHistory(
    scheduleId: string,
    params?: PaginationParams
): Promise<PaginatedResponse<SyncJobHistory>> {
    const { data } = await apiClient.get<PaginatedResponse<SyncJobHistory>>(
        `/admin/integrations/schedules/${scheduleId}/history`,
        { params }
    );
    return data;
}

/** Get all sync job history */
export async function getAllSyncHistory(
    params?: PaginationParams
): Promise<PaginatedResponse<SyncJobHistory>> {
    const { data } = await apiClient.get<PaginatedResponse<SyncJobHistory>>(
        '/admin/integrations/sync-history',
        { params }
    );
    return data;
}

// =============================================================================
// Distribution Rules
// =============================================================================

/** Get all distribution rules */
export async function listDistributionRules(): Promise<DistributionRule[]> {
    const { data } = await apiClient.get<DistributionRule[]>(
        '/admin/integrations/distribution-rules'
    );
    return data;
}

/** Get distribution rules for a schedule */
export async function listDistributionRulesBySchedule(
    scheduleId: string
): Promise<DistributionRule[]> {
    const { data } = await apiClient.get<DistributionRule[]>(
        `/admin/integrations/schedules/${scheduleId}/distribution-rules`
    );
    return data;
}

/** Create a distribution rule */
export async function createDistributionRule(
    rule: CreateDistributionRuleRequest
): Promise<DistributionRule> {
    const { data } = await apiClient.post<DistributionRule>(
        '/admin/integrations/distribution-rules',
        rule
    );
    return data;
}

/** Update a distribution rule */
export async function updateDistributionRule(
    ruleId: string,
    rule: Partial<CreateDistributionRuleRequest>
): Promise<DistributionRule> {
    const { data } = await apiClient.put<DistributionRule>(
        `/admin/integrations/distribution-rules/${ruleId}`,
        rule
    );
    return data;
}

/** Delete a distribution rule */
export async function deleteDistributionRule(ruleId: string): Promise<void> {
    await apiClient.delete(`/admin/integrations/distribution-rules/${ruleId}`);
}

/** Get distribution history */
export async function getDistributionHistory(
    ruleId: string,
    params?: PaginationParams
): Promise<PaginatedResponse<DistributionHistory>> {
    const { data } = await apiClient.get<PaginatedResponse<DistributionHistory>>(
        `/admin/integrations/distribution-rules/${ruleId}/history`,
        { params }
    );
    return data;
}

/** Get all distribution history */
export async function getAllDistributionHistory(
    params?: PaginationParams
): Promise<PaginatedResponse<DistributionHistory>> {
    const { data } = await apiClient.get<PaginatedResponse<DistributionHistory>>(
        '/admin/integrations/distribution-history',
        { params }
    );
    return data;
}
