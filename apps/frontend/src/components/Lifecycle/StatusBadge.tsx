import React from 'react';
import { makeStyles, tokens } from '@fluentui/react-components';
import { getStatusColors, getStatusLabel } from '../../theme/statusColors';

const useStyles = makeStyles({
    root: {
        display: 'inline-flex',
        alignItems: 'center',
        justifyContent: 'center',
        borderRadius: tokens.borderRadiusMedium,
        fontWeight: tokens.fontWeightSemibold,
    },
    small: {
        height: '20px',
        fontSize: '10px',
        paddingLeft: tokens.spacingHorizontalSNudge,
        paddingRight: tokens.spacingHorizontalSNudge,
    },
    medium: {
        height: '24px',
        fontSize: '12px',
        paddingLeft: tokens.spacingHorizontalS,
        paddingRight: tokens.spacingHorizontalS,
    },
    large: {
        height: '28px',
        fontSize: '14px',
        paddingLeft: tokens.spacingHorizontalM,
        paddingRight: tokens.spacingHorizontalM,
    },
});

interface StatusBadgeProps {
    status: string;
    size?: 'small' | 'medium' | 'large';
}

/**
 * Status badge component for report lifecycle states.
 * Uses contextual colors from docs/UX-UI/00-project-color-overrides.md section 5.
 */
export const StatusBadge: React.FC<StatusBadgeProps> = ({
    status,
    size = 'medium'
}) => {
    const styles = useStyles();
    const colors = getStatusColors(status);
    const label = getStatusLabel(status);

    const badgeStyle = {
        backgroundColor: colors.bg,
        color: colors.text,
    };

    return (
        <span 
            className={`${styles.root} ${styles[size]}`}
            style={badgeStyle}
        >
            {label}
        </span>
    );
};

export default StatusBadge;
