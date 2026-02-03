
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from './axios';

export interface UploadedFile {
    id: number;
    filename: string;
    md5hash: string;
    region: string;
    created_at: string;
}

export interface UploadResponse {
    message: string;
    user: string;
}

export const fileKeys = {
    all: ['files'] as const,
    lists: () => [...fileKeys.all, 'list'] as const,
};

const uploadFile = async (file: File, isOpex: boolean = false, batchId?: string, onProgress?: (progress: number) => void) => {
    const formData = new FormData();
    formData.append('file', file);

    // PPTX goes to /upload, Excel goes to /upload/batch (or vice versa depending on intended use)
    // Based on backend imports.py:
    // /upload handles both but is generally for PPTX
    // /upload/batch also handles both but is tagged as OPEX
    const endpoint = isOpex ? '/api/import/upload/batch' : '/api/import/upload';

    // We need a batch_id. If not provided, we might need to fetch one, 
    // but for now let's assume it's passed or we append a placeholder if backend allows (it doesn't yet)
    const url = batchId ? `${endpoint}?batch_id=${batchId}` : endpoint;

    const response = await api.post<UploadResponse>(url, formData, {
        onUploadProgress: (progressEvent) => {
            if (progressEvent.total && onProgress) {
                const percentCompleted = Math.round((progressEvent.loaded * 100) / progressEvent.total);
                onProgress(percentCompleted);
            }
        },
    });
    return response.data;
};

export interface Report {
    id: number;
    title?: string;
    owner?: string;
    region?: string;
    appendix?: any;
}

export const getMyReports = async () => {
    const response = await api.get<Report[]>('/api/reports/my-reports');
    return response.data;
};

export const uploadExcelAppendix = async (reportId: number, file: File) => {
    const formData = new FormData();
    formData.append('report_id', reportId.toString());
    formData.append('file', file);

    const response = await api.post('/api/import/upload/appendix', formData, {
        headers: {},
    });
    return response.data;
};


const getUploadedFiles = async () => {
    console.log("Fetching uploaded files...");
    console.log(api.defaults.baseURL);
    console.log(api.defaults.headers);
    const response = await api.get<UploadedFile[]>('/api/import/get-list-uploaded-files');
    console.log(response.data);
    return response.data;
};

// React Query Hooks

export const useUploadFile = () => {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: ({ file, isOpex, batchId, onProgress }: { file: File; isOpex?: boolean; batchId?: string; onProgress?: (progress: number) => void }) =>
            uploadFile(file, isOpex, batchId, onProgress),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: fileKeys.lists() });
        },
    });
};

export const useFiles = () => {
    return useQuery({
        queryKey: fileKeys.lists(),
        queryFn: getUploadedFiles,
        // Poll every 10 seconds to check for updates (e.g. if other users upload or status changes)
        // Though here we assume list is static unless we upload
        staleTime: 1000 * 60,
    });
};

export const useMyReports = () => {
    return useQuery({
        queryKey: ['reports', 'my'] as const,
        queryFn: getMyReports,
        staleTime: 1000 * 60 * 5,
    });
};

export const useUploadAppendix = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: ({ reportId, file }: { reportId: number; file: File }) =>
            uploadExcelAppendix(reportId, file),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['reports'] });
        },
    });
};

