import apiClient from './axios';
import type {
  ParsedData,
  SlideContent,
  TableData,
  DocumentContent,
  ProcessingLog,
  TableQueryParams,
  PaginatedResponse,
} from '@reportplatform/types';

export async function getFileData(fileId: string): Promise<ParsedData> {
  const { data } = await apiClient.get<ParsedData>(`/query/files/${fileId}/data`);
  return data;
}

export async function getSlides(fileId: string): Promise<SlideContent[]> {
  const { data } = await apiClient.get<SlideContent[]>(`/query/files/${fileId}/slides`);
  return data;
}

export async function queryTables(params: TableQueryParams = {}): Promise<PaginatedResponse<TableData>> {
  const { data } = await apiClient.get<PaginatedResponse<TableData>>('/query/tables', { params });
  return data;
}

export async function getDocument(documentId: string): Promise<DocumentContent> {
  const { data } = await apiClient.get<DocumentContent>(`/query/documents/${documentId}`);
  return data;
}

export async function getProcessingLogs(fileId: string): Promise<ProcessingLog[]> {
  const { data } = await apiClient.get<ProcessingLog[]>(`/query/processing-logs/${fileId}`);
  return data;
}
