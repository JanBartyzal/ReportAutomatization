/** ServiceNow Project types – P8-W7/W9 */

export type RagStatus = 'GREEN' | 'AMBER' | 'RED';
export type ProjectSyncScope = 'ALL' | 'ACTIVE_ONLY' | 'BY_MANAGER';

export interface SnowProjectTask {
  id: string;
  sysId: string;
  number: string | null;
  shortDescription: string | null;
  parentSysId: string | null;
  state: string | null;
  milestone: boolean;
  assignedToName: string | null;
  dueDate: string | null;
  completedAt: string | null;
}

export interface SnowProjectBudget {
  id: string;
  sysId: string;
  category: string | null;
  fiscalYear: string | null;
  plannedAmount: number | null;
  actualAmount: number | null;
}

export interface SnowProject {
  id: string;
  resolverConnectionId: string;
  sysId: string;
  number: string | null;
  shortDescription: string | null;
  status: string | null;
  phase: string | null;
  managerName: string | null;
  managerEmail: string | null;
  department: string | null;
  plannedStartDate: string | null;
  plannedEndDate: string | null;
  actualStartDate: string | null;
  projectedEndDate: string | null;
  percentComplete: number | null;
  totalBudget: number | null;
  actualCost: number | null;
  projectedCost: number | null;
  budgetUtilizationPct: number | null;
  scheduleVarianceDays: number | null;
  milestoneCompletionRate: number | null;
  costForecastAccuracy: number | null;
  ragStatus: RagStatus | null;
  syncedAt: string;
  tasks?: SnowProjectTask[];
  budgets?: SnowProjectBudget[];
}

export interface SnowProjectPage {
  content: SnowProject[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface ProjectSyncConfig {
  id: string;
  connectionId: string;
  orgId: string;
  syncScope: ProjectSyncScope;
  filterManagerEmails: string | null;
  budgetCurrency: string;
  ragAmberBudgetThreshold: number;
  ragRedBudgetThreshold: number;
  ragAmberScheduleDays: number;
  ragRedScheduleDays: number;
  syncEnabled: boolean;
  lastSyncedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface UpsertProjectSyncConfigRequest {
  syncScope?: ProjectSyncScope;
  filterManagerEmails?: string;
  budgetCurrency: string;
  ragAmberBudgetThreshold?: number;
  ragRedBudgetThreshold?: number;
  ragAmberScheduleDays?: number;
  ragRedScheduleDays?: number;
  syncEnabled?: boolean;
}

export interface ProjectListFilters {
  ragStatus?: RagStatus;
  status?: string;
  managerEmail?: string;
  connectionId?: string;
  page?: number;
  size?: number;
}
