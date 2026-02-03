
import { useQuery, useMutation } from '@tanstack/react-query';
import api from './axios';

export interface ColumnMetadata {
    name: string;
    type: string;
    sample_values: any[];
}

export interface SchemaInfo {
    fingerprint: string;
    column_count: number;
    columns: string[];
    matching_files: number;
    total_rows: number;
    confidence_score: number;
}

export interface AggregationPreviewResponse {
    schemas: SchemaInfo[];
}

export interface AggregatedRow {
    [key: string]: any;
}

export interface AggregatedDataResponse {
    schema_fingerprint: string;
    columns: ColumnMetadata[];
    row_count: number;
    rows: AggregatedRow[];
    source_files: string[];
}

export const analyticsKeys = {
    all: ['analytics'] as const,
    preview: (fileIds: number[]) => [...analyticsKeys.all, 'preview', { fileIds }] as const,
    data: (fingerprint: string) => [...analyticsKeys.all, 'data', fingerprint] as const,
};

const previewAggregation = async (fileIds: number[]) => {
    const response = await api.post<AggregationPreviewResponse>('/api/analytics/aggregate/preview', {
        file_ids: fileIds
    });
    return response.data;
};

const getAggregatedData = async (fingerprint: string) => {
    const response = await api.get<AggregatedDataResponse>(`/api/analytics/aggregate/${fingerprint}`);
    return response.data;
};

export const useAggregationPreview = () => {
    return useMutation({
        mutationFn: (fileIds: number[]) => previewAggregation(fileIds),
    });
};

export const useAggregatedData = (fingerprint: string | null) => {
    return useQuery({
        queryKey: analyticsKeys.data(fingerprint || ''),
        queryFn: () => getAggregatedData(fingerprint!),
        enabled: !!fingerprint,
        staleTime: 1000 * 60 * 5, // 5 minutes
    });
};
