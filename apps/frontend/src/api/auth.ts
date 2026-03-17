import apiClient from './axios';
import type { UserContext, Role } from '@reportplatform/types';

/** Raw shape returned by the backend /api/auth/me endpoint */
interface MeApiResponse {
  userId: string;
  displayName: string;
  email: string;
  organizations: Array<{
    organizationId: string;
    organizationCode: string;
    organizationName: string;
    roles: string[];
  }>;
  activeOrganizationId: string;
}

/** Map the backend camelCase response to the frontend UserContext shape */
function mapMeResponse(raw: MeApiResponse): UserContext {
  const activeOrg = raw.organizations.find(
    (o) => o.organizationId === raw.activeOrganizationId,
  );

  // Collect all unique roles across all orgs the user belongs to
  const allRoles = Array.from(
    new Set(raw.organizations.flatMap((o) => o.roles)),
  ) as Role[];

  return {
    user_id: raw.userId,
    email: raw.email,
    display_name: raw.displayName,
    organizations: raw.organizations.map((o) => ({
      id: o.organizationId,
      name: o.organizationName,
      type: 'COMPANY' as any, // org type not returned by /me — default
      parent_id: null,
    })),
    active_org_id: raw.activeOrganizationId ?? (activeOrg?.organizationId || ''),
    roles: allRoles,
  };
}

export async function getMe(): Promise<UserContext> {
  const { data } = await apiClient.get<MeApiResponse>('/auth/me');
  return mapMeResponse(data);
}

export async function switchOrg(orgId: string): Promise<UserContext> {
  const { data } = await apiClient.post<MeApiResponse>('/auth/switch-org', { org_id: orgId });
  return mapMeResponse(data);
}
