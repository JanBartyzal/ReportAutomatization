import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useRef, useCallback } from 'react';
import {
  listForms, getForm, createForm, updateForm, deleteForm,
  publishForm, closeForm, listFormResponses, getFormResponse,
  createFormResponse, updateFormResponse, autoSaveFormResponse,
  importExcel, getFormAssignments, assignOrganizations, type FormListParams,
} from '../api/forms';

export function useForms(params: FormListParams = {}) {
  return useQuery({
    queryKey: ['forms', params],
    queryFn: () => listForms(params),
  });
}

export function useForm(formId: string) {
  return useQuery({
    queryKey: ['forms', formId],
    queryFn: () => getForm(formId),
    enabled: !!formId,
  });
}

export function useCreateForm() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (form: Parameters<typeof createForm>[0]) => createForm(form),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['forms'] }),
  });
}

export function useUpdateForm() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ formId, form }: { formId: string; form: Partial<FormDefinition> }) => updateForm(formId, form),
    onSuccess: (_, { formId }) => {
      qc.invalidateQueries({ queryKey: ['forms', formId] });
      qc.invalidateQueries({ queryKey: ['forms'] });
    },
  });
}

export function useDeleteForm() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (formId: string) => deleteForm(formId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['forms'] }),
  });
}

export function usePublishForm() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (formId: string) => publishForm(formId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['forms'] }),
  });
}

export function useCloseForm() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (formId: string) => closeForm(formId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['forms'] }),
  });
}

// --- Form Assignments ---

export function useFormAssignments(formId: string) {
  return useQuery({
    queryKey: ['forms', formId, 'assignments'],
    queryFn: () => getFormAssignments(formId),
    enabled: !!formId,
  });
}

export function useAssignOrganizations(formId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (orgIds: string[]) => assignOrganizations(formId, orgIds),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['forms', formId, 'assignments'] }),
  });
}

// --- Form Responses ---

export function useFormResponses(formId: string, params: PaginationParams = {}) {
  return useQuery({
    queryKey: ['forms', formId, 'responses', params],
    queryFn: () => listFormResponses(formId, params),
    enabled: !!formId,
  });
}

export function useFormResponse(formId: string, responseId: string) {
  return useQuery({
    queryKey: ['forms', formId, 'responses', responseId],
    queryFn: () => getFormResponse(formId, responseId),
    enabled: !!formId && !!responseId,
  });
}

export function useCreateFormResponse(formId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (response: Parameters<typeof createFormResponse>[1]) => createFormResponse(formId, response),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['forms', formId, 'responses'] }),
  });
}

export function useUpdateFormResponse(formId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ responseId, response }: { responseId: string; response: Partial<FormResponse> }) =>
      updateFormResponse(formId, responseId, response),
    onSuccess: (_, { responseId }) => {
      qc.invalidateQueries({ queryKey: ['forms', formId, 'responses', responseId] });
    },
  });
}

/** Auto-save hook with 30s debounce */
export function useAutoSave(formId: string, responseId: string) {
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const mutation = useMutation({
    mutationFn: (fields: FormResponse['fields']) => autoSaveFormResponse(formId, responseId, fields),
  });

  const debouncedSave = useCallback(
    (fields: FormResponse['fields']) => {
      if (timerRef.current) clearTimeout(timerRef.current);
      timerRef.current = setTimeout(() => mutation.mutate(fields), 30_000);
    },
    [formId, responseId],
  );

  const saveNow = useCallback(
    (fields: FormResponse['fields']) => {
      if (timerRef.current) clearTimeout(timerRef.current);
      mutation.mutate(fields);
    },
    [mutation],
  );

  return { debouncedSave, saveNow, ...mutation };
}

export function useImportExcel(formId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (file: File) => importExcel(formId, file),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['forms', formId, 'responses'] }),
  });
}
