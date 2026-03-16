import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
    listBatches,
    createBatch,
    updateBatch,
    deleteBatch,
    listBatchFiles,
    addFileToBatch,
    removeFileFromBatch,
} from '../api/batches';

export function useBatches(holdingId?: string) {
    return useQuery({
        queryKey: ['batches', holdingId],
        queryFn: () => listBatches(holdingId),
    });
}

export function useCreateBatch() {
    const qc = useQueryClient();
    return useMutation({
        mutationFn: (batch: Parameters<typeof createBatch>[0]) => createBatch(batch),
        onSuccess: () => qc.invalidateQueries({ queryKey: ['batches'] }),
    });
}

export function useUpdateBatch() {
    const qc = useQueryClient();
    return useMutation({
        mutationFn: ({ batchId, update }: { batchId: string; update: Record<string, unknown> }) =>
            updateBatch(batchId, update),
        onSuccess: () => qc.invalidateQueries({ queryKey: ['batches'] }),
    });
}

export function useDeleteBatch() {
    const qc = useQueryClient();
    return useMutation({
        mutationFn: (batchId: string) => deleteBatch(batchId),
        onSuccess: () => qc.invalidateQueries({ queryKey: ['batches'] }),
    });
}

export function useBatchFiles(batchId: string | null) {
    return useQuery({
        queryKey: ['batchFiles', batchId],
        queryFn: () => listBatchFiles(batchId!),
        enabled: !!batchId,
    });
}

export function useAddFileToBatch() {
    const qc = useQueryClient();
    return useMutation({
        mutationFn: ({ batchId, fileId }: { batchId: string; fileId: string }) =>
            addFileToBatch(batchId, fileId),
        onSuccess: (_data, vars) => {
            qc.invalidateQueries({ queryKey: ['batchFiles', vars.batchId] });
            qc.invalidateQueries({ queryKey: ['batches'] });
        },
    });
}

export function useRemoveFileFromBatch() {
    const qc = useQueryClient();
    return useMutation({
        mutationFn: ({ batchId, fileId }: { batchId: string; fileId: string }) =>
            removeFileFromBatch(batchId, fileId),
        onSuccess: (_data, vars) => {
            qc.invalidateQueries({ queryKey: ['batchFiles', vars.batchId] });
            qc.invalidateQueries({ queryKey: ['batches'] });
        },
    });
}
