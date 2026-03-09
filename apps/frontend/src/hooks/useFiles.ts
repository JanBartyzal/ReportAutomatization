import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { listFiles, getFile, uploadFile } from '../api/files';
import type { FileListParams, FileDetails, UploadPurpose } from '@reportplatform/types';

export function useFiles(params: FileListParams = {}) {
  return useQuery({
    queryKey: ['files', params],
    queryFn: () => listFiles(params),
  });
}

export function useFile(fileId: string) {
  return useQuery<FileDetails>({
    queryKey: ['files', fileId],
    queryFn: () => getFile(fileId),
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
