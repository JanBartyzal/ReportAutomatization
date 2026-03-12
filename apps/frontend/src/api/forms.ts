import apiClient from './axios';
import type {
  FormDefinition,
  FormVersion,
  FormResponse,
  FormAssignment,
  ExcelImportResult,
  PaginatedResponse,
  PaginationParams,
} from '@reportplatform/types';

// --- Form Builder (Admin) ---

export interface FormListParams extends PaginationParams {
  status?: string;
  scope?: 'CENTRAL' | 'LOCAL' | 'SHARED';
}

export async function listForms(params: FormListParams = {}): Promise<PaginatedResponse<FormDefinition>> {
  const { data } = await apiClient.get<PaginatedResponse<FormDefinition>>('/forms', { params });
  return data;
}

export async function createForm(form: Omit<FormDefinition, 'id' | 'status' | 'created_at' | 'updated_at'>): Promise<FormDefinition> {
  const { data } = await apiClient.post<FormDefinition>('/forms', form);
  return data;
}

export async function getForm(formId: string): Promise<FormDefinition> {
  const { data } = await apiClient.get<FormDefinition>(`/forms/${formId}`);
  return data;
}

export async function updateForm(formId: string, form: Partial<FormDefinition>): Promise<FormDefinition> {
  const { data } = await apiClient.put<FormDefinition>(`/forms/${formId}`, form);
  return data;
}

export async function deleteForm(formId: string): Promise<void> {
  await apiClient.delete(`/forms/${formId}`);
}

export async function publishForm(formId: string): Promise<FormDefinition> {
  const { data } = await apiClient.post<FormDefinition>(`/forms/${formId}/publish`);
  return data;
}

export async function closeForm(formId: string): Promise<FormDefinition> {
  const { data } = await apiClient.post<FormDefinition>(`/forms/${formId}/close`);
  return data;
}

export async function getFormVersions(formId: string): Promise<FormVersion[]> {
  const { data } = await apiClient.get<FormVersion[]>(`/forms/${formId}/versions`);
  return data;
}

export async function getFormPreview(formId: string): Promise<FormDefinition> {
  const { data } = await apiClient.get<FormDefinition>(`/forms/${formId}/preview`);
  return data;
}

export async function exportExcelTemplate(formId: string): Promise<Blob> {
  const { data } = await apiClient.get<Blob>(`/forms/${formId}/export/excel-template`, {
    responseType: 'blob',
  });
  return data;
}

// --- Form Responses (Editor) ---

export async function listFormResponses(formId: string, params: PaginationParams = {}): Promise<PaginatedResponse<FormResponse>> {
  const { data } = await apiClient.get<PaginatedResponse<FormResponse>>(`/forms/${formId}/responses`, { params });
  return data;
}

export async function createFormResponse(formId: string, response: Omit<FormResponse, 'response_id' | 'submitted_at' | 'updated_at'>): Promise<FormResponse> {
  const { data } = await apiClient.post<FormResponse>(`/forms/${formId}/responses`, response);
  return data;
}

export async function getFormResponse(formId: string, responseId: string): Promise<FormResponse> {
  const { data } = await apiClient.get<FormResponse>(`/forms/${formId}/responses/${responseId}`);
  return data;
}

export async function updateFormResponse(formId: string, responseId: string, response: Partial<FormResponse>): Promise<FormResponse> {
  const { data } = await apiClient.put<FormResponse>(`/forms/${formId}/responses/${responseId}`, response);
  return data;
}

export async function autoSaveFormResponse(formId: string, responseId: string, fields: FormResponse['fields']): Promise<FormResponse> {
  const { data } = await apiClient.put<FormResponse>(`/forms/${formId}/responses/${responseId}/auto-save`, { fields });
  return data;
}

export async function importExcel(formId: string, file: File): Promise<ExcelImportResult> {
  const formData = new FormData();
  formData.append('file', file);
  const { data } = await apiClient.post<ExcelImportResult>(`/forms/${formId}/import/excel`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
  return data;
}

// --- Assignments ---

export async function getFormAssignments(formId: string): Promise<FormAssignment> {
  const { data } = await apiClient.get<FormAssignment>(`/forms/${formId}/assignments`);
  return data;
}

export async function assignOrganizations(formId: string, orgIds: string[]): Promise<FormAssignment> {
  const { data } = await apiClient.post<FormAssignment>(`/forms/${formId}/assignments`, { org_ids: orgIds });
  return data;
}
