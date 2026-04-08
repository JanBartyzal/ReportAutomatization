import apiClient from './axios';
import type {
  SinkListResponse,
  SinkDetail,
  SinkCorrection,
  SinkSelection,
  CreateCorrectionRequest,
  UpsertSelectionRequest,
  SinkListFilters,
} from '@reportplatform/types';

/** List all sinks with pagination and optional filters. */
export async function listSinks(params: SinkListFilters = {}): Promise<SinkListResponse> {
  const { data } = await apiClient.get<SinkListResponse>('/query/sinks', { params });
  return data;
}

/** Get full detail of a sink with corrections applied. */
export async function getSinkDetail(parsedTableId: string): Promise<SinkDetail> {
  const { data } = await apiClient.get<SinkDetail>(`/query/sinks/${parsedTableId}`);
  return data;
}

/** Get correction history for a sink. */
export async function getSinkCorrections(parsedTableId: string): Promise<SinkCorrection[]> {
  const { data } = await apiClient.get<SinkCorrection[]>(`/query/sinks/${parsedTableId}/corrections`);
  return data;
}

/** Get selected sinks for a period. */
export async function getSinkSelections(periodId: string): Promise<SinkSelection[]> {
  const { data } = await apiClient.get<SinkSelection[]>('/query/sinks/selections', {
    params: { period_id: periodId },
  });
  return data;
}

/** Create a correction on a parsed table. */
export async function createCorrection(
  parsedTableId: string,
  request: CreateCorrectionRequest,
): Promise<{ correction_id: string; corrected_at: string }> {
  const { data } = await apiClient.post(`/query/sinks/${parsedTableId}/corrections`, request);
  return data;
}

/** Bulk create corrections. */
export async function createBulkCorrections(
  parsedTableId: string,
  requests: CreateCorrectionRequest[],
): Promise<{ corrections_created: number; correction_ids: string[] }> {
  const { data } = await apiClient.post(`/query/sinks/${parsedTableId}/corrections/bulk`, requests);
  return data;
}

/** Delete a specific correction. */
export async function deleteCorrection(
  parsedTableId: string,
  correctionId: string,
): Promise<void> {
  await apiClient.delete(`/query/sinks/${parsedTableId}/corrections/${correctionId}`);
}

/** Upsert sink selection (select/deselect for report). */
export async function upsertSelection(
  parsedTableId: string,
  request: UpsertSelectionRequest,
): Promise<{ selection_id: string; selected: boolean; selected_at: string }> {
  const { data } = await apiClient.put(`/query/sinks/${parsedTableId}/selection`, request);
  return data;
}
