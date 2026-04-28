import type { StorageBackend } from './sinks';

/** Data source view mode for the SinkBrowser */
export type DataSourceMode = StorageBackend | 'ALL';

/** Storage routing rule (maps to storage_routing_config DB table) */
export interface StorageRoutingRule {
  id: string;
  /** null = applies to all organisations */
  orgId: string | null;
  /** null = applies to all source types */
  sourceType: string | null;
  backend: StorageBackend;
  effectiveFrom: string;
  createdBy: string | null;
  createdAt: string;
}

/** Request body for upsert routing rule */
export interface UpsertRoutingRuleRequest {
  /** null = wildcard (all orgs) */
  orgId?: string | null;
  /** null = wildcard (all source types) */
  sourceType?: string | null;
  backend: StorageBackend;
  effectiveFrom?: string | null;
  createdBy?: string;
}

/** Data source availability and record counts – returned by /data-source-stats */
export interface DataSourceStats {
  postgresRowCount: number;
  sparkAvailable: boolean;
  /** -1 means the count is unknown (external system) */
  sparkRecordCount: number;
  blobAvailable: boolean;
  blobCount: number;
}
