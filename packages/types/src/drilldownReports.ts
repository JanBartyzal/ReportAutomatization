export type DrilldownComponentType = 'KPI' | 'CHART' | 'TABLE' | 'TEXT';
export type DrilldownSourceType =
  | 'DASHBOARD_WIDGET'
  | 'NAMED_QUERY'
  | 'SINK_SELECTION'
  | 'REPORT_FORM'
  | 'RAW_SQL'
  | 'AGGREGATION';

export interface DrilldownReportSection {
  id?: string;
  section_key: string;
  title: string;
  component_type: DrilldownComponentType;
  source_type: DrilldownSourceType;
  source_ref_id?: string;
  query_config: Record<string, unknown>;
  drill_config?: Record<string, unknown>;
  display_order: number;
}

export interface DrilldownReportDefinition {
  id: string;
  org_id?: string;
  name: string;
  description?: string;
  report_type: string;
  base_period_type?: string;
  default_filters: Record<string, unknown>;
  layout_config: Record<string, unknown>;
  is_public: boolean;
  created_by?: string;
  created_at?: string;
  updated_at?: string;
  sections: DrilldownReportSection[];
}

export interface DrilldownReportCreateRequest {
  name: string;
  description?: string;
  report_type?: string;
  base_period_type?: string;
  default_filters?: Record<string, unknown>;
  layout_config?: Record<string, unknown>;
  is_public: boolean;
  sections: DrilldownReportSection[];
}

export type DrilldownReportUpdateRequest = DrilldownReportCreateRequest;

export interface DrilldownReportQueryResponse {
  report_id: string;
  filters: Record<string, unknown>;
  sections: Record<string, DrilldownSectionResult>;
}

export interface DrilldownSectionResult {
  type?: 'aggregation' | 'raw_sql';
  columns?: string[];
  rows?: unknown[] | unknown[][];
  metadata?: Record<string, unknown>;
  total_rows?: number;
}

export interface DrilldownRequest {
  section_key: string;
  filters?: Record<string, unknown>;
  selected_value?: unknown;
  page?: number;
  size?: number;
}

export interface DrilldownResult {
  report_id: string;
  section_key: string;
  filters: Record<string, unknown>;
  selected_value?: unknown;
  page: number;
  size: number;
  columns: string[];
  rows: unknown[][];
  total_rows: number;
}
