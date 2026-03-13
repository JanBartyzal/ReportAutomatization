import React from 'react';
import {
    Title3,
    Body1,
    Body2,
    Spinner,
    makeStyles,
    tokens,
    DataGrid,
    DataGridHeader,
    DataGridRow,
    DataGridBody,
    DataGridCell,
    TableCellLayout,
    TableColumnDefinition,
    createTableColumn,
    Button,
    Card,
    Tooltip,
} from '@fluentui/react-components';
import {
    ArrowDownload24Regular,
    ArrowSync24Regular,
    DocumentPdf24Regular,
    Calendar24Regular,
} from '@fluentui/react-icons';
import { useGeneratedReports, useRegenerateReport } from '../hooks/useGeneration';
import { StatusBadge } from '../components/Generation/StatusBadge';
import { reportBrand } from '../theme/brandTokens';

const useStyles = makeStyles({
    pageHeader: {
        marginBottom: '24px',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
    },
    subtitle: {
        color: tokens.colorNeutralForeground3,
        marginTop: '4px',
    },
    spinnerContainer: {
        padding: '40px',
        textAlign: 'center',
    },
    emptyContainer: {
        padding: '40px',
        textAlign: 'center',
    },
    emptyText: {
        color: tokens.colorNeutralForeground3,
    },
    emptyIcon: {
        color: tokens.colorNeutralForeground4,
        marginBottom: '16px',
    },
    actionsRow: {
        display: 'flex',
        gap: '4px',
    },
    mutedText: {
        color: tokens.colorNeutralForeground3,
    },
});

interface GeneratedReportItem {
    id: string;
    reportId: string;
    templateId: string;
    templateName: string;
    generatedAt: string;
    downloadUrl: string;
    status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
    fileSize?: number;
}

export const GeneratedReportsListPage: React.FC = () => {
    const styles = useStyles();
    const { data: reports, isLoading, refetch } = useGeneratedReports();
    const regenerateMutation = useRegenerateReport();

    const columns: TableColumnDefinition<GeneratedReportItem>[] = [
        createTableColumn<GeneratedReportItem>({
            columnId: 'templateName',
            compare: (a, b) => a.templateName.localeCompare(b.templateName),
            renderHeaderCell: () => 'Template',
            renderCell: (item: GeneratedReportItem) => (
                <TableCellLayout>
                    <DocumentPdf24Regular style={{ marginRight: '8px', color: reportBrand[90] }} />
                    <Body1>{item.templateName}</Body1>
                </TableCellLayout>
            ),
        }),
        createTableColumn<GeneratedReportItem>({
            columnId: 'generatedAt',
            compare: (a, b) => new Date(b.generatedAt).getTime() - new Date(a.generatedAt).getTime(),
            renderHeaderCell: () => 'Generated At',
            renderCell: (item: GeneratedReportItem) => (
                <TableCellLayout>
                    <Calendar24Regular style={{ fontSize: '14px', marginRight: '4px', color: tokens.colorNeutralForeground3 }} />
                    <Body2>
                        {new Date(item.generatedAt).toLocaleString()}
                    </Body2>
                </TableCellLayout>
            ),
        }),
        createTableColumn<GeneratedReportItem>({
            columnId: 'status',
            renderHeaderCell: () => 'Status',
            renderCell: (item: GeneratedReportItem) => (
                <TableCellLayout>
                    <StatusBadge status={item.status} size="small" />
                </TableCellLayout>
            ),
        }),
        createTableColumn<GeneratedReportItem>({
            columnId: 'fileSize',
            renderHeaderCell: () => 'Size',
            renderCell: (item: GeneratedReportItem) => (
                <TableCellLayout>
                    {item.fileSize ? (
                        <Body2>{(item.fileSize / 1024).toFixed(1)} KB</Body2>
                    ) : (
                        <Body2 className={styles.mutedText}>-</Body2>
                    )}
                </TableCellLayout>
            ),
        }),
        createTableColumn<GeneratedReportItem>({
            columnId: 'actions',
            renderHeaderCell: () => 'Actions',
            renderCell: (item: GeneratedReportItem) => (
                <TableCellLayout>
                    <div className={styles.actionsRow}>
                        {item.status === 'COMPLETED' && (
                            <Tooltip content="Download PPTX" relationship="label">
                                <Button
                                    appearance="subtle"
                                    size="small"
                                    icon={<ArrowDownload24Regular />}
                                    onClick={() => window.open(item.downloadUrl, '_blank')}
                                />
                            </Tooltip>
                        )}
                        <Tooltip content="Regenerate with current data" relationship="label">
                            <Button
                                appearance="subtle"
                                size="small"
                                icon={<ArrowSync24Regular />}
                                onClick={() => regenerateMutation.mutate(item.reportId)}
                                disabled={regenerateMutation.isPending}
                            />
                        </Tooltip>
                    </div>
                </TableCellLayout>
            ),
        }),
    ];

    return (
        <div style={{ padding: '24px', maxWidth: '1200px', margin: '0 auto' }}>
            <div className={styles.pageHeader}>
                <div>
                    <Title3>Generated Reports</Title3>
                    <Body2 className={styles.subtitle}>
                        View and manage all generated PowerPoint reports.
                    </Body2>
                </div>
                <Button
                    appearance="secondary"
                    icon={<ArrowSync24Regular />}
                    onClick={() => refetch()}
                >
                    Refresh
                </Button>
            </div>

            <Card>
                {isLoading ? (
                    <div className={styles.spinnerContainer}>
                        <Spinner label="Loading generated reports..." />
                    </div>
                ) : reports && reports.length > 0 ? (
                    <DataGrid
                        items={reports}
                        columns={columns}
                        sortable
                        resizableColumns
                        style={{ minWidth: '100%' }}
                    >
                        <DataGridHeader>
                            <DataGridRow>
                                {({ renderHeaderCell }) => (
                                    <DataGridCell>{renderHeaderCell()}</DataGridCell>
                                )}
                            </DataGridRow>
                        </DataGridHeader>
                        <DataGridBody<GeneratedReportItem>>
                            {({ item, rowId }) => (
                                <DataGridRow<GeneratedReportItem> key={rowId}>
                                    {({ renderCell }) => (
                                        <DataGridCell>{renderCell(item)}</DataGridCell>
                                    )}
                                </DataGridRow>
                            )}
                        </DataGridBody>
                    </DataGrid>
                ) : (
                    <div className={styles.emptyContainer}>
                        <DocumentPdf24Regular style={{ fontSize: '48px' }} className={styles.emptyIcon} />
                        <Body1 className={styles.emptyText}>
                            No generated reports found. Generate a report from the report detail page.
                        </Body1>
                    </div>
                )}
            </Card>
        </div>
    );
};

export default GeneratedReportsListPage;
