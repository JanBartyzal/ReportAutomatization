import apiClient from './axios';

export interface VersionResponse {
    id: string;
    entity_type: string;
    entity_id: string;
    version_number: number;
    locked: boolean;
    created_by: string;
    created_at: string;
    reason?: string;
}

export interface FieldChange {
    field_path: string;
    change_type: 'ADDED' | 'REMOVED' | 'MODIFIED';
    old_value: unknown;
    new_value: unknown;
}

export interface VersionDiffResponse {
    entity_type: string;
    entity_id: string;
    from_version: number;
    to_version: number;
    changes: FieldChange[];
}

export type EntityType = 'FORM' | 'REPORT' | 'FILE' | 'TEMPLATE' | 'DASHBOARD';

export async function listVersions(entityType: EntityType, entityId: string): Promise<VersionResponse[]> {
    const { data } = await apiClient.get<VersionResponse[]>(`/versions/${entityType}/${entityId}`);
    return data;
}

export async function getVersionDiff(
    entityType: EntityType,
    entityId: string,
    v1: number,
    v2: number
): Promise<VersionDiffResponse> {
    const { data } = await apiClient.get<VersionDiffResponse>(`/versions/${entityType}/${entityId}/diff`, {
        params: { v1, v2 },
    });
    return data;
}

export async function createVersion(
    entityType: EntityType,
    entityId: string,
    reason?: string
): Promise<VersionResponse> {
    const { data } = await apiClient.post<VersionResponse>('/versions', {
        entityType,
        entityId,
        reason,
    });
    return data;
}

export async function restoreVersion(
    entityType: EntityType,
    entityId: string,
    versionNumber: number
): Promise<VersionResponse> {
    const { data } = await apiClient.post<VersionResponse>(`/versions/${entityType}/${entityId}/restore`, {
        versionNumber,
    });
    return data;
}
