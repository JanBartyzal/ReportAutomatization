import apiClient from './axios';
import type {
  Period,
  CompletionMatrix,
  PaginatedResponse,
  PaginationParams,
} from '@reportplatform/types';

export interface PeriodListParams extends PaginationParams {
  type?: string;
}

export async function listPeriods(params: PeriodListParams = {}): Promise<PaginatedResponse<Period>> {
  const { data } = await apiClient.get<PaginatedResponse<Period>>('/periods', { params });
  return data;
}

export async function createPeriod(period: Omit<Period, 'id' | 'status' | 'created_at'>): Promise<Period> {
  const { data } = await apiClient.post<Period>('/periods', period);
  return data;
}

export async function getPeriod(periodId: string): Promise<Period> {
  const { data } = await apiClient.get<Period>(`/periods/${periodId}`);
  return data;
}

export async function updatePeriod(periodId: string, period: Partial<Period>): Promise<Period> {
  const { data } = await apiClient.put<Period>(`/periods/${periodId}`, period);
  return data;
}

export async function clonePeriod(periodId: string): Promise<Period> {
  const { data } = await apiClient.post<Period>(`/periods/${periodId}/clone`);
  return data;
}

export async function getPeriodStatus(periodId: string): Promise<CompletionMatrix> {
  const { data } = await apiClient.get<CompletionMatrix>(`/periods/${periodId}/status`);
  return data;
}

export async function exportPeriodStatus(periodId: string, format: 'PDF' | 'EXCEL'): Promise<Blob> {
  const { data } = await apiClient.get<Blob>(`/periods/${periodId}/export`, {
    params: { format },
    responseType: 'blob',
  });
  return data;
}
