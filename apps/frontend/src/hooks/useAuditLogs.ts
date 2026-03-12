import { useQuery } from '@tanstack/react-query';
import { listAuditLogs, getAuditLogDetail, type AuditLogParams, type AuditLogEntry } from '../api/audit';

export function useAuditLogs(params: AuditLogParams = {}) {
    return useQuery({
        queryKey: ['auditLogs', params],
        queryFn: () => listAuditLogs(params),
    });
}

export function useAuditLogDetail(id: string) {
    return useQuery<AuditLogEntry>({
        queryKey: ['auditLog', id],
        queryFn: () => getAuditLogDetail(id),
        enabled: !!id,
    });
}
