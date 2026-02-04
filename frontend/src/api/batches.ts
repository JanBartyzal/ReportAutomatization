import api from './axios';

export interface Batch {
    id: string;
    name: string;
    owner_id: number;
    status: 'OPEN' | 'CLOSED' | 'ARCHIVED';
    created_at?: string;
}

export interface BatchCreate {
    name: string;
}

export const batchKeys = {
    all: ['batches'] as const,
    lists: () => [...batchKeys.all, 'list'] as const,
    detail: (id: string) => [...batchKeys.all, 'detail', id] as const,
};

export const getBatches = async () => {
    const response = await api.get<Batch[]>('/api/batches/');
    return response.data;
};

export const createBatch = async (data: BatchCreate) => {
    const response = await api.post<Batch>('/api/batches/', data);
    return response.data;
};

export const closeBatch = async (batchId: string) => {
    const response = await api.post<Batch>(`/api/batches/${batchId}/close`);
    return response.data;
};

export const deleteBatch = async (batchId: string) => {
    await api.delete(`/api/batches/${batchId}`);
};
