/**
 * Smart Persistence Promotion React Query Hooks
 * P7 - External Integrations & Data Optimization (FS24)
 */

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
    // Promotion candidate hooks
    listPromotionCandidates,
    listPromotionCandidatesByStatus,
    getPromotionCandidate,
    updatePromotionCandidate,
    approvePromotion,
    dismissPromotion,
    deletePromotionCandidate,
    // Migration hooks
    startMigration,
    getMigrationProgress,
    // Promoted table hooks
    listPromotedTables,
    getPromotedTable,
    getPromotedTableByMapping,
} from '../api/promotions';
import type {
    UpdatePromotionCandidateRequest,
    ApprovePromotionRequest,
    DismissPromotionRequest,
    StartMigrationRequest,
    PromotionCandidateStatus,
    PaginationParams,
} from '@reportplatform/types';

// =============================================================================
// Promotion Candidate Hooks
// =============================================================================

/** Get all promotion candidates */
export function usePromotionCandidates(params?: PaginationParams) {
    return useQuery({
        queryKey: ['promotions', 'candidates', params],
        queryFn: () => listPromotionCandidates(params),
    });
}

/** Get promotion candidates by status */
export function usePromotionCandidatesByStatus(status: PromotionCandidateStatus) {
    return useQuery({
        queryKey: ['promotions', 'candidates', 'status', status],
        queryFn: () => listPromotionCandidatesByStatus(status),
    });
}

/** Get a single promotion candidate */
export function usePromotionCandidate(candidateId: string) {
    return useQuery({
        queryKey: ['promotions', 'candidates', candidateId],
        queryFn: () => getPromotionCandidate(candidateId),
        enabled: !!candidateId,
    });
}

/** Update a promotion candidate */
export function useUpdatePromotionCandidate() {
    const qc = useQueryClient();
    return useMutation({
        mutationFn: ({
            candidateId,
            updates,
        }: {
            candidateId: string;
            updates: UpdatePromotionCandidateRequest;
        }) => updatePromotionCandidate(candidateId, updates),
        onSuccess: (_, { candidateId }) => {
            qc.invalidateQueries({ queryKey: ['promotions', 'candidates', candidateId] });
            qc.invalidateQueries({ queryKey: ['promotions', 'candidates'] });
        },
    });
}

/** Approve a promotion candidate */
export function useApprovePromotion() {
    const qc = useQueryClient();
    return useMutation({
        mutationFn: ({
            candidateId,
            request,
        }: {
            candidateId: string;
            request?: ApprovePromotionRequest;
        }) => approvePromotion(candidateId, request),
        onSuccess: (_, { candidateId }) => {
            qc.invalidateQueries({ queryKey: ['promotions', 'candidates', candidateId] });
            qc.invalidateQueries({ queryKey: ['promotions', 'candidates'] });
            qc.invalidateQueries({ queryKey: ['promotions', 'tables'] });
        },
    });
}

/** Dismiss a promotion candidate */
export function useDismissPromotion() {
    const qc = useQueryClient();
    return useMutation({
        mutationFn: ({
            candidateId,
            request,
        }: {
            candidateId: string;
            request: DismissPromotionRequest;
        }) => dismissPromotion(candidateId, request),
        onSuccess: () => {
            qc.invalidateQueries({ queryKey: ['promotions', 'candidates'] });
        },
    });
}

/** Delete a promotion candidate */
export function useDeletePromotionCandidate() {
    const qc = useQueryClient();
    return useMutation({
        mutationFn: (candidateId: string) => deletePromotionCandidate(candidateId),
        onSuccess: () => {
            qc.invalidateQueries({ queryKey: ['promotions', 'candidates'] });
        },
    });
}

// =============================================================================
// Migration Hooks
// =============================================================================

/** Start data migration */
export function useStartMigration() {
    const qc = useQueryClient();
    return useMutation({
        mutationFn: ({
            candidateId,
            request,
        }: {
            candidateId: string;
            request?: StartMigrationRequest;
        }) => startMigration(candidateId, request),
        onSuccess: (_, { candidateId }) => {
            qc.invalidateQueries({ queryKey: ['promotions', 'candidates', candidateId] });
            qc.invalidateQueries({ queryKey: ['promotions', 'migration', candidateId] });
        },
    });
}

/** Get migration progress */
export function useMigrationProgress(candidateId: string) {
    return useQuery({
        queryKey: ['promotions', 'migration', candidateId],
        queryFn: () => getMigrationProgress(candidateId),
        enabled: !!candidateId,
        refetchInterval: 2000, // Poll every 2 seconds during migration
    });
}

// =============================================================================
// Promoted Table Hooks
// =============================================================================

/** Get all promoted tables */
export function usePromotedTables() {
    return useQuery({
        queryKey: ['promotions', 'tables'],
        queryFn: listPromotedTables,
    });
}

/** Get a single promoted table */
export function usePromotedTable(tableId: string) {
    return useQuery({
        queryKey: ['promotions', 'tables', tableId],
        queryFn: () => getPromotedTable(tableId),
        enabled: !!tableId,
    });
}

/** Get promoted table by mapping ID */
export function usePromotedTableByMapping(mappingId: string) {
    return useQuery({
        queryKey: ['promotions', 'tables', 'mapping', mappingId],
        queryFn: () => getPromotedTableByMapping(mappingId),
        enabled: !!mappingId,
    });
}
