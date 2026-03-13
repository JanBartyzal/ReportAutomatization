/**
 * Health check API endpoints for monitoring.
 *
 * Provides service status, error logs, and system metrics
 * for the Health Dashboard (P5-W4-002).
 */
// import apiClient from './axios';

// --- Types ---

/**
 * Service health status enum.
 */
export type ServiceStatus = 'healthy' | 'degraded' | 'down';

/**
 * Individual service health information.
 */
export interface ServiceHealth {
    id: string;
    name: string;
    status: ServiceStatus;
    lastCheck: string; // ISO timestamp
    responseTime: number; // milliseconds
    uptime: number; // percentage
    version: string;
    errorCount: number;
}

/**
 * System metrics.
 */
export interface SystemMetrics {
    activeWorkflows: number;
    dlqDepth: number;
    totalProcessed: number;
    failedJobs: number;
    avgProcessingTime: number; // milliseconds
}

/**
 * Recent error log entry.
 */
export interface ErrorLogEntry {
    id: string;
    timestamp: string; // ISO timestamp
    service: string;
    level: 'error' | 'warning' | 'critical';
    message: string;
    details?: string;
}

/**
 * Complete health dashboard response.
 */
export interface HealthDashboard {
    services: ServiceHealth[];
    metrics: SystemMetrics;
    recentErrors: ErrorLogEntry[];
    grafanaUrl: string;
    lastUpdated: string;
}

// --- Mock Data ---

/**
 * Generate mock service health data.
 * Updated for P8 consolidated architecture (8 deployment units).
 * In production, this would come from actual health endpoints.
 */
function getMockServices(): ServiceHealth[] {
    return [
        {
            id: 'engine-core',
            name: 'Engine Core (Auth/Admin)',
            status: 'healthy',
            lastCheck: new Date().toISOString(),
            responseTime: 45,
            uptime: 99.9,
            version: '1.0.0',
            errorCount: 0,
        },
        {
            id: 'ms-gw',
            name: 'API Gateway (Nginx)',
            status: 'healthy',
            lastCheck: new Date().toISOString(),
            responseTime: 23,
            uptime: 99.8,
            version: '1.0.0',
            errorCount: 2,
        },
        {
            id: 'engine-ingestor',
            name: 'Engine Ingestor',
            status: 'healthy',
            lastCheck: new Date().toISOString(),
            responseTime: 89,
            uptime: 99.7,
            version: '1.0.0',
            errorCount: 0,
        },
        {
            id: 'engine-orchestrator',
            name: 'Engine Orchestrator',
            status: 'healthy',
            lastCheck: new Date().toISOString(),
            responseTime: 156,
            uptime: 99.5,
            version: '1.0.0',
            errorCount: 5,
        },
        {
            id: 'engine-data',
            name: 'Engine Data',
            status: 'healthy',
            lastCheck: new Date().toISOString(),
            responseTime: 78,
            uptime: 99.9,
            version: '1.0.0',
            errorCount: 0,
        },
        {
            id: 'engine-reporting',
            name: 'Engine Reporting',
            status: 'healthy',
            lastCheck: new Date().toISOString(),
            responseTime: 112,
            uptime: 99.8,
            version: '1.0.0',
            errorCount: 1,
        },
        {
            id: 'engine-integrations',
            name: 'Engine Integrations',
            status: 'healthy',
            lastCheck: new Date().toISOString(),
            responseTime: 234,
            uptime: 99.6,
            version: '1.0.0',
            errorCount: 0,
        },
        {
            id: 'processor-atomizers',
            name: 'Processor Atomizers',
            status: 'healthy',
            lastCheck: new Date().toISOString(),
            responseTime: 345,
            uptime: 99.7,
            version: '1.0.0',
            errorCount: 1,
        },
        {
            id: 'processor-generators',
            name: 'Processor Generators',
            status: 'healthy',
            lastCheck: new Date().toISOString(),
            responseTime: 567,
            uptime: 99.5,
            version: '1.0.0',
            errorCount: 2,
        },
    ];
}

/**
 * Generate mock system metrics.
 */
function getMockMetrics(): SystemMetrics {
    return {
        activeWorkflows: 23,
        dlqDepth: 5,
        totalProcessed: 1547,
        failedJobs: 12,
        avgProcessingTime: 3450,
    };
}

/**
 * Generate mock error logs.
 */
function getMockErrors(): ErrorLogEntry[] {
    const now = new Date();
    return [
        {
            id: 'err-001',
            timestamp: new Date(now.getTime() - 2 * 60 * 1000).toISOString(),
            service: 'ms-sink-tbl',
            level: 'warning',
            message: 'Slow query detected',
            details: 'Query took > 500ms on table form_responses',
        },
        {
            id: 'err-002',
            timestamp: new Date(now.getTime() - 15 * 60 * 1000).toISOString(),
            service: 'ms-orch',
            level: 'error',
            message: 'Workflow retry exhausted',
            details: 'File f-12345 failed after 3 retries',
        },
        {
            id: 'err-003',
            timestamp: new Date(now.getTime() - 45 * 60 * 1000).toISOString(),
            service: 'ms-atm-pptx',
            level: 'error',
            message: 'PPTX parsing failed',
            details: 'Invalid slide layout in file f-12300',
        },
        {
            id: 'err-004',
            timestamp: new Date(now.getTime() - 60 * 60 * 1000).toISOString(),
            service: 'ms-sink-tbl',
            level: 'critical',
            message: 'Database connection timeout',
            details: 'Connection pool exhausted, recovered after 30s',
        },
        {
            id: 'err-005',
            timestamp: new Date(now.getTime() - 2 * 60 * 60 * 1000).toISOString(),
            service: 'ms-gw',
            level: 'warning',
            message: 'Rate limit approaching',
            details: 'IP 10.0.0.5 at 80% of rate limit',
        },
    ];
}

// --- API Functions ---

/**
 * Get complete health dashboard data.
 * Uses mock data for now - in production would call actual backend.
 */
export async function getHealthDashboard(): Promise<HealthDashboard> {
    // Simulate network delay
    await new Promise(resolve => setTimeout(resolve, 300));

    // In production, this would be:
    // const { data } = await apiClient.get<HealthDashboard>('/health/dashboard');
    // return data;

    return {
        services: getMockServices(),
        metrics: getMockMetrics(),
        recentErrors: getMockErrors(),
        grafanaUrl: 'http://localhost:3000/grafana',
        lastUpdated: new Date().toISOString(),
    };
}

/**
 * Get individual service health.
 */
export async function getServiceHealth(serviceId: string): Promise<ServiceHealth | null> {
    const dashboard = await getHealthDashboard();
    return dashboard.services.find(s => s.id === serviceId) || null;
}

/**
 * Get system metrics.
 */
export async function getSystemMetrics(): Promise<SystemMetrics> {
    const dashboard = await getHealthDashboard();
    return dashboard.metrics;
}

/**
 * Get recent error logs.
 */
export async function getRecentErrors(limit: number = 10): Promise<ErrorLogEntry[]> {
    const dashboard = await getHealthDashboard();
    return dashboard.recentErrors.slice(0, limit);
}
