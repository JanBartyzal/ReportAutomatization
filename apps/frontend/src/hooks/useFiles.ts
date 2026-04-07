import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { listFiles, getFile, uploadFile, getFileContent, getFileTables, reprocessFile } from '../api/files';
import type { FileListParams, FileDetails, UploadPurpose, FileContent } from '@reportplatform/types';

export function useFiles(params: FileListParams = {}) {
  return useQuery({
    queryKey: ['files', params],
    queryFn: () => listFiles(params),
  });
}

export function useFile(fileId: string, options?: { pollingInterval?: number }) {
  const { pollingInterval = 0 } = options || {};

  return useQuery<FileDetails>({
    queryKey: ['files', fileId],
    enabled: !!fileId,
    refetchInterval: pollingInterval > 0 ? pollingInterval : undefined,
    queryFn: async () => {
      const file = await getFile(fileId);
      return file;
    },
  });
}

export function useFileContent(fileId: string, mimeType?: string) {
  return useQuery<FileContent>({
    queryKey: ['files', fileId, 'content', mimeType],
    queryFn: () => getFileContent(fileId, mimeType),
    // Wait until mimeType is resolved so we call the right endpoint
    enabled: !!fileId && mimeType !== undefined,
  });
}

export function useFileTables(fileId: string) {
  return useQuery({
    queryKey: ['files', fileId, 'tables'],
    queryFn: () => getFileTables(fileId),
    enabled: !!fileId,
  });
}

export function useUpload() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ file, purpose, onProgress }: {
      file: File;
      purpose?: UploadPurpose;
      onProgress?: (event: { loaded: number; total?: number }) => void;
    }) => uploadFile(file, purpose, onProgress),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['files'] });
    },
  });
}

export function useReprocessFile() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (fileId: string) => reprocessFile(fileId),
    onSuccess: (_data, fileId) => {
      queryClient.invalidateQueries({ queryKey: ['files', fileId] });
    },
  });
}
