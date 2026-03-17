/**
 * Health check API endpoints for monitoring.
 *
 * Provides service status, error logs, and system metrics
 * for the Health Dashboard (P5-W4-002).
 */
import apiClient from './axios';

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

/**
 * Health service registry entry (admin settings).
 */
export interface HealthServiceConfig {
    id: string;
    serviceId: string;
    displayName: string;
    healthUrl: string;
    enabled: boolean;
    sortOrder: number;
    createdAt: string;
    updatedAt: string;
}

/**
 * Request body for creating/updating a health service.
 */
export interface CreateHealthServiceRequest {
    serviceId: string;
    displayName: string;
    healthUrl: string;
    enabled: boolean;
    sortOrder: number;
}

// --- API Functions ---

/**
 * Get complete health dashboard data from backend.
 */
export async function getHealthDashboard(): Promise<HealthDashboard> {
    const { data } = await apiClient.get<HealthDashboard>('/admin/health/dashboard');
    return data;
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

// --- Health Service Registry (Admin Settings) ---

export async function getHealthServices(): Promise<HealthServiceConfig[]> {
    const { data } = await apiClient.get<HealthServiceConfig[]>('/admin/health/services');
    return data;
}

export async function createHealthService(request: CreateHealthServiceRequest): Promise<HealthServiceConfig> {
    const { data } = await apiClient.post<HealthServiceConfig>('/admin/health/services', request);
    return data;
}

export async function updateHealthService(id: string, request: CreateHealthServiceRequest): Promise<HealthServiceConfig> {
    const { data } = await apiClient.put<HealthServiceConfig>(`/admin/health/services/${id}`, request);
    return data;
}

export async function deleteHealthService(id: string): Promise<void> {
    await apiClient.delete(`/admin/health/services/${id}`);
}
