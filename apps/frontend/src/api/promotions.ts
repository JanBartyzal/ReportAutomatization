/**
 * Smart Persistence Promotion API Client
 * P7 - External Integrations & Data Optimization (FS24)
 */

import apiClient from './axios';
import type {
    PromotionCandidate,
    UpdatePromotionCandidateRequest,
    ApprovePromotionRequest,
    DismissPromotionRequest,
    StartMigrationRequest,
    PromotedTable,
    MigrationProgress,
    PaginatedResponse,
    PaginationParams,
} from '@reportplatform/types';

// =============================================================================
// Promotion Candidates
// =============================================================================

/** Get all promotion candidates */
export async function listPromotionCandidates(
    params?: PaginationParams
): Promise<PaginatedResponse<PromotionCandidate>> {
    const { data } = await apiClient.get<PaginatedResponse<PromotionCandidate>>(
        '/admin/promotions',
        { params }
    );
    return data;
}

/** Get candidates by status */
export async function listPromotionCandidatesByStatus(
    status: PromotionCandidate['status']
): Promise<PromotionCandidate[]> {
    const { data } = await apiClient.get<PromotionCandidate[]>(
        '/admin/promotions',
        { params: { status } }
    );
    return data;
}

/** Get a single promotion candidate by ID */
export async function getPromotionCandidate(
    candidateId: string
): Promise<PromotionCandidate> {
    const { data } = await apiClient.get<PromotionCandidate>(
        `/admin/promotions/${candidateId}`
    );
    return data;
}

/** Update a promotion candidate (admin modifications) */
export async function updatePromotionCandidate(
    candidateId: string,
    updates: UpdatePromotionCandidateRequest
): Promise<PromotionCandidate> {
    const { data } = await apiClient.put<PromotionCandidate>(
        `/admin/promotions/${candidateId}`,
        updates
    );
    return data;
}

/** Approve a promotion candidate and trigger table creation */
export async function approvePromotion(
    candidateId: string,
    request?: ApprovePromotionRequest
): Promise<PromotionCandidate> {
    const { data } = await apiClient.post<PromotionCandidate>(
        `/admin/promotions/${candidateId}/approve`,
        request
    );
    return data;
}

/** Dismiss a promotion candidate */
export async function dismissPromotion(
    candidateId: string,
    request: DismissPromotionRequest
): Promise<PromotionCandidate> {
    const { data } = await apiClient.post<PromotionCandidate>(
        `/admin/promotions/${candidateId}/dismiss`,
        request
    );
    return data;
}

/** Delete a promotion candidate */
export async function deletePromotionCandidate(candidateId: string): Promise<void> {
    await apiClient.delete(`/admin/promotions/${candidateId}`);
}

// =============================================================================
// Data Migration
// =============================================================================

/** Start data migration for a promoted table */
export async function startMigration(
    candidateId: string,
    request?: StartMigrationRequest
): Promise<MigrationProgress> {
    const { data } = await apiClient.post<MigrationProgress>(
        `/admin/promotions/${candidateId}/migrate`,
        request
    );
    return data;
}

/** Get migration progress */
export async function getMigrationProgress(
    candidateId: string
): Promise<MigrationProgress> {
    const { data } = await apiClient.get<MigrationProgress>(
        `/admin/promotions/${candidateId}/migration-progress`
    );
    return data;
}

// =============================================================================
// Promoted Tables
// =============================================================================

/** Get all promoted tables */
export async function listPromotedTables(): Promise<PromotedTable[]> {
    const { data } = await apiClient.get<PromotedTable[]>('/admin/promoted-tables');
    return data;
}

/** Get a single promoted table by ID */
export async function getPromotedTable(tableId: string): Promise<PromotedTable> {
    const { data } = await apiClient.get<PromotedTable>(
        `/admin/promoted-tables/${tableId}`
    );
    return data;
}

/** Get promoted table by mapping ID */
export async function getPromotedTableByMapping(
    mappingId: string
): Promise<PromotedTable | null> {
    const { data } = await apiClient.get<PromotedTable | null>(
        `/admin/promoted-tables/by-mapping/${mappingId}`
    );
    return data;
}
