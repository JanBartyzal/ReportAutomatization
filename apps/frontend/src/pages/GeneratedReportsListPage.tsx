import React from 'react';
import {
    Page,
    Title3,
    Title4,
    Body1,
    Body2,
    Spinner,
} from '@fluentui/react-components';
import {
    DataGrid,
    DataGridHeader,
    DataGridRow,
    DataGridHeaderCell,
    DataGridBody,
    DataGridCell,
    TableCellLayout,
    TableColumnDefinition,
    createTableColumn,
} from '@fluentui/react-components/unstable';
import {
    Button,
    Card,
    CardHeader,
    Divider,
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
    const { data: reports, isLoading, refetch } = useGeneratedReports();
    const regenerateMutation = useRegenerateReport();

    const columns: TableColumnDefinition<GeneratedReportItem>[] = [
        createTableColumn<GeneratedReportItem>({
            columnId: 'templateName',
            compare: (a, b) => a.templateName.localeCompare(b.templateName),
            renderHeaderCell: () => 'Template',
            renderCell: (item) => (
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
            renderCell: (item) => (
                <TableCellLayout>
                    <Calendar24Regular style={{ fontSize: '14px', marginRight: '4px', color: '#666' }} />
                    <Body2>
                        {new Date(item.generatedAt).toLocaleString()}
                    </Body2>
                </TableCellLayout>
            ),
        }),
        createTableColumn<GeneratedReportItem>({
            columnId: 'status',
            width: 120,
            renderHeaderCell: () => 'Status',
            renderCell: (item) => (
                <TableCellLayout>
                    <StatusBadge status={item.status} size="small" />
                </TableCellLayout>
            ),
        }),
        createTableColumn<GeneratedReportItem>({
            columnId: 'fileSize',
            width: 100,
            renderHeaderCell: () => 'Size',
            renderCell: (item) => (
                <TableCellLayout>
                    {item.fileSize ? (
                        <Body2>{(item.fileSize / 1024).toFixed(1)} KB</Body2>
                    ) : (
                        <Body2 style={{ color: '#666' }}>-</Body2>
                    )}
                </TableCellLayout>
            ),
        }),
        createTableColumn<GeneratedReportItem>({
            columnId: 'actions',
            width: 140,
            renderHeaderCell: () => 'Actions',
            renderCell: (item) => (
                <TableCellLayout>
                    <div style={{ display: 'flex', gap: '4px' }}>
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
        <Page style={{ padding: '24px', maxWidth: '1200px', margin: '0 auto' }}>
            <div style={{ marginBottom: '24px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                    <Title3>Generated Reports</Title3>
                    <Body2 style={{ color: '#666', marginTop: '4px' }}>
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
                    <div style={{ padding: '40px', textAlign: 'center' }}>
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
                                    <DataGridHeaderCell>{renderHeaderCell()}</DataGridHeaderCell>
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
                    <div style={{ padding: '40px', textAlign: 'center' }}>
                        <DocumentPdf24Regular style={{ fontSize: '48px', color: '#ccc', marginBottom: '16px' }} />
                        <Body1 style={{ color: '#666' }}>
                            No generated reports found. Generate a report from the report detail page.
                        </Body1>
                    </div>
                )}
            </Card>
        </Page>
    );
};

export default GeneratedReportsListPage;
