/** Widget type for dashboards */
export enum WidgetType {
  TABLE = 'TABLE',
  BAR_CHART = 'BAR_CHART',
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
