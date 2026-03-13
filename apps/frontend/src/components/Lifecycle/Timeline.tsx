import React from 'react';
import { 
    makeStyles, 
    tokens, 
    Body1, 
    Caption1, 
    Body2,
} from '@fluentui/react-components';
import { 
    CheckmarkCircleRegular,
    ArrowCircleRightRegular,
    Dismiss24Regular,
} from '@fluentui/react-icons';
import { StatusTransition, ReportStatus } from '@reportplatform/types';
import { getStatusColors, getStatusLabel } from '../../theme/statusColors';

const useStyles = makeStyles({
    root: {
        display: 'flex',
        flexDirection: 'column',
        padding: tokens.spacingHorizontalMNudge,
    },
    item: {
        display: 'flex',
        position: 'relative',
        paddingBottom: tokens.spacingHorizontalXL,
        '&:last-child': {
            paddingBottom: 0,
        },
    },
    connector: {
        position: 'absolute',
        top: '24px',
        left: '11px',
        bottom: 0,
        width: '2px',
        backgroundColor: tokens.colorNeutralStroke1,
        zIndex: 0,
    },
    iconContainer: {
        width: '24px',
        height: '24px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        marginRight: tokens.spacingHorizontalM,
        zIndex: 1,
        backgroundColor: tokens.colorNeutralBackground1,
    },
    content: {
        flex: 1,
        display: 'flex',
        flexDirection: 'column',
    },
    header: {
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginBottom: tokens.spacingHorizontalXXS,
    },
    comment: {
        marginTop: tokens.spacingHorizontalXS,
        padding: tokens.spacingHorizontalS,
        backgroundColor: tokens.colorNeutralBackground2,
        borderRadius: tokens.borderRadiusMedium,
        borderLeft: `3px solid ${tokens.colorNeutralStroke1}`,
    },
    statusBadge: {
        padding: '2px 8px',
        borderRadius: tokens.borderRadiusMedium,
        fontSize: '11px',
        fontWeight: tokens.fontWeightSemibold,
    }
});

interface TimelineProps {
    transitions: StatusTransition[];
}

export const Timeline: React.FC<TimelineProps> = ({ transitions }) => {
    const styles = useStyles();

    if (!transitions || transitions.length === 0) {
        return (
            <div className={styles.root}>
                <Body2 italic>No history available.</Body2>
            </div>
        );
    }

    // Sort transitions by date descending (newest first)
    const sortedTransitions = [...transitions].sort((a, b) => 
        new Date(b.changed_at).getTime() - new Date(a.changed_at).getTime()
    );

    return (
        <div className={styles.root}>
            {sortedTransitions.map((t, index) => {
                const colors = getStatusColors(t.to_status);
                const isApproved = t.to_status === ReportStatus.APPROVED;
                const isRejected = t.to_status === ReportStatus.REJECTED;

                return (
                    <div key={index} className={styles.item}>
                        {index !== sortedTransitions.length - 1 && <div className={styles.connector} />}
                        
                        <div className={styles.iconContainer}>
                            {isApproved ? (
                                <CheckmarkCircleRegular style={{ color: tokens.colorPaletteGreenForeground1 }} />
                            ) : isRejected ? (
                                <Dismiss24Regular style={{ color: tokens.colorPaletteRedForeground1 }} />
                            ) : (
                                <ArrowCircleRightRegular style={{ color: tokens.colorNeutralForeground2 }} />
                            )}
                        </div>

                        <div className={styles.content}>
                            <div className={styles.header}>
                                <div style={{ display: 'flex', alignItems: 'center', gap: tokens.spacingHorizontalS }}>
                                    <Body1><strong>{t.changed_by}</strong></Body1>
                                    <span 
                                        className={styles.statusBadge}
                                        style={{ backgroundColor: colors.bg, color: colors.text }}
                                    >
                                        {getStatusLabel(t.to_status)}
                                    </span>
                                </div>
                                <Caption1>{new Date(t.changed_at).toLocaleString()}</Caption1>
                            </div>
                            
                            <Body2>
                                {t.from_status ? `Changed from ${getStatusLabel(t.from_status)}` : 'Initiated'}
                            </Body2>

                            {t.comment && (
                                <div className={styles.comment}>
                                    <Body2 italic>"{t.comment}"</Body2>
                                </div>
                            )}
                        </div>
                    </div>
                );
            })}
        </div>
    );
};

export default Timeline;
