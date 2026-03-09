import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  listPeriods, getPeriod, createPeriod, updatePeriod,
  clonePeriod, getPeriodStatus, type PeriodListParams,
} from '../api/periods';
import type { Period } from '@reportplatform/types';

export function usePeriods(params: PeriodListParams = {}) {
  return useQuery({
    queryKey: ['periods', params],
    queryFn: () => listPeriods(params),
  });
}

export function usePeriod(periodId: string) {
  return useQuery({
    queryKey: ['periods', periodId],
    queryFn: () => getPeriod(periodId),
    enabled: !!periodId,
  });
}

export function usePeriodStatus(periodId: string) {
  return useQuery({
    queryKey: ['periods', periodId, 'status'],
    queryFn: () => getPeriodStatus(periodId),
    enabled: !!periodId,
  });
}

export function useCreatePeriod() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (period: Parameters<typeof createPeriod>[0]) => createPeriod(period),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['periods'] }),
  });
}

export function useUpdatePeriod() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ periodId, period }: { periodId: string; period: Partial<Period> }) => updatePeriod(periodId, period),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['periods'] }),
  });
}

export function useClonePeriod() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (periodId: string) => clonePeriod(periodId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['periods'] }),
  });
}
