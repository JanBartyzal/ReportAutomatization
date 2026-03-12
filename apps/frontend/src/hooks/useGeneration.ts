import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useState, useEffect, useCallback } from 'react';
import generationApi, {
    GenerateReportRequest,
    GenerateReportResponse,
    BatchGenerateRequest,
    BatchGenerateResponse,
    GeneratedReport,
    GenerationStatus,
} from '../api/generation';

// Query keys
export const generationKeys = {
    all: ['generation'] as const,
    lists: () => [...generationKeys.all, 'list'] as const,
    list: () => [...generationKeys.lists()] as const,
    status: (reportId: string, jobId: string) => [...generationKeys.all, 'status', reportId, jobId] as const,
    dashboardStatus: (jobId: string) => [...generationKeys.all, 'dashboard-status', jobId] as const,
};

// Hooks
export const useGenerateReport = () => {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: (data: GenerateReportRequest) => generationApi.generate(data),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: generationKeys.lists() });
        },
    });
};

export const useReportGenerationStatus = (reportId: string | null, jobId: string | null, enabled = false) => {
    return useQuery({
        queryKey: generationKeys.status(reportId || '', jobId || ''),
        queryFn: () => generationApi.getStatus(reportId!, jobId!),
        enabled: enabled && !!reportId && !!jobId,
        refetchInterval: (query) => {
            const data = query.state.data;
            if (data && data.status === 'PROCESSING') {
                return 2000; // Poll every 2 seconds while processing
            }
            if (data && data.status === 'PENDING') {
                return 1000; // Poll more frequently when pending
            }
            return false; // Stop polling when completed or failed
        },
    });
};

export const useDownloadReport = () => {
    return useMutation({
        mutationFn: (reportId: string) => generationApi.download(reportId),
    });
};

export const useBatchGenerate = () => {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: (data: BatchGenerateRequest) => generationApi.batchGenerate(data),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: generationKeys.lists() });
        },
    });
};

// Alias for backward compatibility
export const useGenerateBatch = useBatchGenerate;

export const useBatchGenerationPolling = (jobId: string | null, enabled = false) => {
    return useQuery({
        queryKey: ['batch-job-status', jobId],
        queryFn: () => generationApi.getBatchJobStatus(jobId!),
        enabled: enabled && !!jobId,
        refetchInterval: (query) => {
            const data = query.state.data;
            if (data && data.status === 'PROCESSING') {
                return 3000; // Poll every 3 seconds while processing
            }
            if (data && data.status === 'PENDING') {
                return 1000;
            }
            return false;
        },
    });
};

export const useApprovedReports = (period?: string) => {
    return useQuery({
        queryKey: ['approved-reports', period],
        queryFn: () => generationApi.getApprovedReports(period),
        enabled: !!period,
    });
};

export const useGeneratedReports = () => {
    return useQuery({
        queryKey: generationKeys.list(),
        queryFn: () => generationApi.listGeneratedReports(),
    });
};

export const useRegenerateReport = () => {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: (reportId: string) => generationApi.regenerate(reportId),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: generationKeys.lists() });
        },
    });
};

export const useDashboardGenerate = () => {
    return useMutation({
        mutationFn: ({ dashboardId, templateId }: { dashboardId: string; templateId?: string }) =>
            generationApi.generateFromDashboard(dashboardId, templateId),
    });
};

export const useDashboardGenerationStatus = (jobId: string | null, enabled = false) => {
    return useQuery({
        queryKey: generationKeys.dashboardStatus(jobId || ''),
        queryFn: () => generationApi.getDashboardGenerationStatus(jobId!),
        enabled: enabled && !!jobId,
        refetchInterval: (query) => {
            const data = query.state.data;
            if (data && data.status === 'PROCESSING') {
                return 2000;
            }
            return false;
        },
    });
};

export const useDownloadDashboardReport = () => {
    return useMutation({
        mutationFn: (jobId: string) => generationApi.downloadDashboardReport(jobId),
    });
};

// Polling hook for generation status
export const useGenerationPolling = (
    reportId: string | null,
    jobId: string | null,
    onComplete?: (response: GenerateReportResponse) => void,
    onError?: (error: Error) => void
) => {
    const [currentJobId, setCurrentJobId] = useState(jobId);
    const [currentReportId, setCurrentReportId] = useState(reportId);

    useEffect(() => {
        if (jobId) setCurrentJobId(jobId);
        if (reportId) setCurrentReportId(reportId);
    }, [jobId, reportId]);

    const statusQuery = useReportGenerationStatus(
        currentReportId,
        currentJobId,
        !!currentReportId && !!currentJobId
    );

    useEffect(() => {
        if (statusQuery.data) {
            if (statusQuery.data.status === 'COMPLETED' && onComplete) {
                onComplete(statusQuery.data);
            } else if (statusQuery.data.status === 'FAILED' && onError) {
                onError(new Error(statusQuery.data.message));
            }
        }
    }, [statusQuery.data, onComplete, onError]);

    return {
        status: statusQuery.data?.status || 'PENDING',
        downloadUrl: statusQuery.data?.downloadUrl,
        message: statusQuery.data?.message,
        isPolling: statusQuery.isFetching,
        error: statusQuery.error,
    };
};
