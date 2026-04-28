/** Text Template Engine types – P8-W4 */

export type TemplateType = 'MARKDOWN' | 'HTML';
export type OutputFormat = 'PPTX' | 'EXCEL' | 'HTML_EMAIL';
export type BindingType = 'TABLE' | 'SCALAR' | 'CHART';

export interface BindingEntry {
  placeholder: string;
  type: BindingType;
  queryId: string;
  params?: Record<string, unknown>;
  chartType?: string;
  label: string;
}

export interface DataBindings {
  bindings: BindingEntry[];
}

export interface TextTemplate {
  id: string;
  orgId: string | null;
  name: string;
  description: string | null;
  templateType: TemplateType;
  content: string;
  outputFormats: OutputFormat[];
  dataBindings: DataBindings;
  scope: string;
  isSystem: boolean;
  isActive: boolean;
  version: number;
  createdBy: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateTextTemplateRequest {
  name: string;
  description?: string;
  templateType?: TemplateType;
  content: string;
  outputFormats?: OutputFormat[];
  dataBindings?: DataBindings;
  scope?: string;
}

export interface UpdateTextTemplateRequest {
  name?: string;
  description?: string;
  content?: string;
  outputFormats?: OutputFormat[];
  dataBindings?: DataBindings;
  isActive?: boolean;
}

export interface RenderRequest {
  outputFormat: OutputFormat;
  inputParams?: Record<string, unknown>;
}

export interface RenderResponse {
  downloadUrl: string;
  outputFormat: OutputFormat;
  renderedAt: string;
}
