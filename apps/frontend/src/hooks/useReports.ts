import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  listReports, getReport, submitReport, reviewReport,
  approveReport, rejectReport, getReportHistory,
  bulkApprove, bulkReject, getReportMatrix,
  type ReportListParams,
} from '../api/reports';

export function useReports(params: ReportListParams = {}) {
  return useQuery({
    queryKey: ['reports', params],
    queryFn: () => listReports(params),
  });
}

export function useReport(reportId: string) {
  return useQuery({
    queryKey: ['reports', reportId],
    queryFn: () => getReport(reportId),
    enabled: !!reportId,
  });
}

export function useReportHistory(reportId: string) {
  return useQuery({
    queryKey: ['reports', reportId, 'history'],
    queryFn: () => getReportHistory(reportId),
    enabled: !!reportId,
  });
}

export function useReportMatrix() {
  return useQuery({
    queryKey: ['reports', 'matrix'],
    queryFn: getReportMatrix,
  });
}

export function useSubmitReport() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (reportId: string) => submitReport(reportId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['reports'] }),
  });
}

export function useReviewReport() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (reportId: string) => reviewReport(reportId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['reports'] }),
  });
}

export function useApproveReport() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (reportId: string) => approveReport(reportId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['reports'] }),
  });
}

export function useRejectReport() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ reportId, comment }: { reportId: string; comment: string }) => rejectReport(reportId, comment),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['reports'] }),
  });
}

export function useBulkApprove() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (reportIds: string[]) => bulkApprove(reportIds),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['reports'] }),
  });
}

export function useBulkReject() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ reportIds, comment }: { reportIds: string[]; comment: string }) => bulkReject(reportIds, comment),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['reports'] }),
  });
}
