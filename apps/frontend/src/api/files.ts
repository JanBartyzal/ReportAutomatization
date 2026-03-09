import apiClient from './axios';
import type {
  FileUploadResponse,
  FileDetails,
  FileListParams,
  PaginatedResponse,
  UploadPurpose,
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
