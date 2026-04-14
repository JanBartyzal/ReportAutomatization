/** Widget type for dashboards */
export enum WidgetType {
  TABLE = 'TABLE',
  BAR_CHART = 'BAR_CHART',
  STACKED_BAR_CHART = 'STACKED_BAR_CHART',
  LINE_CHART = 'LINE_CHART',
  PIE_CHART = 'PIE_CHART',
  HEATMAP = 'HEATMAP',
}

/** Dashboard widget configuration */
export interface WidgetConfig {
  type: WidgetType;
  title: string;
  data_source: string;
  config: Record<string, unknown>;
}

/** Full dashboard configuration */
export interface DashboardConfig {
  id?: string;
  name: string;
  description?: string;
  is_public: boolean;
  widgets: WidgetConfig[];
  created_at?: string;
}

/** Dashboard summary for list view */
export interface DashboardSummary {
  id: string;
  name: string;
  is_public: boolean;
  created_at: string;
}

/** Dashboard query parameters */
export interface DashboardQueryParams {
  group_by: string[];
  order_by?: string;
  filters?: Record<string, unknown>;
  date_range?: DateRange;
}

export interface DateRange {
  from: string; // ISO 8601
  to: string;
}

/** Aggregated data response */
export interface AggregatedData {
  columns: string[];
  rows: Record<string, unknown>[];
  total_rows: number;
}

/** Period comparison request */
export interface PeriodComparisonRequest {
  metric: string;
  period_ids: string[];
  org_ids?: string[];
}

/** Period comparison response */
export interface ComparisonData {
  periods: PeriodValue[];
  deltas: PeriodDelta[];
}

export interface PeriodValue {
  period_id: string;
  period_name: string;
  value: number;
}

export interface PeriodDelta {
  from_period: string;
  to_period: string;
  absolute_change: number;
  percentage_change: number;
}

/** Comparison KPI definition */
export interface ComparisonKpi {
  id: string;
  name: string;
  description?: string;
  value_field: string;
  aggregation: 'SUM' | 'AVG' | 'COUNT' | 'MIN' | 'MAX';
  group_by: string[];
  source_type: 'FILE' | 'FORM' | 'ALL';
  normalization: 'NONE' | 'MONTHLY' | 'DAILY' | 'ANNUAL';
  active: boolean;
  created_at: string;
}

/** Multi-org comparison request */
export interface MultiOrgComparisonRequest {
  org_ids: string[];
  group_by: string[];
  aggregation: string;
  value_field: string;
  date_from?: string;
  date_to?: string;
  source_type?: string;
  normalization?: string;
}

/** Multi-org comparison response */
export interface MultiOrgComparisonResponse {
  rows: OrgComparisonRow[];
  metadata: Record<string, unknown>;
}

export interface OrgComparisonRow {
  org_id: string;
  group_key: Record<string, unknown>;
  value: number;
  normalized_value?: number;
}

/** Cross-type period comparison context */
export interface CrossTypeComparison {
  periods: CrossTypePeriodInfo[];
  normalizations: NormalizationInfo[];
}

export interface CrossTypePeriodInfo {
  id: string;
  name: string;
  period_type: string;
  period_code: string;
  start_date: string;
  end_date: string;
  duration_days: number;
  monthly_normalization_factor: number;
  daily_normalization_factor: number;
}

export interface NormalizationInfo {
  from_period_id: string;
  to_period_id: string;
  duration_ratio: number;
  hint: string;
}
