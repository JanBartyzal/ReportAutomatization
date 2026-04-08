import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  listSinks,
  getSinkDetail,
  getSinkCorrections,
  createCorrection,
  createBulkCorrections,
  deleteCorrection,
  upsertSelection,
} from '../api/sinks';
import type {
  SinkListFilters,
  CreateCorrectionRequest,
  UpsertSelectionRequest,
} from '@reportplatform/types';

export function useSinks(params: SinkListFilters = {}) {
  return useQuery({
    queryKey: ['sinks', params],
    queryFn: () => listSinks(params),
  });
}

export function useSinkDetail(parsedTableId: string) {
  return useQuery({
    queryKey: ['sinks', parsedTableId],
    queryFn: () => getSinkDetail(parsedTableId),
    enabled: !!parsedTableId,
  });
}

export function useSinkCorrections(parsedTableId: string) {
  return useQuery({
    queryKey: ['sinks', parsedTableId, 'corrections'],
    queryFn: () => getSinkCorrections(parsedTableId),
    enabled: !!parsedTableId,
  });
}

export function useCreateCorrection(parsedTableId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: CreateCorrectionRequest) => createCorrection(parsedTableId, request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sinks', parsedTableId] });
      queryClient.invalidateQueries({ queryKey: ['sinks', parsedTableId, 'corrections'] });
    },
  });
}

export function useCreateBulkCorrections(parsedTableId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (requests: CreateCorrectionRequest[]) => createBulkCorrections(parsedTableId, requests),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sinks', parsedTableId] });
    },
  });
}

export function useDeleteCorrection(parsedTableId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (correctionId: string) => deleteCorrection(parsedTableId, correctionId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sinks', parsedTableId] });
      queryClient.invalidateQueries({ queryKey: ['sinks', parsedTableId, 'corrections'] });
    },
  });
}

export function useUpsertSelection(parsedTableId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: UpsertSelectionRequest) => upsertSelection(parsedTableId, request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sinks', parsedTableId] });
      queryClient.invalidateQueries({ queryKey: ['sinks'] });
    },
  });
}
