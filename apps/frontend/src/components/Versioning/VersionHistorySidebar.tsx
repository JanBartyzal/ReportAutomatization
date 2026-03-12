import { useState } from 'react';
import {
    Button,
    makeStyles,
    tokens,
    Badge,
    Avatar,
    Spinner,
} from '@fluentui/react-components';
import {
    History24Regular,
    ArrowClockwise24Regular,
    Eye24Regular,
} from '@fluentui/react-icons';
import { useVersions, useRestoreVersion } from '../../hooks/useVersions';
import type { VersionResponse, EntityType } from '../../api/versions';

const useStyles = makeStyles({
    sidebar: {
        width: '320px',
        borderLeft: `1px solid ${tokens.colorNeutralStroke1}`,
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        backgroundColor: tokens.colorNeutralBackground1,
    },
    header: {
        padding: tokens.spacingHorizontalM,
        borderBottom: `1px solid ${tokens.colorNeutralStroke1}`,
        display: 'flex',
        alignItems: 'center',
        gap: tokens.spacingHorizontalS,
    },
    headerTitle: {
        fontWeight: tokens.fontWeightSemibold,
        fontSize: tokens.fontSizeBase16,
        flex: 1,
    },
    list: {
        flex: 1,
        overflowY: 'auto',
    },
    versionItem: {
        padding: tokens.spacingHorizontalM,
        borderBottom: `1px solid ${tokens.colorNeutralStroke1}`,
        cursor: 'pointer',
        transition: 'background-color 0.15s ease-in-out',
        '&:hover': {
            backgroundColor: tokens.colorNeutralBackground1Hover,
        },
    },
    versionItemSelected: {
        backgroundColor: tokens.colorBrandBackground2,
    },
    versionHeader: {
        display: 'flex',
        alignItems: 'center',
        gap: tokens.spacingHorizontalS,
        marginBottom: tokens.spacingVerticalXS,
    },
    versionNumber: {
        fontWeight: tokens.fontWeightSemibold,
        fontSize: tokens.fontSizeBase14,
    },
    lockedBadge: {
        marginLeft: 'auto',
    },
    versionMeta: {
        fontSize: tokens.fontSizeBase12,
        color: tokens.colorNeutralForeground2,
        display: 'flex',
        flexDirection: 'column',
        gap: '2px',
    },
    versionActions: {
        display: 'flex',
        gap: tokens.spacingHorizontalS,
        marginTop: tokens.spacingVerticalS,
    },
    emptyState: {
        padding: tokens.spacingHorizontalXL,
        textAlign: 'center',
        color: tokens.colorNeutralForeground2,
    },
    compareSection: {
        padding: tokens.spacingHorizontalM,
        borderTop: `1px solid ${tokens.colorNeutralStroke1}`,
        backgroundColor: tokens.colorNeutralBackground2,
    },
    compareActions: {
        display: 'flex',
        gap: tokens.spacingHorizontalS,
        marginTop: tokens.spacingVerticalS,
    },
});

const formatDate = (dateString: string): string => {
    const date = new Date(dateString);
    return date.toLocaleString(undefined, {
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
    });
};

interface VersionHistorySidebarProps {
    entityType: EntityType;
    entityId: string;
    onViewDiff?: (v1: number, v2: number) => void;
    onRestore?: (version: VersionResponse) => void;
}

export default function VersionHistorySidebar({
    entityType,
    entityId,
    onViewDiff,
    onRestore,
}: VersionHistorySidebarProps) {
    const styles = useStyles();
    const { data: versions, isLoading } = useVersions(entityType, entityId);
    const restoreVersion = useRestoreVersion();

    const [selectedVersions, setSelectedVersions] = useState<number[]>([]);
    const [versionToRestore, setVersionToRestore] = useState<VersionResponse | null>(null);

    const handleVersionClick = (versionNumber: number) => {
        if (selectedVersions.includes(versionNumber)) {
            setSelectedVersions(selectedVersions.filter(v => v !== versionNumber));
        } else if (selectedVersions.length < 2) {
            setSelectedVersions([...selectedVersions, versionNumber].sort((a, b) => b - a));
        } else {
            setSelectedVersions([versionNumber]);
        }
    };

    const handleCompare = () => {
        if (selectedVersions.length === 2) {
            onViewDiff?.(selectedVersions[1], selectedVersions[0]);
        }
    };

    const handleRestore = async () => {
        if (versionToRestore) {
            await restoreVersion.mutateAsync({
                entityType,
                entityId,
                versionNumber: versionToRestore.version_number,
            });
            setVersionToRestore(null);
            onRestore?.(versionToRestore);
        }
    };

    return (
        <div className={styles.sidebar}>
            <div className={styles.header}>
                <History24Regular />
                <span className={styles.headerTitle}>Version History</span>
                <Badge appearance="filled" color="brand">
                    {versions?.length || 0}
                </Badge>
            </div>

            <div className={styles.list}>
                {isLoading ? (
                    <div className={styles.emptyState}>
                        <Spinner size="small" />
                    </div>
                ) : !versions || versions.length === 0 ? (
                    <div className={styles.emptyState}>
                        <History24Regular style={{ marginBottom: '8px', opacity: 0.5 }} />
                        <p>No versions yet</p>
                    </div>
                ) : (
                    versions.map((version) => (
                        <div
                            key={version.id}
                            className={`${styles.versionItem} ${selectedVersions.includes(version.version_number) ? styles.versionItemSelected : ''
                                }`}
                            onClick={() => handleVersionClick(version.version_number)}
                        >
                            <div className={styles.versionHeader}>
                                <span className={styles.versionNumber}>v{version.version_number}</span>
                                {version.locked && (
                                    <Badge className={styles.lockedBadge} appearance="filled" color="warning">
                                        Locked
                                    </Badge>
                                )}
                            </div>
                            <div className={styles.versionMeta}>
                                <span>{version.created_by}</span>
                                <span>{formatDate(version.created_at)}</span>
                                {version.reason && <span>Reason: {version.reason}</span>}
                            </div>
                            <div className={styles.versionActions}>
                                <Button
                                    appearance="subtle"
                                    size="small"
                                    icon={<Eye24Regular />}
                                    onClick={(e) => {
                                        e.stopPropagation();
                                        setSelectedVersions([version.version_number]);
                                        if (versions.length > 1) {
                                            const otherVersion = versions.find(v => v.version_number !== version.version_number);
                                            if (otherVersion) {
                                                onViewDiff?.(otherVersion.version_number, version.version_number);
                                            }
                                        }
                                    }}
                                >
                                    View
                                </Button>
                                {!version.locked && (
                                    <Button
                                        appearance="subtle"
                                        size="small"
                                        icon={<ArrowClockwise24Regular />}
                                        onClick={(e) => {
                                            e.stopPropagation();
                                            setVersionToRestore(version);
                                        }}
                                    >
                                        Restore
                                    </Button>
                                )}
                            </div>
                        </div>
                    ))
                )}
            </div>

            {selectedVersions.length === 2 && (
                <div className={styles.compareSection}>
                    <span>Compare v{selectedVersions[1]} → v{selectedVersions[0]}</span>
                    <div className={styles.compareActions}>
                        <Button
                            appearance="primary"
                            size="small"
                            onClick={handleCompare}
                        >
                            View Diff
                        </Button>
                        <Button
                            appearance="subtle"
                            size="small"
                            onClick={() => setSelectedVersions([])}
                        >
                            Clear
                        </Button>
                    </div>
                </div>
            )}

            {versionToRestore && (
                <div className={styles.compareSection}>
                    <span>Restore to v{versionToRestore.version_number}?</span>
                    <div className={styles.compareActions}>
                        <Button
                            appearance="primary"
                            size="small"
                            onClick={handleRestore}
                            disabled={restoreVersion.isPending}
                        >
                            {restoreVersion.isPending ? 'Restoring...' : 'Confirm'}
                        </Button>
                        <Button
                            appearance="subtle"
                            size="small"
                            onClick={() => setVersionToRestore(null)}
                        >
                            Cancel
                        </Button>
                    </div>
                </div>
            )}
        </div>
    );
}
