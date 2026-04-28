/** Named Query Catalog types – P8-W5 */

export interface NamedQuery {
  id: string;
  orgId: string | null;
  name: string;
  description: string | null;
  dataSourceType: string;
  sqlQuery: string;
  paramsSchema: Record<string, unknown> | null;
  isSystem: boolean;
  isActive: boolean;
  createdBy: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateNamedQueryRequest {
  name: string;
  description?: string;
  dataSourceType: string;
  sqlQuery: string;
  paramsSchema?: Record<string, unknown>;
}

export interface UpdateNamedQueryRequest {
  name?: string;
  description?: string;
  sqlQuery?: string;
  paramsSchema?: Record<string, unknown>;
  isActive?: boolean;
}

export interface NamedQueryExecuteRequest {
  params?: Record<string, unknown>;
  limit?: number;
}

export interface NamedQueryResult {
  queryId: string;
  queryName: string;
  rows: Record<string, unknown>[];
  totalCount: number;
  executedAt: string;
}
