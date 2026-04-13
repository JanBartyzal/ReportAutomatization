/**
 * Service-Now Integration Types
 * P7 - External Integrations & Data Optimization
 */

/** Service-Now authentication type */
export type ServiceNowAuthType = 'oauth2' | 'basic';

/** Integration connection status */
export type IntegrationStatus = 'active' | 'inactive' | 'error' | 'testing';

/** Service-Now table sync configuration */
export interface ServiceNowTableConfig {
    table_name: string;
    mapping_template_id?: string;
}

/** Service-Now connection configuration */
export interface ServiceNowConnection {
    id: string;
    instance_url: string;
    auth_type: ServiceNowAuthType;
    credentials_ref: string;
    org_id: string;
    status: IntegrationStatus;
    tables: ServiceNowTableConfig[];
    last_sync?: string;
    created_at: string;
    updated_at: string;
}

/** Request to create a new Service-Now connection */
export interface CreateServiceNowConnectionRequest {
    instance_url: string;
    auth_type: ServiceNowAuthType;
    credentials_ref: string;
    tables: ServiceNowTableConfig[];
    org_id?: string;
}

/** Request to test a Service-Now connection */
export interface TestConnectionRequest {
    instance_url: string;
    auth_type: ServiceNowAuthType;
    credentials_ref: string;
}

/** Response from testing a connection */
export interface TestConnectionResponse {
    success: boolean;
    message: string;
    instance_info?: {
        version: string;
        instance_name: string;
    };
}

/** Sync schedule status */
export type SyncScheduleStatus = 'idle' | 'running' | 'completed' | 'failed';

/** Sync schedule for data retrieval */
export interface SyncSchedule {
    id: string;
    integration_id: string;
    cron_expression: string;
    enabled: boolean;
    last_run?: string;
    next_run?: string;
    status: SyncScheduleStatus;
    created_at: string;
    updated_at: string;
}

/** Request to create a sync schedule */
export interface CreateSyncScheduleRequest {
    integration_id: string;
    cron_expression: string;
    enabled: boolean;
}

/** Sync job history record */
export interface SyncJobHistory {
    id: string;
    schedule_id: string;
    start_time: string;
    end_time?: string;
    records_fetched: number;
    records_stored: number;
    status: 'running' | 'completed' | 'failed';
    error_detail?: string;
}

/** Distribution rule status */
export type DistributionRuleStatus = 'active' | 'inactive';

/** Distribution rule for automated report delivery */
export interface DistributionRule {
    id: string;
    schedule_id: string;
    report_template_id: string;
    report_template_name?: string;
    recipients: string[];
    format: 'xlsx' | 'pdf' | 'pptx';
    enabled: boolean;
    created_at: string;
    updated_at: string;
}

/** Request to create a distribution rule */
export interface CreateDistributionRuleRequest {
    schedule_id: string;
    report_template_id: string;
    recipients: string[];
    format: 'xlsx' | 'pdf' | 'pptx';
    enabled: boolean;
}

/** Distribution history record */
export interface DistributionHistory {
    id: string;
    rule_id: string;
    recipients: string[];
    timestamp: string;
    status: 'sent' | 'failed' | 'pending';
    error_detail?: string;
    file_url?: string;
}

/** Service-Now table data response */
export interface ServiceNowTableData {
    table_name: string;
    records: Record<string, unknown>[];
    total_count: number;
    has_more: boolean;
}

// =============================================================================
// Excel Sync – Export Flow Types (FS27)
// Aligned with com.reportplatform.excelsync.model.dto (camelCase JSON)
// =============================================================================

/** Target type for export flow destination */
export type TargetType = 'SHAREPOINT' | 'LOCAL_PATH';

/** File naming strategy for exported workbooks */
export type FileNaming = 'CUSTOM' | 'BATCH_NAME';

/** When the export flow is triggered */
export type TriggerType = 'AUTO' | 'MANUAL';

/** Execution status of a single export run */
export type ExecutionStatus = 'PENDING' | 'RUNNING' | 'SUCCESS' | 'FAILED';

/** SharePoint connection config (serialised as JSON string inside ExportFlowDefinition) */
export interface SharePointConfig {
    siteUrl: string;
    library: string;
    folderPath?: string;
    clientId?: string;
    tenantId?: string;
}

/** Export flow execution record — ExportFlowExecutionDTO */
export interface ExportFlowExecution {
    id: string;
    flowId: string;
    triggerSource?: string;
    triggerEventId?: string;
    status: ExecutionStatus;
    rowsExported?: number;
    targetPathUsed?: string;
    errorMessage?: string;
    startedAt?: string;
    completedAt?: string;
}

/** Export flow definition — ExportFlowDTO (includes computed lastExecution) */
export interface ExportFlowDefinition {
    id: string;
    name: string;
    description?: string;
    sqlQuery: string;
    targetType: TargetType;
    targetPath: string;
    targetSheet: string;
    fileNaming: FileNaming;
    customFileName?: string;
    triggerType: TriggerType;
    triggerFilter?: string;
    /** sharepointConfig is stored as a JSON string in the backend */
    sharepointConfig?: string;
    active: boolean;
    createdBy: string;
    createdAt: string;
    updatedAt: string;
    /** Most recent execution, populated by the list/get endpoints */
    lastExecution?: ExportFlowExecution;
}

/** Request body to create an export flow — CreateExportFlowRequest */
export interface ExportFlowCreateRequest {
    name: string;
    description?: string;
    sqlQuery: string;
    targetType: TargetType;
    targetPath: string;
    targetSheet: string;
    fileNaming?: FileNaming;
    customFileName?: string;
    triggerType?: TriggerType;
    triggerFilter?: string;
    sharepointConfig?: string;
}

/** Request body to update an export flow — UpdateExportFlowRequest */
export interface ExportFlowUpdateRequest {
    name?: string;
    description?: string;
    sqlQuery?: string;
    targetType?: TargetType;
    targetPath?: string;
    targetSheet?: string;
    fileNaming?: FileNaming;
    customFileName?: string;
    triggerType?: TriggerType;
    triggerFilter?: string;
    sharepointConfig?: string;
    active?: boolean;
}

/** Response from the test/preview endpoint — ExportFlowTestResult */
export interface ExportFlowTestResponse {
    headers: string[];
    rows: Record<string, unknown>[];
    totalRows: number;
    truncated: boolean;
    error?: string;
}
