/** User roles in the platform */
export enum Role {
  ADMIN = 'ADMIN',
  EDITOR = 'EDITOR',
  VIEWER = 'VIEWER',
  HOLDING_ADMIN = 'HOLDING_ADMIN',
  REVIEWER = 'REVIEWER',
}

/** Organization type in holding hierarchy */
export enum OrgType {
  HOLDING = 'HOLDING',
  COMPANY = 'COMPANY',
  DIVISION = 'DIVISION',
}

/** Organization in holding hierarchy */
export interface Organization {
  id: string;
  name: string;
  type: OrgType;
  parent_id: string | null;
}

/** Current user context returned by /api/auth/me */
export interface UserContext {
  user_id: string;
  email: string;
  display_name: string;
  organizations: Organization[];
  active_org_id: string;
  roles: Role[];
}
