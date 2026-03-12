import type { Organization, Role } from './auth';

/** Organization with children for admin view */
export interface OrganizationAdmin extends Organization {
  children?: OrganizationAdmin[];
}

/** User with assigned roles */
export interface UserRole {
  user_id: string;
  email: string;
  display_name: string;
  roles: RoleAssignment[];
}

export interface RoleAssignment {
  role: Role;
  org_id: string;
  org_name: string;
}

/** API key (key value only shown on creation) */
export interface ApiKey {
  key_id: string;
  name: string;
  scopes: string[];
  created_at: string;
  last_used_at?: string;
}

/** API key creation response (key shown once) */
export interface ApiKeyCreated {
  key_id: string;
  key: string;
  created_at: string;
}

/** Role assignment request (for admin role management) */
export interface RoleAssignmentRequest {
  target_user_id: string;
  org_id: string;
  role: string;
}

/** Role assignment response */
export interface RoleAssignmentResponse {
  target_user_id: string;
  org_id: string;
  role: string;
  assigned_by: string;
  assigned_at: string;
}

/** Failed job from DLQ */
export interface FailedJob {
  id: string;
  file_id: string;
  workflow_id: string;
  error_type: string;
  error_detail: string;
  failed_at: string;
  retry_count: number;
}
