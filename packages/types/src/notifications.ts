/** Notification types */
export enum NotificationType {
  FILE_PROCESSED = 'FILE_PROCESSED',
  FILE_FAILED = 'FILE_FAILED',
  REPORT_SUBMITTED = 'REPORT_SUBMITTED',
  REPORT_APPROVED = 'REPORT_APPROVED',
  REPORT_REJECTED = 'REPORT_REJECTED',
  DEADLINE_APPROACHING = 'DEADLINE_APPROACHING',
  DEADLINE_MISSED = 'DEADLINE_MISSED',
  BATCH_COMPLETED = 'BATCH_COMPLETED',
}

/** Notification entity */
export interface Notification {
  id: string;
  type: NotificationType;
  title: string;
  body: string;
  read: boolean;
  data: Record<string, string>;
  created_at: string;
}

/** Per-type notification preference */
export interface TypePreference {
  email: boolean;
  in_app: boolean;
}

/** User notification settings */
export interface NotificationSettings {
  email_enabled: boolean;
  in_app_enabled: boolean;
  preferences: Partial<Record<NotificationType, TypePreference>>;
}
