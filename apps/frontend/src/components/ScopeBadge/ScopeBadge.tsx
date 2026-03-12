/**
 * ScopeBadge Component
 * Displays badge for CENTRAL/LOCAL/SHARED scope
 * Per docs/UX-UI/02-design-system.md and 03-figma-components.md
 */

import React from 'react';
import { Badge } from '@fluentui/react-components';
import { makeStyles, tokens } from '@fluentui/react-components';

// Scope types
export type ScopeType = 'CENTRAL' | 'LOCAL' | 'SHARED';

// Badge color mapping per scope type (Info/Success/Warning semantics)
const scopeColors: Record<ScopeType, 'informative' | 'success' | 'warning'> = {
    CENTRAL: 'informative',  // Blue - central/holding level
    LOCAL: 'success',       // Green - local/company level
    SHARED: 'warning',      // Orange - shared between orgs
};

// Badge appearance mapping
const scopeAppearances: Record<ScopeType, 'filled' | 'outline'> = {
    CENTRAL: 'filled',
    LOCAL: 'filled',
    SHARED: 'filled',
};

const useStyles = makeStyles({
    root: {
        display: 'inline-flex',
        alignItems: 'center',
        gap: tokens.spacingHorizontalXS,
    },
    badge: {
        fontWeight: tokens.fontWeightSemibold,
        fontSize: tokens.fontSizeBase100,
    },
});

interface ScopeBadgeProps {
    /** Scope type: CENTRAL, LOCAL, or SHARED */
    scope: ScopeType;
    /** Optional: show icon or additional info */
    showIcon?: boolean;
    /** Optional: override size */
    size?: 'small' | 'medium' | 'large';
    /** Optional: custom class name */
    className?: string;
    /** Optional: inline display */
    inline?: boolean;
}

/**
 * ScopeBadge component displays scope type with appropriate colors
 * 
 * @example
 * ```tsx
 * <ScopeBadge scope="LOCAL" />
 * <ScopeBadge scope="CENTRAL" size="large" />
 * <ScopeBadge scope="SHARED" showIcon />
 * ```
 */
export const ScopeBadge: React.FC<ScopeBadgeProps> = ({
    scope,
    showIcon = false,
    size = 'small',
    className,
    inline = true,
}) => {
    const styles = useStyles();

    const color = scopeColors[scope as ScopeType];
    const appearance = scopeAppearances[scope as ScopeType];

    // Size mapping
    const sizeMap = {
        small: 'small',
        medium: 'medium',
        large: 'large',
    };

    return (
        <span className={`${styles.root} ${className || ''}`} style={{ display: inline ? 'inline-flex' : 'flex' }}>
            {showIcon && <ScopeIcon scope={scope} />}
            <Badge
                appearance={appearance}
                color={color}
                size={sizeMap[size]}
                className={styles.badge}
            >
                {scope}
            </Badge>
        </span>
    );
};

/**
 * Small scope icon component
 */
const ScopeIcon: React.FC<{ scope: ScopeType }> = ({ scope }) => {
    const iconStyles = makeStyles({
        root: {
            display: 'flex',
            alignItems: 'center',
            fontSize: '12px',
        },
    });
    const iconClasses = iconStyles();

    switch (scope) {
        case 'CENTRAL':
            return <span className={iconClasses.root} title="Central (Holding)">🏢</span>;
        case 'LOCAL':
            return <span className={iconClasses.root} title="Local (Company)">🏪</span>;
        case 'SHARED':
            return <span className={iconClasses.root} title="Shared">🔗</span>;
        default:
            return null;
    }
};

/**
 * Get display color for scope type (for custom styling)
 * Returns CSS custom property or hex color
 */
export function getScopeColor(scope: ScopeType): string {
    const colorMap: Record<ScopeType, string> = {
        CENTRAL: 'var(--colorInformalForeground1)',  // Blue
        LOCAL: 'var(--colorSuccessForeground1)',      // Green  
        SHARED: 'var(--colorWarningForeground1)',    // Orange
    };
    return colorMap[scope];
}

/**
 * Get scope label with more detail
 */
export function getScopeLabel(scope: ScopeType): string {
    const labelMap: Record<ScopeType, string> = {
        CENTRAL: 'Central (Holding)',
        LOCAL: 'Local (Company)',
        SHARED: 'Shared',
    };
    return labelMap[scope];
}

export default ScopeBadge;