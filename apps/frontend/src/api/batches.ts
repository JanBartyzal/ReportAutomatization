import apiClient from './axios';

export interface Batch {
    id: string;
    name: string;
    period: string;
    periodId?: string;
    description?: string;
    holdingId: string;
    status: 'OPEN' | 'COLLECTING' | 'CLOSED';
    createdBy: string;
    createdAt: string;
    closedAt?: string;
}

export interface BatchFile {
    id: string;
    batchId: string;
    fileId: string;
    addedAt: string;
    addedBy: string;
}

export async function listBatches(holdingId?: string): Promise<Batch[]> {
    const params: Record<string, string> = {};
    if (holdingId) params.holdingId = holdingId;
    const { data } = await apiClient.get<Batch[]>('/batches', { params });
    return data;
}

export async function createBatch(batch: {
    name: string;
    period: string;
    period_id?: string;
    description?: string;
    holding_id: string;
    created_by?: string;
}): Promise<Batch> {
    const { data } = await apiClient.post<Batch>('/batches', batch);
    return data;
}

export async function updateBatch(batchId: string, update: Record<string, unknown>): Promise<Batch> {
    const { data } = await apiClient.put<Batch>(`/batches/${batchId}`, update);
    return data;
}

export async function deleteBatch(batchId: string): Promise<void> {
    await apiClient.delete(`/batches/${batchId}`);
}

export async function listBatchFiles(batchId: string): Promise<BatchFile[]> {
    const { data } = await apiClient.get<BatchFile[]>(`/batches/${batchId}/files`);
    return data;
}

export async function addFileToBatch(batchId: string, fileId: string): Promise<BatchFile> {
    const { data } = await apiClient.post<BatchFile>(`/batches/${batchId}/files`, { file_id: fileId });
    return data;
}

export async function removeFileFromBatch(batchId: string, fileId: string): Promise<void> {
    await apiClient.delete(`/batches/${batchId}/files/${fileId}`);
}
