
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

const uploadFile = async (file: File, isOpex: boolean = false, onProgress?: (progress: number) => void) => {
    const formData = new FormData();
    formData.append('file', file);

    const endpoint = isOpex ? '/import/uploadopex' : '/import/upload';

    const response = await api.post<UploadResponse>(endpoint, formData, {
        onUploadProgress: (progressEvent) => {
            if (progressEvent.total && onProgress) {
                const percentCompleted = Math.round((progressEvent.loaded * 100) / progressEvent.total);
                onProgress(percentCompleted);
            }
        },
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
        mutationFn: ({ file, isOpex, onProgress }: { file: File; isOpex?: boolean; onProgress?: (progress: number) => void }) =>
            uploadFile(file, isOpex, onProgress),
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
