import React from 'react';
import { Badge } from '@fluentui/react-components';
import { CheckmarkCircleRegular, WarningRegular, ErrorCircleRegular } from '@fluentui/react-icons';

interface SuggestionBadgeProps {
    confidence: number;
    showLabel?: boolean;
}

export const SuggestionBadge: React.FC<SuggestionBadgeProps> = ({
    confidence,
    showLabel = true
}) => {
    const getBadgeProps = () => {
        if (confidence >= 0.8) {
            return {
                appearance: 'filled' as const,
                color: 'success' as const,
                icon: <CheckmarkCircleRegular />,
                label: 'High',
            };
        } else if (confidence >= 0.5) {
            return {
                appearance: 'filled' as const,
                color: 'warning' as const,
                icon: <WarningRegular />,
                label: 'Medium',
            };
        }
        return {
            appearance: 'filled' as const,
            color: 'danger' as const,
            icon: <ErrorCircleRegular />,
            label: 'Low',
        };
    };

    const badgeProps = getBadgeProps();

    return (
        <Badge
            appearance={badgeProps.appearance}
            color={badgeProps.color}
            icon={badgeProps.icon}
        >
            {showLabel ? `${Math.round(confidence * 100)}%` : `${Math.round(confidence * 100)}`}
        </Badge>
    );
};

export default SuggestionBadge;
