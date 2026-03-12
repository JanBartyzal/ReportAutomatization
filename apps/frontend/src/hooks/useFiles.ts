import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { listFiles, getFile, uploadFile, getFileContent, getFileTables } from '../api/files';
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

export function useFileContent(fileId: string) {
  return useQuery<FileContent>({
    queryKey: ['files', fileId, 'content'],
    queryFn: () => getFileContent(fileId),
    enabled: !!fileId,
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
