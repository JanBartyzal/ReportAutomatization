import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  createDrilldownReport,
  deleteDrilldownReport,
  drillDrilldownReport,
  getDrilldownReport,
  listDrilldownReports,
  queryDrilldownReport,
  updateDrilldownReport,
} from '../api/drilldownReports';
import type {
  DrilldownReportCreateRequest,
  DrilldownReportUpdateRequest,
  DrilldownRequest,
} from '@reportplatform/types';

const QUERY_KEYS = {
  all: ['drilldown-reports'] as const,
  list: () => [...QUERY_KEYS.all, 'list'] as const,
  detail: (id: string) => [...QUERY_KEYS.all, 'detail', id] as const,
  data: (id: string, filters: Record<string, unknown>) =>
    [...QUERY_KEYS.detail(id), 'data', filters] as const,
};

export function useDrilldownReports() {
  return useQuery({
    queryKey: QUERY_KEYS.list(),
    queryFn: listDrilldownReports,
    staleTime: 30_000,
  });
}

export function useDrilldownReport(id: string) {
  return useQuery({
    queryKey: QUERY_KEYS.detail(id),
    queryFn: () => getDrilldownReport(id),
    enabled: !!id,
  });
}

export function useDrilldownReportData(id: string, filters: Record<string, unknown>) {
  return useQuery({
    queryKey: QUERY_KEYS.data(id, filters),
    queryFn: () => queryDrilldownReport(id, filters),
    enabled: !!id,
  });
}

export function useCreateDrilldownReport() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: DrilldownReportCreateRequest) => createDrilldownReport(payload),
    onSuccess: () => qc.invalidateQueries({ queryKey: QUERY_KEYS.list() }),
  });
}

export function useUpdateDrilldownReport() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: DrilldownReportUpdateRequest }) =>
      updateDrilldownReport(id, payload),
    onSuccess: (_data, { id }) => {
      qc.invalidateQueries({ queryKey: QUERY_KEYS.list() });
      qc.invalidateQueries({ queryKey: QUERY_KEYS.detail(id) });
    },
  });
}

export function useDeleteDrilldownReport() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: deleteDrilldownReport,
    onSuccess: () => qc.invalidateQueries({ queryKey: QUERY_KEYS.all }),
  });
}

export function useDrilldownAction(reportId: string) {
  return useMutation({
    mutationFn: (payload: DrilldownRequest) => drillDrilldownReport(reportId, payload),
  });
}
