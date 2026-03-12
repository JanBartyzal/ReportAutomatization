import React from 'react';
import { Badge, BadgeProps } from '@fluentui/react-components';
import { GenerationStatus } from '../../api/generation';

interface StatusBadgeProps {
    status: GenerationStatus;
    size?: 'small' | 'medium' | 'large';
}

export const StatusBadge: React.FC<StatusBadgeProps> = ({
    status,
    size = 'medium',
}) => {
    const getStatusConfig = (): {
        appearance: BadgeProps['appearance'];
        color: BadgeProps['color'];
        label: string;
    } => {
        switch (status) {
            case 'PENDING':
                return {
                    appearance: 'filled',
                    color: 'warning',
                    label: 'Pending',
                };
            case 'PROCESSING':
                return {
                    appearance: 'filled',
                    color: 'info',
                    label: 'Processing',
                };
            case 'COMPLETED':
                return {
                    appearance: 'filled',
                    color: 'success',
                    label: 'Completed',
                };
            case 'FAILED':
                return {
                    appearance: 'filled',
                    color: 'danger',
                    label: 'Failed',
                };
            default:
                return {
                    appearance: 'filled',
                    color: 'neutral',
                    label: status,
                };
        }
    };

    const config = getStatusConfig();

    return (
        <Badge
            appearance={config.appearance}
            color={config.color}
            size={size}
        >
            {config.label}
        </Badge>
    );
};

// Convenience component for report status (for use in tables)
export const ReportStatusBadge: React.FC<{
    status: 'DRAFT' | 'SUBMITTED' | 'APPROVED' | 'REJECTED';
    size?: 'small' | 'medium' | 'large';
}> = ({ status, size = 'medium' }) => {
    const getReportStatusConfig = (): {
        appearance: BadgeProps['appearance'];
        color: BadgeProps['color'];
        label: string;
    } => {
        switch (status) {
            case 'DRAFT':
                return {
                    appearance: 'outline',
                    color: 'neutral',
                    label: 'Draft',
                };
            case 'SUBMITTED':
                return {
                    appearance: 'filled',
                    color: 'warning',
                    label: 'Submitted',
                };
            case 'APPROVED':
                return {
                    appearance: 'filled',
                    color: 'success',
                    label: 'Approved',
                };
            case 'REJECTED':
                return {
                    appearance: 'filled',
                    color: 'danger',
                    label: 'Rejected',
                };
            default:
                return {
                    appearance: 'filled',
                    color: 'neutral',
                    label: status,
                };
        }
    };

    const config = getReportStatusConfig();

    return (
        <Badge
            appearance={config.appearance}
            color={config.color}
            size={size}
        >
            {config.label}
        </Badge>
    );
};

export default StatusBadge;
