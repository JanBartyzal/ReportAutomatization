import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import templatesApi, {
    PlaceholderMappingItem,
} from '../api/templates';

// Query keys
export const templateKeys = {
    all: ['templates'] as const,
    lists: () => [...templateKeys.all, 'list'] as const,
    list: () => [...templateKeys.lists()] as const,
    details: () => [...templateKeys.all, 'detail'] as const,
    detail: (id: string) => [...templateKeys.details(), id] as const,
    placeholders: (id: string) => [...templateKeys.all, 'placeholders', id] as const,
    mappings: (id: string) => [...templateKeys.all, 'mapping', id] as const,
};

// Hooks
export const useTemplates = () => {
    return useQuery({
        queryKey: templateKeys.list(),
        queryFn: () => templatesApi.list(),
    });
};

export const useTemplate = (id: string) => {
    return useQuery({
        queryKey: templateKeys.detail(id),
        queryFn: () => templatesApi.getById(id),
        enabled: !!id,
    });
};

export const useTemplatePlaceholders = (id: string) => {
    return useQuery({
        queryKey: templateKeys.placeholders(id),
        queryFn: () => templatesApi.getPlaceholders(id),
        enabled: !!id,
    });
};

export const useTemplateMapping = (id: string) => {
    return useQuery({
        queryKey: templateKeys.mappings(id),
        queryFn: () => templatesApi.getMapping(id),
        enabled: !!id,
    });
};

export const useUploadTemplate = () => {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: (data: FormData) => templatesApi.upload(data),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: templateKeys.lists() });
        },
    });
};

export const useUpdateTemplate = () => {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: ({ id, data }: { id: string; data: FormData }) =>
            templatesApi.update(id, data),
        onSuccess: (_, { id }) => {
            queryClient.invalidateQueries({ queryKey: templateKeys.lists() });
            queryClient.invalidateQueries({ queryKey: templateKeys.detail(id) });
        },
    });
};

export const useDeleteTemplate = () => {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: (id: string) => templatesApi.delete(id),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: templateKeys.lists() });
        },
    });
};

export const useSaveTemplateMapping = () => {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: ({ id, mappings }: { id: string; mappings: PlaceholderMappingItem[] }) =>
            templatesApi.saveMapping(id, mappings),
        onSuccess: (_, { id }) => {
            queryClient.invalidateQueries({ queryKey: templateKeys.mappings(id) });
        },
    });
};

export const useTemplatePreview = (id: string) => {
    return useQuery({
        queryKey: [...templateKeys.all, 'preview', id] as const,
        queryFn: () => templatesApi.preview(id),
        enabled: !!id,
        retry: false,
    });
};
