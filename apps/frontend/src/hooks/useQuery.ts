import { useQuery } from '@tanstack/react-query';
import { getFileData, getSlides, queryTables, getProcessingLogs } from '../api/query';
import type { TableQueryParams } from '@reportplatform/types';

export function useFileData(fileId: string) {
  return useQuery({
    queryKey: ['query', 'fileData', fileId],
    queryFn: () => getFileData(fileId),
    enabled: !!fileId,
  });
}

export function useSlides(fileId: string) {
  return useQuery({
    queryKey: ['query', 'slides', fileId],
    queryFn: () => getSlides(fileId),
    enabled: !!fileId,
  });
}

export function useTables(params: TableQueryParams = {}) {
  return useQuery({
    queryKey: ['query', 'tables', params],
    queryFn: () => queryTables(params),
  });
}

export function useProcessingLogs(fileId: string) {
  return useQuery({
    queryKey: ['query', 'processingLogs', fileId],
    queryFn: () => getProcessingLogs(fileId),
    enabled: !!fileId,
  });
}
