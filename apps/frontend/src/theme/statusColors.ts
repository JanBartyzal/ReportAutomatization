/**
 * Status colors for report lifecycle states
 * Based on docs/UX-UI/00-project-color-overrides.md section 5
 */

export const STATUS_COLORS = {
    DRAFT: { bg: '#F3F2F1', text: '#616161' },
    SUBMITTED: { bg: '#FEF3C7', text: '#92400E' },
    UNDER_REVIEW: { bg: '#E0F2FE', text: '#0369A1' },
    IN_REVIEW: { bg: '#E0F2FE', text: '#0369A1' },
    APPROVED: { bg: '#DFF6DD', text: '#107C10' },
    REJECTED: { bg: '#FDE7E9', text: '#D13438' },
    OVERDUE: { bg: '#FDEBE2', text: '#D83B01' },
} as const;

export type ReportStatusKey = keyof typeof STATUS_COLORS;

/**
 * Get status colors for a given status
 */
export function getStatusColors(status: string): { bg: string; text: string } {
    const upperStatus = status.toUpperCase() as ReportStatusKey;
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
};

/**
 * Get status display label
 */
export function getStatusLabel(status: string): string {
    return STATUS_LABELS[status] || status;
}
