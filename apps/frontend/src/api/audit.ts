import apiClient from './axios';
import type { PaginationParams } from '@reportplatform/types';

export type AuditAction =
    | 'CREATE'
    | 'UPDATE'
    | 'DELETE'
    | 'LOGIN'
    | 'LOGOUT'
    | 'EXPORT'
    | 'APPROVE'
    | 'REJECT';

export type AuditEntityType =
    | 'USER'
    | 'ORGANIZATION'
    | 'FILE'
    | 'REPORT'
    | 'FORM'
    | 'DASHBOARD'
    | 'TEMPLATE';

export interface AuditLogEntry {
    id: string;
    action: AuditAction;
    entity_type: AuditEntityType;
    entity_id: string;
    user_id: string;
    user_name: string;
    org_id: string;
    org_name: string;
    details: Record<string, unknown>;
    ip_address: string;
    user_agent: string;
    created_at: string;
}

export interface AuditLogParams extends PaginationParams {
    action?: AuditAction;
    entity_type?: AuditEntityType;
    entity_id?: string;
    user_id?: string;
    org_id?: string;
    start_date?: string;
    end_date?: string;
}

export async function listAuditLogs(params: AuditLogParams = {}): Promise<{
    data: AuditLogEntry[];
    pagination: {
        page: number;
        page_size: number;
        total_items: number;
        total_pages: number;
    };
}> {
    const { data } = await apiClient.get('/audit/logs', { params });
    return data;
}

export async function getAuditLogDetail(id: string): Promise<AuditLogEntry> {
    const { data } = await apiClient.get<AuditLogEntry>(`/audit/logs/${id}`);
    return data;
}

export async function exportAuditLogs(
    params: AuditLogParams = {},
    format: 'csv' | 'json'
): Promise<Blob> {
    const { data } = await apiClient.get('/audit/export', {
        params: { ...params, format },
        responseType: 'blob',
    });
    return data;
}
