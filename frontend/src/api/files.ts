
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

    // PPTX goes to /upload, Excel goes to /upload/batch (or vice versa depending on intended use - checking backend)
    // Backend imports.py:
    // /upload -> upload_file (general)
    // /upload/batch -> upload_opex_file (specific for Opex/Batch context)

    // For this refactor, if we are in "UploadOpex" context, we likely want to use the batch endpoint if it does specific things,
    // OR just pass the batch_id to the standard endpoint if they are unified.
    // Looking at backend: Both accept batch_id. 
    // /upload/batch is strictly "upload_opex_file" which might tag it differently? 
    // "Similar to regular upload but tagged as OPEX file."

    // So if isOpex is true, we use /api/import/upload/batch.
    const endpoint = isOpex ? '/api/import/upload/batch' : '/api/import/upload';

    let url = endpoint;
    if (batchId) {
        url += `?batch_id=${batchId}`;
    }

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

