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

// Excel Import Types
export interface ExcelColumn {
    name: string;
    sampleValues: string[];
}

export interface FormField {
    id: string;
    name: string;
    label: string;
    type: string;
    required: boolean;
}

export interface MappingSuggestion {
    excelColumn: string;
    formField: string | null;
    confidence: number;
    suggestions: string[];
}

export interface ApplyMappingRequest {
    fileId: string;
    formId: string;
    mappings: MappingSuggestion[];
}

export interface ApplyMappingResponse {
    success: boolean;
    importedRowCount: number;
    errors?: string[];
}

// API functions
const templatesApi = {
    /**
     * Get list of PPTX templates
     */
    list: async (config?: AxiosRequestConfig): Promise<Template[]> => {
        const response = await axios.get('/api/templates/pptx', config);
        const data = response.data;
        // Backend returns Spring Page {content: [...]} — extract the array
        return Array.isArray(data) ? data : (data?.content ?? []);
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

    // Excel Import API Functions

    /**
     * Get mapping suggestions for Excel file columns to form fields
     */
    getMappingSuggestions: async (
        fileId: string,
        formId: string,
        config?: AxiosRequestConfig
    ): Promise<MappingSuggestion[]> => {
        const response = await axios.get('/api/templates/suggest', {
            ...config,
            params: { fileId, formId },
        });
        return response.data;
    },

    /**
     * Apply mapping to import Excel data into form
     */
    applyMapping: async (
        data: ApplyMappingRequest,
        config?: AxiosRequestConfig
    ): Promise<ApplyMappingResponse> => {
        const response = await axios.post('/api/templates/map/excel-to-form', data, config);
        return response.data;
    },

    /**
     * Get Excel file columns (preview)
     */
    getExcelColumns: async (
        fileId: string,
        config?: AxiosRequestConfig
    ): Promise<ExcelColumn[]> => {
        const response = await axios.get(`/api/files/${fileId}/columns`, config);
        return response.data;
    },

    /**
     * Get form fields for mapping
     */
    getFormFields: async (
        formId: string,
        config?: AxiosRequestConfig
    ): Promise<FormField[]> => {
        const response = await axios.get(`/api/forms/${formId}/fields`, config);
        return response.data;
    },

    /**
     * Save mapping template for reuse
     */
    saveMappingTemplate: async (
        name: string,
        mappings: MappingSuggestion[],
        config?: AxiosRequestConfig
    ): Promise<{ id: string; name: string }> => {
        const response = await axios.post('/api/templates/mapping-templates', { name, mappings }, config);
        return response.data;
    },

    /**
     * Get saved mapping templates
     */
    getMappingTemplates: async (config?: AxiosRequestConfig): Promise<{ id: string; name: string; mappings: MappingSuggestion[] }[]> => {
        const response = await axios.get('/api/templates/mapping-templates', config);
        return response.data;
    },
};

export default templatesApi;
