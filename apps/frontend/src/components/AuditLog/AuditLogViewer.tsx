import { useState } from 'react';
import {
    makeStyles,
    tokens,
    Button,
    Input,
    Dropdown,
    DropdownTrigger,
    Option,
    Badge,
    Table,
    TableHeader,
    TableRow,
    TableHeaderCell,
    TableCell,
    TableBody,
    Card,
} from '@fluentui/react-components';
import {
    ArrowDownload24Regular,
    Filter24Regular,
    Eye24Regular,
} from '@fluentui/react-icons';
import { useAuditLogs } from '../../hooks/useAuditLogs';
import { exportAuditLogs, type AuditLogParams, type AuditAction, type AuditEntityType } from '../../api/audit';
import LoadingSpinner from '../LoadingSpinner';

const useStyles = makeStyles({
    container: {
        display: 'flex',
        flexDirection: 'column',
        gap: tokens.spacingHorizontalL,
    },
    filters: {
        display: 'flex',
        gap: tokens.spacingHorizontalM,
        flexWrap: 'wrap',
        padding: tokens.spacingHorizontalM,
        backgroundColor: tokens.colorNeutralBackground2,
        borderRadius: tokens.borderRadiusMedium,
    },
    filterGroup: {
        display: 'flex',
        flexDirection: 'column',
        gap: tokens.spacingVerticalXS,
    },
    filterLabel: {
        fontSize: tokens.fontSizeBase12,
        fontWeight: tokens.fontWeightSemibold,
    },
    actions: {
        display: 'flex',
        gap: tokens.spacingHorizontalS,
        marginLeft: 'auto',
    },
    tableWrapper: {
        overflowX: 'auto',
    },
    table: {
        width: '100%',
    },
    actionBadge: {
        display: 'inline-flex',
    },
    entityLink: {
        color: tokens.colorBrandForeground1,
        cursor: 'pointer',
        textDecoration: 'none',
        '&:hover': {
            textDecoration: 'underline',
        },
    },
    detailModal: {
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        backgroundColor: 'rgba(0, 0, 0, 0.5)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 1000,
    },
    detailContent: {
        backgroundColor: tokens.colorNeutralBackground1,
        borderRadius: tokens.borderRadiusLarge,
        padding: tokens.spacingHorizontalL,
        maxWidth: '600px',
        width: '90%',
        maxHeight: '80vh',
        overflow: 'auto',
    },
    detailHeader: {
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginBottom: tokens.spacingHorizontalM,
    },
    detailSection: {
        marginBottom: tokens.spacingHorizontalM,
    },
    detailLabel: {
        fontWeight: tokens.fontWeightSemibold,
        fontSize: tokens.fontSizeBase12,
        color: tokens.colorNeutralForeground2,
    },
    detailValue: {
        fontSize: tokens.fontSizeBase14,
    },
    jsonBlock: {
        backgroundColor: tokens.colorNeutralBackground2,
        padding: tokens.spacingHorizontalM,
        borderRadius: tokens.borderRadiusMedium,
        fontFamily: 'monospace',
        fontSize: tokens.fontSizeBase13,
        overflow: 'auto',
        maxHeight: '200px',
    },
    pagination: {
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
    },
});

const ACTION_COLORS: Record<AuditAction, string> = {
    CREATE: 'success',
    UPDATE: 'warning',
    DELETE: 'danger',
    LOGIN: 'info',
    LOGOUT: 'subtle',
    EXPORT: 'subtle',
    APPROVE: 'success',
    REJECT: 'danger',
};

const ACTION_OPTIONS: { value: AuditAction | ''; label: string }[] = [
    { value: '', label: 'All Actions' },
    { value: 'CREATE', label: 'Create' },
    { value: 'UPDATE', label: 'Update' },
    { value: 'DELETE', label: 'Delete' },
    { value: 'LOGIN', label: 'Login' },
    { value: 'LOGOUT', label: 'Logout' },
    { value: 'EXPORT', label: 'Export' },
    { value: 'APPROVE', label: 'Approve' },
    { value: 'REJECT', label: 'Reject' },
];

const ENTITY_TYPE_OPTIONS: { value: AuditEntityType | ''; label: string }[] = [
    { value: '', label: 'All Types' },
    { value: 'USER', label: 'User' },
    { value: 'ORGANIZATION', label: 'Organization' },
    { value: 'FILE', label: 'File' },
    { value: 'REPORT', label: 'Report' },
    { value: 'FORM', label: 'Form' },
    { value: 'DASHBOARD', label: 'Dashboard' },
    { value: 'TEMPLATE', label: 'Template' },
];

const formatDate = (dateString: string): string => {
    return new Date(dateString).toLocaleString();
};

interface AuditLogViewerProps {
    entityId?: string;
    entityType?: AuditEntityType;
    onEntityClick?: (entityType: AuditEntityType, entityId: string) => void;
}

export default function AuditLogViewer({ entityId, entityType, onEntityClick }: AuditLogViewerProps) {
    const styles = useStyles();

    const [params, setParams] = useState<AuditLogParams>({
        page: 1,
        page_size: 20,
        ...(entityId && { entity_id: entityId }),
        ...(entityType && { entity_type: entityType }),
    });
    const [selectedLog, setSelectedLog] = useState<{ id: string; details: Record<string, unknown> } | null>(null);

    const { data, isLoading, refetch } = useAuditLogs(params);

    const handleFilterChange = (key: keyof AuditLogParams, value: string) => {
        const newParams = { ...params, [key]: value || undefined, page: 1 };
        setParams(newParams);
    };

    const handleExport = async (format: 'csv' | 'json') => {
        const blob = await exportAuditLogs(params, format);
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `audit-logs.${format}`;
        a.click();
        window.URL.revokeObjectURL(url);
    };

    const handlePageChange = (newPage: number) => {
        setParams({ ...params, page: newPage });
    };

    return (
        <div className={styles.container}>
            <div className={styles.filters}>
                <div className={styles.filterGroup}>
                    <span className={styles.filterLabel}>Action</span>
                    <Dropdown
                        value={ACTION_OPTIONS.find(o => o.value === (params.action || ''))?.label || 'All Actions'}
                        onOptionSelect={(_ev, data) => handleFilterChange('action', data.optionValue as string)}
                    >
                        {ACTION_OPTIONS.map((opt) => (
                            <Option key={opt.value} value={opt.value}>
                                {opt.label}
                            </Option>
                        ))}
                    </Dropdown>
                </div>

                <div className={styles.filterGroup}>
                    <span className={styles.filterLabel}>Entity Type</span>
                    <Dropdown
                        value={ENTITY_TYPE_OPTIONS.find(o => o.value === (params.entity_type || ''))?.label || 'All Types'}
                        onOptionSelect={(_ev, data) => handleFilterChange('entity_type', data.optionValue as string)}
                    >
                        {ENTITY_TYPE_OPTIONS.map((opt) => (
                            <Option key={opt.value} value={opt.value}>
                                {opt.label}
                            </Option>
                        ))}
                    </Dropdown>
                </div>

                <div className={styles.filterGroup}>
                    <span className={styles.filterLabel}>Entity ID</span>
                    <Input
                        placeholder="Filter by entity ID"
                        value={params.entity_id || ''}
                        onChange={(_ev, data) => handleFilterChange('entity_id', data.value)}
                    />
                </div>

                <div className={styles.filterGroup}>
                    <span className={styles.filterLabel}>Date From</span>
                    <Input
                        type="date"
                        value={params.start_date || ''}
                        onChange={(_ev, data) => handleFilterChange('start_date', data.value)}
                    />
                </div>

                <div className={styles.filterGroup}>
                    <span className={styles.filterLabel}>Date To</span>
                    <Input
                        type="date"
                        value={params.end_date || ''}
                        onChange={(_ev, data) => handleFilterChange('end_date', data.value)}
                    />
                </div>

                <div className={styles.actions}>
                    <Button
                        appearance="subtle"
                        icon={<ArrowDownload24Regular />}
                        onClick={() => handleExport('csv')}
                    >
                        CSV
                    </Button>
                    <Button
                        appearance="subtle"
                        icon={<ArrowDownload24Regular />}
                        onClick={() => handleExport('json')}
                    >
                        JSON
                    </Button>
                </div>
            </div>

            {isLoading ? (
                <LoadingSpinner label="Loading audit logs..." />
            ) : (
                <>
                    <div className={styles.tableWrapper}>
                        <Table className={styles.table}>
                            <TableHeader>
                                <TableRow>
                                    <TableHeaderCell>Timestamp</TableHeaderCell>
                                    <TableHeaderCell>Action</TableHeaderCell>
                                    <TableHeaderCell>Entity</TableHeaderCell>
                                    <TableHeaderCell>User</TableHeaderCell>
                                    <TableHeaderCell>Organization</TableHeaderCell>
                                    <TableHeaderCell>IP Address</TableHeaderCell>
                                    <TableHeaderCell></TableHeaderCell>
                                </TableRow>
                            </TableHeader>
                            <TableBody>
                                {data?.data.map((log) => (
                                    <TableRow key={log.id}>
                                        <TableCell>{formatDate(log.created_at)}</TableCell>
                                        <TableCell>
                                            <Badge appearance="filled" color={ACTION_COLORS[log.action] as any}>
                                                {log.action}
                                            </Badge>
                                        </TableCell>
                                        <TableCell>
                                            <span
                                                className={styles.entityLink}
                                                onClick={() => onEntityClick?.(log.entity_type, log.entity_id)}
                                            >
                                                {log.entity_type} ({log.entity_id.slice(0, 8)}...)
                                            </span>
                                        </TableCell>
                                        <TableCell>{log.user_name}</TableCell>
                                        <TableCell>{log.org_name}</TableCell>
                                        <TableCell>{log.ip_address}</TableCell>
                                        <TableCell>
                                            <Button
                                                appearance="subtle"
                                                icon={<Eye24Regular />}
                                                onClick={() => setSelectedLog({ id: log.id, details: log.details })}
                                            >
                                                Details
                                            </Button>
                                        </TableCell>
                                    </TableRow>
                                ))}
                            </TableBody>
                        </Table>
                    </div>

                    <div className={styles.pagination}>
                        <span>
                            Showing {(params.page! - 1) * (params.page_size || 20) + 1} -{' '}
                            {Math.min(params.page! * (params.page_size || 20), data?.pagination.total_items || 0)} of{' '}
                            {data?.pagination.total_items || 0}
                        </span>
                        <div>
                            <Button
                                appearance="subtle"
                                disabled={params.page === 1}
                                onClick={() => handlePageChange(params.page! - 1)}
                            >
                                Previous
                            </Button>
                            <Button
                                appearance="subtle"
                                disabled={params.page === data?.pagination.total_pages}
                                onClick={() => handlePageChange(params.page! + 1)}
                            >
                                Next
                            </Button>
                        </div>
                    </div>
                </>
            )}

            {selectedLog && (
                <div className={styles.detailModal} onClick={() => setSelectedLog(null)}>
                    <div className={styles.detailContent} onClick={(e) => e.stopPropagation()}>
                        <div className={styles.detailHeader}>
                            <h3>Audit Log Details</h3>
                            <Button appearance="subtle" onClick={() => setSelectedLog(null)}>
                                Close
                            </Button>
                        </div>
                        <div className={styles.detailSection}>
                            <div className={styles.detailLabel}>ID</div>
                            <div className={styles.detailValue}>{selectedLog.id}</div>
                        </div>
                        <div className={styles.detailSection}>
                            <div className={styles.detailLabel}>Details (JSON)</div>
                            <pre className={styles.jsonBlock}>
                                {JSON.stringify(selectedLog.details, null, 2)}
                            </pre>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
