import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  listOrganizations, createOrganization, updateOrganization, deleteOrganization,
  listUsers, assignRole, removeRole,
  listApiKeys, createApiKey, revokeApiKey,
  listFailedJobs, reprocessFailedJob,
  type UserListParams, type FailedJobListParams,
} from '../api/admin';
import type { Role } from '@reportplatform/types';

// --- Organizations ---

export function useOrganizations() {
  return useQuery({
    queryKey: ['admin', 'organizations'],
    queryFn: listOrganizations,
  });
}

export function useCreateOrganization() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (org: Parameters<typeof createOrganization>[0]) => createOrganization(org),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin', 'organizations'] }),
  });
}

export function useUpdateOrganization() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ orgId, org }: { orgId: string; org: Parameters<typeof updateOrganization>[1] }) =>
      updateOrganization(orgId, org),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin', 'organizations'] }),
  });
}

export function useDeleteOrganization() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (orgId: string) => deleteOrganization(orgId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin', 'organizations'] }),
  });
}

// --- Users ---

export function useUsers(params: UserListParams = {}) {
  return useQuery({
    queryKey: ['admin', 'users', params],
    queryFn: () => listUsers(params),
  });
}

export function useAssignRole() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ userId, role, orgId }: { userId: string; role: Role; orgId: string }) =>
      assignRole(userId, role, orgId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin', 'users'] }),
  });
}

export function useRemoveRole() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ userId, role, orgId }: { userId: string; role: Role; orgId: string }) =>
      removeRole(userId, role, orgId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin', 'users'] }),
  });
}

// --- API Keys ---

export function useApiKeys() {
  return useQuery({
    queryKey: ['admin', 'apiKeys'],
    queryFn: listApiKeys,
  });
}

export function useCreateApiKey() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: Parameters<typeof createApiKey>[0]) => createApiKey(body),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin', 'apiKeys'] }),
  });
}

export function useRevokeApiKey() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (keyId: string) => revokeApiKey(keyId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin', 'apiKeys'] }),
  });
}

// --- Failed Jobs ---

export function useFailedJobs(params: FailedJobListParams = {}) {
  return useQuery({
    queryKey: ['admin', 'failedJobs', params],
    queryFn: () => listFailedJobs(params),
  });
}

export function useReprocessFailedJob() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (jobId: string) => reprocessFailedJob(jobId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin', 'failedJobs'] }),
  });
}
