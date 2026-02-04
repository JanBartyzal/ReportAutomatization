import apiClient from './axios';
import {
    OpexFile,
    OpexData

} from '../../';

export const api = {
    import: {
        uploadOpex: async (files: FileList) => {
            const formData = new FormData();
            Array.from(files).forEach((file) => formData.append('file', file));
            const response = await apiClient.post<OpexFile>('/upload/import', formData, {
                headers: { 'Content-Type': 'multipart/form-data' },
            });

            console.log(response.data);
            return response.data;
        },

        getStatus: async (planId: string) => {
            const response = await apiClient.get<OpexFile>(`/api/import/status`);
            return response.data;
        },


    },

    opex: {
        getOpexData: async (opexId: string) => {
            const response = await apiClient.get<OpexData>(`/api/opex/data/${opexId}`);
            return response.data;
        },
        listUploadedFiles: async () => {
            const response = await apiClient.get<any[]>('/api/import/get-list-uploaded-files');
            return response.data;
        },
        getFileHeader: async (fileId: string) => {
            const response = await apiClient.get<any[]>(`/api/opex/get_file_header?file_id=${fileId}`);
            return response.data;
        },
        getSlideData: async (fileId: string, slideId: number) => {
            const response = await apiClient.get<any[]>(`/api/opex/get_slide_data?file_id=${fileId}&slide_id=${slideId}`);
            return response.data;
        },
        runProceedOpex: async (fileId: string) => {
            const response = await apiClient.get<any[]>(`/api/opex/run_opex?file_id=${fileId}`);
            return response.data;
        }
    }
};