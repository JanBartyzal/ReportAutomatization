import axios from './axios';
import { AxiosRequestConfig } from 'axios';

// Types
export interface GenerateReportRequest {
    reportId: string;
    templateId?: string;
    period?: string;
}

export interface GenerateReportResponse {
    jobId: string;
    status: GenerationStatus;
    message: string;
    downloadUrl?: string;
    generatedAt?: string;
}

export type GenerationStatus =
    | 'PENDING'
    | 'PROCESSING'
    | 'COMPLETED'
    | 'FAILED';

export interface BatchGenerateRequest {
    reportIds: string[];
    templateId?: string;
}

export interface BatchGenerateResponse {
    total: number;
    successful: number;
    failed: number;
    results: GenerateReportResponse[];
}

export interface BatchJobStatus {
    jobId: string;
    status: GenerationStatus;
    total: number;
    completed: number;
    failed: number;
    results: Record<string, string | null>; // reportId -> downloadUrl
    message?: string;
}

export interface ApprovedReport {
    id: string;
    name: string;
    status: 'APPROVED';
    period: string;
    generatedAt?: string;
    downloadUrl?: string;
}

export interface GeneratedReport {
    id: string;
    reportId: string;
    templateId: string;
    templateName: string;
    generatedAt: string;
    downloadUrl: string;
    status: GenerationStatus;
    fileSize?: number;
}

// API functions
const generationApi = {
    /**
     * Generate PPTX for a single report
     */
    generate: async (data: GenerateReportRequest, config?: AxiosRequestConfig): Promise<GenerateReportResponse> => {
        const response = await axios.post(`/api/reports/${data.reportId}/generate`, data, config);
        return response.data;
    },

    /**
     * Get generation status
     */
    getStatus: async (reportId: string, jobId: string, config?: AxiosRequestConfig): Promise<GenerateReportResponse> => {
        const response = await axios.get(`/api/reports/${reportId}/generation-status?jobId=${jobId}`, config);
        return response.data;
    },

    /**
     * Download generated PPTX
     */
    download: async (reportId: string, config?: AxiosRequestConfig): Promise<Blob> => {
        const response = await axios.get(`/api/reports/${reportId}/download`, {
            ...config,
            responseType: 'blob',
        });
        return response.data;
    },

    /**
     * Generate PPTX for multiple reports (batch)
     */
    batchGenerate: async (data: BatchGenerateRequest, config?: AxiosRequestConfig): Promise<BatchGenerateResponse> => {
        const response = await axios.post('/api/reports/batch-generate', data, config);
        return response.data;
    },

    /**
     * Generate PPTX from dashboard
     */
    generateFromDashboard: async (dashboardId: string, templateId?: string, config?: AxiosRequestConfig): Promise<GenerateReportResponse> => {
        const response = await axios.post('/api/dashboards/generate-pptx', {
            dashboardId,
            templateId,
        }, config);
        return response.data;
    },

    /**
     * Get dashboard generation status
     */
    getDashboardGenerationStatus: async (jobId: string, config?: AxiosRequestConfig): Promise<GenerateReportResponse> => {
        const response = await axios.get(`/api/dashboards/generate-pptx/${jobId}`, config);
        return response.data;
    },

    /**
     * Download dashboard-generated PPTX
     */
    downloadDashboardReport: async (jobId: string, config?: AxiosRequestConfig): Promise<Blob> => {
        const response = await axios.get(`/api/dashboards/generate-pptx/${jobId}/download`, {
            ...config,
            responseType: 'blob',
        });
        return response.data;
    },

    /**
     * Get list of generated reports
     */
    listGeneratedReports: async (config?: AxiosRequestConfig): Promise<GeneratedReport[]> => {
        const response = await axios.get('/api/reports/generated', config);
        return response.data;
    },

    /**
     * Regenerate report with current data
     */
    regenerate: async (reportId: string, config?: AxiosRequestConfig): Promise<GenerateReportResponse> => {
        const response = await axios.post(`/api/reports/${reportId}/regenerate`, {}, config);
        return response.data;
    },

    /**
     * Get batch generation job status
     */
    getBatchJobStatus: async (jobId: string, config?: AxiosRequestConfig): Promise<BatchJobStatus> => {
        const response = await axios.get(`/api/reports/batch-generate/${jobId}/status`, config);
        return response.data;
    },

    /**
     * Get approved reports for a period (for batch generation)
     */
    getApprovedReports: async (period?: string, config?: AxiosRequestConfig): Promise<ApprovedReport[]> => {
        const params = period ? `?period=${encodeURIComponent(period)}` : '';
        const response = await axios.get(`/api/reports/approved${params}`, config);
        return response.data;
    },

    /**
     * Generate report (alias for generate)
     */
    generateReport: async (data: GenerateReportRequest, config?: AxiosRequestConfig): Promise<GenerateReportResponse> => {
        const response = await axios.post(`/api/reports/${data.reportId}/generate`, data, config);
        return response.data;
    },

    /**
     * Get generation job status (alias for getStatus)
     */
    getJobStatus: async (jobId: string, config?: AxiosRequestConfig): Promise<GenerateReportResponse> => {
        const response = await axios.get(`/api/reports/generation-jobs/${jobId}`, config);
        return response.data;
    },

    /**
     * Get all generation jobs
     */
    getGenerationJobs: async (reportId?: string, config?: AxiosRequestConfig): Promise<GenerateReportResponse[]> => {
        const params = reportId ? `?reportId=${encodeURIComponent(reportId)}` : '';
        const response = await axios.get(`/api/reports/generation-jobs${params}`, config);
        return response.data;
    },
};

export default generationApi;
