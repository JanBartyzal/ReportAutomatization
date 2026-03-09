/** Period type */
export enum PeriodType {
  MONTHLY = 'MONTHLY',
  QUARTERLY = 'QUARTERLY',
  ANNUAL = 'ANNUAL',
}

/** Period status */
export enum PeriodStatus {
  OPEN = 'OPEN',
  COLLECTING = 'COLLECTING',
  REVIEWING = 'REVIEWING',
  CLOSED = 'CLOSED',
}

/** Reporting period */
export interface Period {
  id: string;
  name: string;
  type: PeriodType;
  start_date: string; // ISO 8601
  submission_deadline: string;
  review_deadline: string;
  period_code: string;
  status: PeriodStatus;
  created_at: string;
}

/** Period completion matrix */
export interface CompletionMatrix {
  period_id: string;
  total_orgs: number;
  submitted: number;
  approved: number;
  rejected: number;
  draft: number;
  completion_percentage: number;
  orgs: OrgPeriodStatus[];
}

export interface OrgPeriodStatus {
  org_id: string;
  org_name: string;
  status: string;
  submitted_at?: string;
}
