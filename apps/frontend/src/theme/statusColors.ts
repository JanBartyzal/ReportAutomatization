/**
 * Status colors for report lifecycle states
 * Driven by theme-config.json — section "status"
 */
import themeConfig from './theme-config.json';

export const STATUS_COLORS: Record<string, { bg: string; text: string }> = {
  DRAFT: themeConfig.status.DRAFT,
  SUBMITTED: themeConfig.status.SUBMITTED,
  UNDER_REVIEW: themeConfig.status.IN_REVIEW,
  IN_REVIEW: themeConfig.status.IN_REVIEW,
  APPROVED: themeConfig.status.APPROVED,
  REJECTED: themeConfig.status.REJECTED,
  OVERDUE: themeConfig.status.OVERDUE,
  // Export flow execution statuses (FS27)
  PENDING: themeConfig.status.PENDING,
  RUNNING: themeConfig.status.RUNNING,
  SUCCESS: themeConfig.status.SUCCESS,
  FAILED: themeConfig.status.FAILED,
};

export type ReportStatusKey = keyof typeof STATUS_COLORS;

/**
 * Get status colors for a given status
 */
export function getStatusColors(status: string): { bg: string; text: string } {
  const upperStatus = status.toUpperCase();
  return STATUS_COLORS[upperStatus] || STATUS_COLORS.DRAFT;
}

/**
 * Status display labels
 */
export const STATUS_LABELS: Record<string, string> = {
  DRAFT: 'Draft',
  SUBMITTED: 'Submitted',
  UNDER_REVIEW: 'Under Review',
  IN_REVIEW: 'In Review',
  APPROVED: 'Approved',
  REJECTED: 'Rejected',
  OVERDUE: 'Overdue',
  PENDING: 'Pending',
  RUNNING: 'Running',
  SUCCESS: 'Success',
  FAILED: 'Failed',
};

/**
 * Get status display label
 */
export function getStatusLabel(status: string): string {
  return STATUS_LABELS[status] || status;
}
