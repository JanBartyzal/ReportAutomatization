/** Field types supported by form builder */
export enum FormFieldType {
  TEXT = 'TEXT',
  NUMBER = 'NUMBER',
  PERCENTAGE = 'PERCENTAGE',
  DATE = 'DATE',
  DROPDOWN = 'DROPDOWN',
  TABLE = 'TABLE',
  FILE_ATTACHMENT = 'FILE_ATTACHMENT',
}

/** Form status */
export enum FormStatus {
  DRAFT = 'DRAFT',
  PUBLISHED = 'PUBLISHED',
  CLOSED = 'CLOSED',
}

/** Form field definition */
export interface FormField {
  field_id: string;
  name: string;
  type: FormFieldType;
  required: boolean;
  validation_rules?: ValidationRules;
  section?: string;
  order: number;
  options?: string[]; // for DROPDOWN
}

export interface ValidationRules {
  min?: number;
  max?: number;
  pattern?: string;
  depends_on?: FieldDependency;
}

export interface FieldDependency {
  field_id: string;
  condition: string; // e.g., "> 0"
}

/** Form scope */
export type FormScope = 'CENTRAL' | 'LOCAL' | 'SHARED_WITHIN_HOLDING';

/** Form definition */
export interface FormDefinition {
  id?: string;
  name: string;
  description?: string;
  report_type: string;
  status: FormStatus;
  scope: FormScope;
  owner_org_id?: string;
  released_at?: string;
  released_by?: string;
  fields: FormField[];
  created_at?: string;
  updated_at?: string;
}

/** Form version */
export interface FormVersion {
  version_id: string;
  form_id: string;
  version_number: number;
  fields: FormField[];
  created_at: string;
}

/** Form response (filled-in data) */
export interface FormResponse {
  response_id: string;
  form_id: string;
  form_version_id: string;
  org_id: string;
  fields: FormFieldValue[];
  status: 'DRAFT' | 'SUBMITTED';
  submitted_at?: string;
  updated_at: string;
}

export interface FormFieldValue {
  field_id: string;
  value: string;
  comment?: string;
}

/** Form assignment to organizations */
export interface FormAssignment {
  form_id: string;
  org_ids: string[];
}

/** Excel import result */
export interface ExcelImportResult {
  rows_imported: number;
  warnings: string[];
  errors: string[];
}
