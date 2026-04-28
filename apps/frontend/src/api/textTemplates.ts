import apiClient from './axios';
import type {
  TextTemplate,
  CreateTextTemplateRequest,
  UpdateTextTemplateRequest,
  RenderRequest,
  RenderResponse,
} from '@reportplatform/types';

export async function listTextTemplates(): Promise<TextTemplate[]> {
  const { data } = await apiClient.get<TextTemplate[]>('/v1/reporting/text-templates');
  return data;
}

export async function getTextTemplate(id: string): Promise<TextTemplate> {
  const { data } = await apiClient.get<TextTemplate>(`/v1/reporting/text-templates/${id}`);
  return data;
}

export async function createTextTemplate(req: CreateTextTemplateRequest): Promise<TextTemplate> {
  const { data } = await apiClient.post<TextTemplate>('/v1/reporting/text-templates', req);
  return data;
}

export async function updateTextTemplate(
  id: string,
  req: UpdateTextTemplateRequest,
): Promise<TextTemplate> {
  const { data } = await apiClient.patch<TextTemplate>(
    `/v1/reporting/text-templates/${id}`,
    req,
  );
  return data;
}

export async function deleteTextTemplate(id: string): Promise<void> {
  await apiClient.delete(`/v1/reporting/text-templates/${id}`);
}

export async function renderTextTemplate(
  id: string,
  req: RenderRequest,
): Promise<RenderResponse> {
  const { data } = await apiClient.post<RenderResponse>(
    `/v1/reporting/text-templates/${id}/render`,
    req,
  );
  return data;
}
