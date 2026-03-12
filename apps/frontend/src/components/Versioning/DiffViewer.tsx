import {
    makeStyles,
    tokens,
    Button,
} from '@fluentui/react-components';
import {
    Add24Regular,
    Delete24Regular,
    Edit24Regular,
} from '@fluentui/react-icons';
import { useVersionDiff } from '../../hooks/useVersions';
import type { EntityType, FieldChange } from '../../api/versions';
import LoadingSpinner from '../LoadingSpinner';

const useStyles = makeStyles({
    container: {
        display: 'flex',
        flexDirection: 'column',
        height: '100%',
    },
    header: {
        padding: tokens.spacingHorizontalM,
        borderBottom: `1px solid ${tokens.colorNeutralStroke1}`,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
    },
    title: {
        fontWeight: tokens.fontWeightSemibold,
        fontSize: tokens.fontSizeBase16,
    },
    diffInfo: {
        fontSize: tokens.fontSizeBase14,
        color: tokens.colorNeutralForeground2,
    },
    content: {
        flex: 1,
        overflow: 'auto',
        padding: tokens.spacingHorizontalM,
    },
    changesList: {
        display: 'flex',
        flexDirection: 'column',
        gap: tokens.spacingVerticalM,
    },
    changeItem: {
        border: `1px solid ${tokens.colorNeutralStroke1}`,
        borderRadius: tokens.borderRadiusMedium,
        overflow: 'hidden',
    },
    changeHeader: {
        padding: tokens.spacingHorizontalS,
        paddingLeft: tokens.spacingHorizontalM,
        display: 'flex',
        alignItems: 'center',
        gap: tokens.spacingHorizontalS,
        fontWeight: tokens.fontWeightSemibold,
        fontSize: tokens.fontSizeBase14,
    },
    changeHeaderAdded: {
        backgroundColor: tokens.colorPaletteGreenBackground1, // Success light bg
    },
    changeHeaderRemoved: {
        backgroundColor: tokens.colorPaletteRedBackground1, // Danger light bg
    },
    changeHeaderModified: {
        backgroundColor: tokens.colorPaletteOrangeBackground1, // Warning light bg
    },
    changeIcon: {
        width: '20px',
        height: '20px',
    },
    fieldPath: {
        fontFamily: 'monospace',
        fontSize: tokens.fontSizeBase13,
    },
    diffContent: {
        display: 'grid',
        gridTemplateColumns: '1fr 1fr',
    },
    diffCell: {
        padding: tokens.spacingHorizontalM,
        fontFamily: 'monospace',
        fontSize: tokens.fontSizeBase13,
        overflow: 'auto',
        wordBreak: 'break-word',
    },
    diffCellOld: {
        backgroundColor: tokens.colorPaletteRedBackground2, // Danger light bg (more subtle)
        borderRight: `1px solid ${tokens.colorNeutralStroke1}`,
    },
    diffCellNew: {
        backgroundColor: tokens.colorPaletteGreenBackground2, // Success light bg (more subtle)
    },
    diffLabel: {
        fontSize: tokens.fontSizeBase12,
        fontWeight: tokens.fontWeightSemibold,
        padding: `${tokens.spacingVerticalXS} ${tokens.spacingHorizontalM}`,
        backgroundColor: tokens.colorNeutralBackground2,
        borderBottom: `1px solid ${tokens.colorNeutralStroke1}`,
    },
    emptyState: {
        textAlign: 'center',
        padding: tokens.spacingHorizontalXL,
        color: tokens.colorNeutralForeground2,
    },
});

const getChangeIcon = (changeType: FieldChange['change_type']) => {
    switch (changeType) {
        case 'ADDED':
            return <Add24Regular className={styles.changeIcon} style={{ color: tokens.colorGreenForeground1 }} />;
        case 'REMOVED':
            return <Delete24Regular className={styles.changeIcon} style={{ color: tokens.colorRedForeground1 }} />;
        case 'MODIFIED':
            return <Edit24Regular className={styles.changeIcon} style={{ color: tokens.colorOrangeForeground1 }} />;
    }
};

const getChangeHeaderClass = (changeType: FieldChange['change_type']) => {
    switch (changeType) {
        case 'ADDED':
            return styles.changeHeaderAdded;
        case 'REMOVED':
            return styles.changeHeaderRemoved;
        case 'MODIFIED':
            return styles.changeHeaderModified;
    }
};

const formatValue = (value: unknown): string => {
    if (value === null || value === undefined) return '<empty>';
    if (typeof value === 'object') return JSON.stringify(value, null, 2);
    return String(value);
};

interface DiffViewerProps {
    entityType: EntityType;
    entityId: string;
    fromVersion: number;
    toVersion: number;
    onClose?: () => void;
}

export default function DiffViewer({
    entityType,
    entityId,
    fromVersion,
    toVersion,
    onClose,
}: DiffViewerProps) {
    const styles = useStyles();
    const { data: diff, isLoading } = useVersionDiff(entityType, entityId, fromVersion, toVersion);

    if (isLoading) {
        return <LoadingSpinner label="Loading diff..." />;
    }

    if (!diff || diff.changes.length === 0) {
        return (
            <div className={styles.container}>
                <div className={styles.header}>
                    <span className={styles.title}>Changes</span>
                    {onClose && (
                        <Button appearance="subtle" onClick={onClose}>
                            Close
                        </Button>
                    )}
                </div>
                <div className={styles.emptyState}>
                    <p>No changes between v{fromVersion} and v{toVersion}</p>
                </div>
            </div>
        );
    }

    return (
        <div className={styles.container}>
            <div className={styles.header}>
                <span className={styles.title}>Changes</span>
                <span className={styles.diffInfo}>
                    v{fromVersion} → v{toVersion} ({diff.changes.length} changes)
                </span>
                {onClose && (
                    <Button appearance="subtle" onClick={onClose}>
                        Close
                    </Button>
                )}
            </div>

            <div className={styles.content}>
                <div className={styles.changesList}>
                    {diff.changes.map((change, index) => (
                        <div key={index} className={styles.changeItem}>
                            <div className={`${styles.changeHeader} ${getChangeHeaderClass(change.change_type)}`}>
                                {getChangeIcon(change.change_type)}
                                <span className={styles.fieldPath}>{change.field_path}</span>
                            </div>
                            <div className={styles.diffContent}>
                                {change.change_type === 'ADDED' ? (
                                    <>
                                        <div className={styles.diffCell}>
                                            <div className={styles.diffLabel}>Value</div>
                                            <div className={`${styles.diffCell} ${styles.diffCellNew}`}>
                                                {formatValue(change.new_value)}
                                            </div>
                                        </div>
                                    </>
                                ) : change.change_type === 'REMOVED' ? (
                                    <>
                                        <div className={styles.diffCell}>
                                            <div className={styles.diffLabel}>Value</div>
                                            <div className={`${styles.diffCell} ${styles.diffCellOld}`}>
                                                {formatValue(change.old_value)}
                                            </div>
                                        </div>
                                    </>
                                ) : (
                                    <>
                                        <div className={styles.diffCell}>
                                            <div className={styles.diffLabel}>v{fromVersion}</div>
                                            <div className={`${styles.diffCell} ${styles.diffCellOld}`}>
                                                {formatValue(change.old_value)}
                                            </div>
                                        </div>
                                        <div className={styles.diffCell}>
                                            <div className={styles.diffLabel}>v{toVersion}</div>
                                            <div className={`${styles.diffCell} ${styles.diffCellNew}`}>
                                                {formatValue(change.new_value)}
                                            </div>
                                        </div>
                                    </>
                                )}
                            </div>
                        </div>
                    ))}
                </div>
            </div>
        </div>
    );
}
