import axios from './axios';
import { AxiosRequestConfig } from 'axios';

// Types
export interface Template {
    id: string;
    name: string;
    version: number;
    scope: 'CENTRAL' | 'LOCAL';
    placeholderCount: number;
    createdAt: string;
    updatedAt: string;
}

export interface TemplateDetail extends Template {
    placeholders: Placeholder[];
    versions: TemplateVersion[];
    mapping?: PlaceholderMapping;
}

export interface Placeholder {
    name: string;
    type: 'TEXT' | 'TABLE' | 'CHART';
    description?: string;
}

export interface TemplateVersion {
    version: number;
    createdAt: string;
    createdBy: string;
    changes?: string;
}

export interface PlaceholderMapping {
    templateId: string;
    mappings: PlaceholderMappingItem[];
}

export interface PlaceholderMappingItem {
    placeholder: string;
    source: 'form_field' | 'table' | 'aggregated' | 'time_series';
    field?: string;
    tableName?: string;
    calculation?: string;
    chartType?: string;
}

export interface TemplateUploadRequest {
    name: string;
    scope: 'CENTRAL' | 'LOCAL';
    file: File;
}

export interface TemplateUploadResponse {
    id: string;
    name: string;
    version: number;
    placeholders: Placeholder[];
}

// API functions
const templatesApi = {
    /**
     * Get list of PPTX templates
     */
    list: async (config?: AxiosRequestConfig): Promise<Template[]> => {
        const response = await axios.get('/api/templates/pptx', config);
        return response.data;
    },

    /**
     * Get template detail by ID
     */
    getById: async (id: string, config?: AxiosRequestConfig): Promise<TemplateDetail> => {
        const response = await axios.get(`/api/templates/pptx/${id}`, config);
        return response.data;
    },

    /**
     * Upload a new PPTX template
     */
    upload: async (data: FormData, config?: AxiosRequestConfig): Promise<TemplateUploadResponse> => {
        const response = await axios.post('/api/templates/pptx', data, {
            ...config,
            headers: {
                ...config?.headers,
                'Content-Type': 'multipart/form-data',
            },
        });
        return response.data;
    },

    /**
     * Update template (upload new version)
     */
    update: async (id: string, data: FormData, config?: AxiosRequestConfig): Promise<TemplateUploadResponse> => {
        const response = await axios.put(`/api/templates/pptx/${id}`, data, {
            ...config,
            headers: {
                ...config?.headers,
                'Content-Type': 'multipart/form-data',
            },
        });
        return response.data;
    },

    /**
     * Delete/deactivate template
     */
    delete: async (id: string, config?: AxiosRequestConfig): Promise<void> => {
        await axios.delete(`/api/templates/pptx/${id}`, config);
    },

    /**
     * Get placeholders for a template
     */
    getPlaceholders: async (id: string, config?: AxiosRequestConfig): Promise<Placeholder[]> => {
        const response = await axios.get(`/api/templates/pptx/${id}/placeholders`, config);
        return response.data;
    },

    /**
     * Get placeholder mapping for a template
     */
    getMapping: async (id: string, config?: AxiosRequestConfig): Promise<PlaceholderMapping> => {
        const response = await axios.get(`/api/templates/pptx/${id}/mapping`, config);
        return response.data;
    },

    /**
     * Save placeholder mapping for a template
     */
    saveMapping: async (id: string, mappings: PlaceholderMappingItem[], config?: AxiosRequestConfig): Promise<void> => {
        await axios.post(`/api/templates/pptx/${id}/mapping`, { mappings }, config);
    },

    /**
     * Preview template with sample data
     */
    preview: async (id: string, config?: AxiosRequestConfig): Promise<Blob> => {
        const response = await axios.get(`/api/templates/pptx/${id}/preview`, {
            ...config,
            responseType: 'blob',
        });
        return response.data;
    },
};

export default templatesApi;
