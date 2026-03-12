import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
    listVersions,
    getVersionDiff,
    createVersion,
    restoreVersion,
    type EntityType,
} from '../api/versions';

export function useVersions(entityType: EntityType, entityId: string) {
    return useQuery({
        queryKey: ['versions', entityType, entityId],
        queryFn: () => listVersions(entityType, entityId),
        enabled: !!entityId,
    });
}

export function useVersionDiff(entityType: EntityType, entityId: string, v1: number, v2: number) {
    return useQuery({
        queryKey: ['versionDiff', entityType, entityId, v1, v2],
        queryFn: () => getVersionDiff(entityType, entityId, v1, v2),
        enabled: !!entityId && v1 > 0 && v2 > 0 && v1 !== v2,
    });
}

export function useCreateVersion() {
    const qc = useQueryClient();
    return useMutation({
        mutationFn: ({ entityType, entityId, reason }: { entityType: EntityType; entityId: string; reason?: string }) =>
            createVersion(entityType, entityId, reason),
        onSuccess: (_data, variables) => {
            qc.invalidateQueries({ queryKey: ['versions', variables.entityType, variables.entityId] });
        },
    });
}

export function useRestoreVersion() {
    const qc = useQueryClient();
    return useMutation({
        mutationFn: ({ entityType, entityId, versionNumber }: { entityType: EntityType; entityId: string; versionNumber: number }) =>
            restoreVersion(entityType, entityId, versionNumber),
        onSuccess: (_data, variables) => {
            qc.invalidateQueries({ queryKey: ['versions', variables.entityType, variables.entityId] });
        },
    });
}
