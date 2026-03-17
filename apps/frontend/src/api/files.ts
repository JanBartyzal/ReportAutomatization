import apiClient from './axios';
import type {
  FileUploadResponse,
  FileDetails,
  FileListParams,
  PaginatedResponse,
  UploadPurpose,
  FileContent,
} from '@reportplatform/types';

export async function uploadFile(
  file: File,
  purpose: UploadPurpose = 'PARSE' as UploadPurpose,
  onUploadProgress?: (progressEvent: { loaded: number; total?: number }) => void,
): Promise<FileUploadResponse> {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('upload_purpose', purpose);

  const { data } = await apiClient.post<FileUploadResponse>('/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress,
  });
  return data;
}

export async function listFiles(params: FileListParams = {}): Promise<PaginatedResponse<FileDetails>> {
  const { data } = await apiClient.get<PaginatedResponse<FileDetails>>('/files', { params });
  return data;
}

export async function getFile(fileId: string): Promise<FileDetails> {
  const { data } = await apiClient.get<FileDetails>(`/files/${fileId}`);
  return data;
}

/** Get extracted content for a file (Excel sheets, PDF pages, CSV data) */
export async function getFileContent(fileId: string): Promise<FileContent> {
  const { data } = await apiClient.get<FileContent>(`/files/${fileId}/content`);
  return data;
}

/** Get extracted tables from a file */
export async function getFileTables(fileId: string): Promise<FileContent['tables']> {
  const { data } = await apiClient.get<FileContent['tables']>(`/files/${fileId}/tables`);
  return data;
}

/** Trigger reprocessing of a file through the orchestration pipeline */
export async function reprocessFile(fileId: string): Promise<{ status: string; file_id: string }> {
  const { data } = await apiClient.post<{ status: string; file_id: string }>(`/files/${fileId}/reprocess`);
  return data;
}
