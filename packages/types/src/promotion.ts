/**
 * Smart Persistence Promotion Types
 * P7 - External Integrations & Data Optimization (FS24)
 */

/** Promotion candidate status */
export type PromotionCandidateStatus =
    | 'candidate'      // Detected as frequently used
    | 'approved'       // Admin approved for promotion
    | 'promoted'       // Table has been created
    | 'dismissed'      // Rejected by admin
    | 'migration_in_progress'  // Currently migrating data
    | 'migration_failed';       // Migration failed

/** Proposed column definition for promoted table */
export interface ProposedColumn {
    name: string;
    data_type: string;
    nullable: boolean;
    confidence: number;  // 0-1, how confident the type inference is
    sample_values: unknown[];
}

/** Proposed index for promoted table */
export interface ProposedIndex {
    name: string;
    columns: string[];
    unique: boolean;
}

/** Promotion candidate - mapping proposed for promotion */
export interface PromotionCandidate {
    id: string;
    mapping_id: string;
    mapping_name: string;
    usage_count: number;
    distinct_org_count: number;
    status: PromotionCandidateStatus;
    proposed_ddl: string;
    proposed_columns: ProposedColumn[];
    proposed_indexes: ProposedIndex[];
    admin_notes?: string;
    approved_at?: string;
    approved_by?: string;
    created_at: string;
    updated_at: string;
}

/** Request to create/update a promotion candidate */
export interface CreatePromotionCandidateRequest {
    mapping_id: string;
    proposed_ddl: string;
    proposed_columns: ProposedColumn[];
    proposed_indexes: ProposedIndex[];
}

/** Request to update a promotion candidate (admin modifications) */
export interface UpdatePromotionCandidateRequest {
    proposed_ddl?: string;
    proposed_columns?: ProposedColumn[];
    proposed_indexes?: ProposedIndex[];
    admin_notes?: string;
}

/** Promoted table - actual table created from candidate */
export interface PromotedTable {
    id: string;
    mapping_id: string;
    table_name: string;
    column_count: number;
    row_count?: number;
    size_bytes?: number;
    dual_write_until?: string;
    created_at: string;
}

/** Migration progress for data backfill */
export interface MigrationProgress {
    candidate_id: string;
    total_records: number;
    migrated_records: number;
    status: 'not_started' | 'in_progress' | 'completed' | 'failed';
    error_detail?: string;
    started_at?: string;
    completed_at?: string;
}

/** Request to approve a promotion candidate */
export interface ApprovePromotionRequest {
    dual_write_days?: number;  // Default: 7 days
}

/** Request to dismiss a promotion candidate */
export interface DismissPromotionRequest {
    reason: string;
}

/** Request to start data migration for promoted table */
export interface StartMigrationRequest {
    batch_size?: number;
}
