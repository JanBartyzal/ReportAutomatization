import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
    getHealthServices,
    createHealthService,
    updateHealthService,
    deleteHealthService,
    type CreateHealthServiceRequest,
} from '../api/health';

export function useHealthServices() {
    return useQuery({
        queryKey: ['admin', 'health-services'],
        queryFn: getHealthServices,
    });
}

export function useCreateHealthService() {
    const qc = useQueryClient();
    return useMutation({
        mutationFn: (request: CreateHealthServiceRequest) => createHealthService(request),
        onSuccess: () => {
            qc.invalidateQueries({ queryKey: ['admin', 'health-services'] });
            qc.invalidateQueries({ queryKey: ['health-dashboard'] });
        },
    });
}

export function useUpdateHealthService() {
    const qc = useQueryClient();
    return useMutation({
        mutationFn: ({ id, request }: { id: string; request: CreateHealthServiceRequest }) =>
            updateHealthService(id, request),
        onSuccess: () => {
            qc.invalidateQueries({ queryKey: ['admin', 'health-services'] });
            qc.invalidateQueries({ queryKey: ['health-dashboard'] });
        },
    });
}

export function useDeleteHealthService() {
    const qc = useQueryClient();
    return useMutation({
        mutationFn: (id: string) => deleteHealthService(id),
        onSuccess: () => {
            qc.invalidateQueries({ queryKey: ['admin', 'health-services'] });
            qc.invalidateQueries({ queryKey: ['health-dashboard'] });
        },
    });
}
