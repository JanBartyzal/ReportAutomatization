import React, { useState, useEffect } from 'react';
import {
    Title2,
    Title3,
    Body1,
    Body2,
    Caption1,
    Spinner,
    makeStyles,
    tokens,
    DataGrid,
    DataGridHeader,
    DataGridRow,
    DataGridHeaderCell,
    DataGridBody,
    DataGridCell,
    TableCellLayout,
    TableColumnDefinition,
    createTableColumn,
} from '@fluentui/react-components';
import {
    Button,
    Card,
    CardHeader,
    Divider,
    Dropdown,
    Option,
    Tooltip,
} from '@fluentui/react-components';
import {
    DocumentPdf24Regular,
    ArrowDownload24Regular,
    CheckmarkCircle24Regular,
    Calendar24Regular,
} from '@fluentui/react-icons';
// ...
import { useTemplates } from '../hooks/useTemplates';
import {
    useGenerateBatch,
    useBatchGenerationPolling,
    useApprovedReports
} from '../hooks/useGeneration';
import { GenerationProgress } from '../components/Generation/GenerationProgress';
import { ReportStatusBadge } from '../components/Generation/StatusBadge';
import { reportBrand } from '../theme/brandTokens';

const useStyles = makeStyles({
    header: {
        marginBottom: '24px',
    },
    headerSubtitle: {
        color: tokens.colorNeutralForeground3,
        marginTop: '4px',
    },
    periodCard: {
        marginBottom: '24px',
    },
    periodBody: {
        padding: '16px',
    },
    progressCard: {
        marginBottom: '24px',
        borderLeft: `4px solid ${reportBrand[90]}`,
    },
    progressBody: {
        padding: '16px',
    },
    completedCard: {
        marginBottom: '24px',
        borderLeft: `4px solid ${tokens.colorStatusSuccessBorderActive}`,
    },
    completedHeader: {
        display: 'flex',
        alignItems: 'center',
        gap: '8px',
    },
    completedIcon: {
        color: tokens.colorStatusSuccessForeground1,
    },
    completedActions: {
        marginTop: '16px',
        display: 'flex',
        gap: '8px',
    },
    tableHeader: {
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
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
    mutedCaption: {
        color: tokens.colorNeutralForeground3,
    },
});

interface Report {
    id: string;
    name: string;
    status: 'DRAFT' | 'SUBMITTED' | 'APPROVED' | 'REJECTED';
    period: string;
    generatedAt?: string;
    downloadUrl?: string;
}

interface BatchJob {
    jobId: string;
    total: number;
    completed: number;
    failed: number;
    status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
}

export const BatchGenerationPage: React.FC = () => {
    const styles = useStyles();
    const [selectedPeriod, setSelectedPeriod] = useState<string>('');
    const [batchJob, setBatchJob] = useState<BatchJob | null>(null);
    const [selectedReports, setSelectedReports] = useState<string[]>([]);

    const { data: templates } = useTemplates();
    const { data: approvedReports, isLoading: reportsLoading } = useApprovedReports(selectedPeriod);
    const generateBatchMutation = useGenerateBatch();

    // Poll for batch job status
    const pollingResult = useBatchGenerationPolling(
        batchJob?.jobId || '',
        !!batchJob
    ) as any;
    const { current, total, status, downloadUrls } = pollingResult;

    // Update batch job status from polling
    useEffect(() => {
        if (batchJob && current !== undefined && total !== undefined) {
            setBatchJob(prev => prev ? {
                ...prev,
                current,
                total,
                status,
            } : null);
        }
    }, [current, total, status]);

    // Update download URLs from polling
    useEffect(() => {
        if (downloadUrls && downloadUrls.length > 0) {
            // Check if all are complete
            const allComplete = Object.values(downloadUrls).every(url => url !== null);
            if (allComplete) {
                // Could create a ZIP download URL here
            }
        }
    }, [downloadUrls]);

    const columns: TableColumnDefinition<Report>[] = [
        createTableColumn<Report>({
            columnId: 'select',
            renderCell: (item) => (
                <input
                    type="checkbox"
                    checked={selectedReports.includes(item.id)}
                    onChange={(e) => {
                        if (e.target.checked) {
                            setSelectedReports([...selectedReports, item.id]);
                        } else {
                            setSelectedReports(selectedReports.filter(id => id !== item.id));
                        }
                    }}
                />
            ),
            renderHeaderCell: () => (
                <input
                    type="checkbox"
                    checked={selectedReports.length === (approvedReports?.length || 0)}
                    onChange={(e) => {
                        if (e.target.checked && approvedReports) {
                            setSelectedReports(approvedReports.map(r => r.id));
                        } else {
                            setSelectedReports([]);
                        }
                    }}
                />
            ),
        }),
        createTableColumn<Report>({
            columnId: 'name',
            compare: (a, b) => a.name.localeCompare(b.name),
            renderHeaderCell: () => 'Report Name',
            renderCell: (item) => (
                <TableCellLayout>
                    <Body1>{item.name}</Body1>
                </TableCellLayout>
            ),
        }),
        createTableColumn<Report>({
            columnId: 'period',
            renderHeaderCell: () => 'Period',
            renderCell: (item) => (
                <TableCellLayout>
                    <Caption1><Calendar24Regular style={{ fontSize: '14px', marginRight: '4px' }} />{item.period}</Caption1>
                </TableCellLayout>
            ),
        }),
        createTableColumn<Report>({
            columnId: 'status',
            renderHeaderCell: () => 'Status',
            renderCell: (item) => (
                <TableCellLayout>
                    <ReportStatusBadge status={item.status} size="small" />
                </TableCellLayout>
            ),
        }),
        createTableColumn<Report>({
            columnId: 'generated',
            renderHeaderCell: () => 'Generated',
            renderCell: (item) => (
                <TableCellLayout>
                    {item.generatedAt ? (
                        <Caption1>{new Date(item.generatedAt).toLocaleDateString()}</Caption1>
                    ) : (
                        <Caption1 className={styles.mutedCaption}>Not generated</Caption1>
                    )}
                </TableCellLayout>
            ),
        }),
        createTableColumn<Report>({
            columnId: 'actions',
            renderHeaderCell: () => 'Actions',
            renderCell: (item) => (
                <TableCellLayout>
                    {item.downloadUrl ? (
                        <Tooltip content="Download PPTX" relationship="label">
                            <Button
                                appearance="subtle"
                                icon={<ArrowDownload24Regular />}
                                onClick={() => window.open(item.downloadUrl, '_blank')}
                            />
                        </Tooltip>
                    ) : item.status === 'APPROVED' ? (
                        <Tooltip content="Generate PPTX" relationship="label">
                            <Button
                                appearance="subtle"
                                icon={<DocumentPdf24Regular />}
                                onClick={() => {
                                    setSelectedReports([item.id]);
                                    handleGenerateBatch();
                                }}
                            />
                        </Tooltip>
                    ) : null}
                </TableCellLayout>
            ),
        }),
    ];

    const handleGenerateBatch = async () => {
        if (selectedReports.length === 0) return;

        try {
            const response = await generateBatchMutation.mutateAsync({
                reportIds: selectedReports,
                templateId: templates?.[0]?.id, 
            });

            // The API returns results with individual jobIds. 
            // For batch polling, we use the first one or a combined ID if available.
            const jobId = (response as any).jobId || (response.results?.[0]?.jobId);
            
            if (jobId) {
                setBatchJob({
                    jobId: jobId,
                    total: selectedReports.length,
                    completed: 0,
                    failed: 0,
                    status: 'PROCESSING',
                });
            }
        } catch (error) {
            console.error('Failed to start batch generation:', error);
        }
    };

    const handleDownloadAll = () => {
        // For now, download each individually
        // In a real implementation, this would trigger a ZIP download
        if (downloadUrls) {
            Object.entries(downloadUrls).forEach(([, url]) => {
                if (url) window.open(url as string, '_blank');
            });
        }
    };

    return (
        <div style={{ padding: '24px', maxWidth: '1200px', margin: '0 auto' }}>
            <div className={styles.header}>
                <Title3>Batch Report Generation</Title3>
                <Body2 className={styles.headerSubtitle}>
                    Generate PowerPoint reports for multiple approved reports at once.
                </Body2>
            </div>

            {/* Period Selection */}
            <Card className={styles.periodCard}>
                <CardHeader
                    header={<Title2>Select Period</Title2>}
                />
                <div className={styles.periodBody}>
                    <Dropdown
                        placeholder="Select a period"
                        style={{ minWidth: '300px' }}
                        onOptionSelect={(_, data) => setSelectedPeriod(data.optionValue as string)}
                        value={selectedPeriod}
                    >
                        <Option value="Q1-2026">Q1 2026</Option>
                        <Option value="Q2-2026">Q2 2026</Option>
                        <Option value="Q3-2026">Q3 2026</Option>
                        <Option value="Q4-2026">Q4 2026</Option>
                        <Option value="2025">Full Year 2025</Option>
                    </Dropdown>

                    {selectedPeriod && (
                        <Body1 style={{ marginTop: '12px' }}>
                            {approvedReports?.length || 0} approved reports found for {selectedPeriod}
                        </Body1>
                    )}
                </div>
            </Card>

            {/* Progress Card (when generating) */}
            {batchJob && batchJob.status !== 'COMPLETED' && batchJob.status !== 'FAILED' && (
                <Card className={styles.progressCard}>
                    <CardHeader
                        header={<Title2>Generating Reports</Title2>}
                    />
                    <div className={styles.progressBody}>
                        <GenerationProgress
                            status={batchJob.status}
                            current={current}
                            total={total}
                        />
                    </div>
                </Card>
            )}

            {/* Completed Card */}
            {batchJob && batchJob.status === 'COMPLETED' && (
                <Card className={styles.completedCard}>
                    <CardHeader
                        header={
                            <div className={styles.completedHeader}>
                                <CheckmarkCircle24Regular className={styles.completedIcon} />
                                <Title2>Generation Complete</Title2>
                            </div>
                        }
                    />
                    <div className={styles.periodBody}>
                        <Body1>Successfully generated {total} reports.</Body1>
                        <div className={styles.completedActions}>
                            <Button
                                appearance="primary"
                                icon={<ArrowDownload24Regular />}
                                onClick={handleDownloadAll}
                            >
                                Download All
                            </Button>
                            <Button
                                appearance="secondary"
                                onClick={() => setBatchJob(null)}
                            >
                                Close
                            </Button>
                        </div>
                    </div>
                </Card>
            )}

            {/* Reports Table */}
            {selectedPeriod && (
                <Card>
                    <CardHeader
                        header={
                            <div className={styles.tableHeader}>
                                <Title2>Approved Reports ({approvedReports?.length || 0})</Title2>
                                <Button
                                    appearance="primary"
                                    icon={<DocumentPdf24Regular />}
                                    disabled={selectedReports.length === 0 || !!batchJob}
                                    onClick={handleGenerateBatch}
                                >
                                    Generate Selected ({selectedReports.length})
                                </Button>
                            </div>
                        }
                    />
                    <Divider />
                    {reportsLoading ? (
                        <div className={styles.spinnerContainer}>
                            <Spinner label="Loading reports..." />
                        </div>
                    ) : approvedReports && approvedReports.length > 0 ? (
                        <DataGrid
                            items={approvedReports}
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
                            <DataGridBody<Report>>
                                {({ item, rowId }) => (
                                    <DataGridRow<Report> key={rowId}>
                                        {({ renderCell }) => (
                                            <DataGridCell>{renderCell(item)}</DataGridCell>
                                        )}
                                    </DataGridRow>
                                )}
                            </DataGridBody>
                        </DataGrid>
                    ) : (
                        <div className={styles.emptyContainer}>
                            <Body1 className={styles.emptyText}>
                                No approved reports found for the selected period.
                            </Body1>
                        </div>
                    )}
                </Card>
            )}
        </div>
    );
};

export default BatchGenerationPage;
