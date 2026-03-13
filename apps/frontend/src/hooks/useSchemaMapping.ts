import { useState, useCallback } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import templatesApi, { MappingSuggestion, FormField, ExcelColumn } from '../api/templates';

export interface SchemaMappingState {
    fileId: string | null;
    formId: string | null;
    sourceColumns: ExcelColumn[];
    targetFields: FormField[];
    mappings: MappingSuggestion[];
    isLoading: boolean;
    isFetchingSuggestions: boolean;
    error: string | null;
}

export const useSchemaMapping = (fileId?: string, formId?: string) => {
    const queryClient = useQueryClient();

    const [mappings, setMappings] = useState<MappingSuggestion[]>([]);
    const [sourceColumns, setSourceColumns] = useState<ExcelColumn[]>([]);
    const [targetFields, setTargetFields] = useState<FormField[]>([]);

    // Fetch mapping suggestions from API
    const suggestionsQuery = useQuery({
        queryKey: ['mapping-suggestions', fileId, formId],
        queryFn: () => templatesApi.getMappingSuggestions(fileId!, formId!),
        enabled: !!fileId && !!formId,
    });

    // Fetch form fields
    const formFieldsQuery = useQuery({
        queryKey: ['form-fields', formId],
        queryFn: () => templatesApi.getFormFields(formId!),
        enabled: !!formId,
    });

    // Fetch Excel columns
    const excelColumnsQuery = useQuery({
        queryKey: ['excel-columns', fileId],
        queryFn: () => templatesApi.getExcelColumns(fileId!),
        enabled: !!fileId,
    });

    // Apply mapping mutation
    const applyMappingMutation = useMutation({
        mutationFn: (data: { mappings: MappingSuggestion[] }) =>
            templatesApi.applyMapping({
                fileId: fileId!,
                formId: formId!,
                mappings: data.mappings,
            }),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['forms', formId] });
        },
    });

    // Save mapping template mutation
    const saveTemplateMutation = useMutation({
        mutationFn: (data: { name: string; mappings: MappingSuggestion[] }) =>
            templatesApi.saveMappingTemplate(data.name, data.mappings),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['mapping-templates'] });
        },
    });

    // Update a single mapping
    const updateMapping = useCallback((excelColumn: string, formField: string | null) => {
        setMappings(prev =>
            prev.map(m =>
                m.excelColumn === excelColumn
                    ? { ...m, formField, confidence: formField ? 1.0 : 0 }
                    : m
            )
        );
    }, []);

    // Auto-map based on suggestions
    const autoMap = useCallback(() => {
        setMappings(prev =>
            prev.map(m => ({
                ...m,
                formField: m.suggestions.length > 0 ? m.suggestions[0] : null,
                confidence: m.suggestions.length > 0 ? 0.9 : 0,
            }))
        );
    }, []);

    // Clear all mappings
    const clearMappings = useCallback(() => {
        setMappings(prev =>
            prev.map(m => ({ ...m, formField: null, confidence: 0 }))
        );
    }, []);

    // Initialize mappings from suggestions
    const initializeMappings = useCallback((columns: ExcelColumn[], fields: FormField[]) => {
        setSourceColumns(columns);
        setTargetFields(fields);

        // Create initial mappings based on columns
        const initialMappings: MappingSuggestion[] = columns.map(col => ({
            excelColumn: col.name,
            formField: null,
            confidence: 0,
            suggestions: fields
                .filter(f => f.name.toLowerCase().includes(col.name.toLowerCase()))
                .map(f => f.name),
        }));

        setMappings(initialMappings);
    }, []);

    // Load mapping template
    const loadTemplate = useCallback((templateMappings: MappingSuggestion[]) => {
        setMappings(templateMappings);
    }, []);

    return {
        // State
        mappings,
        sourceColumns,
        targetFields,
        isLoading: suggestionsQuery.isLoading || formFieldsQuery.isLoading || excelColumnsQuery.isLoading,
        isFetchingSuggestions: suggestionsQuery.isFetching,
        error: suggestionsQuery.error?.message || formFieldsQuery.error?.message || null,

        // Data from queries
        suggestions: suggestionsQuery.data || [],
        formFields: formFieldsQuery.data || [],
        excelColumns: excelColumnsQuery.data || [],

        // Mutations
        applyMapping: applyMappingMutation.mutate,
        applyMappingAsync: applyMappingMutation.mutateAsync,
        isApplying: applyMappingMutation.isPending,
        applyError: applyMappingMutation.error,

        saveTemplate: saveTemplateMutation.mutate,
        isSaving: saveTemplateMutation.isPending,

        // Actions
        updateMapping,
        autoMap,
        clearMappings,
        initializeMappings,
        loadTemplate,

        // Query refetch
        refetch: () => {
            suggestionsQuery.refetch();
            formFieldsQuery.refetch();
            excelColumnsQuery.refetch();
        },
    };
};

export default useSchemaMapping;
