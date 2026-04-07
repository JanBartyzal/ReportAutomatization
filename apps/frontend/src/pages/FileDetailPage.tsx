import { useParams } from 'react-router-dom';
import { useState } from 'react';
import {
    Title2,
    Title3,
    Body1,
    makeStyles,
    Button,
    tokens,
    Table,
    TableHeader,
    TableRow,
    TableHeaderCell,
    TableBody,
    TableCell,
    Badge,
    Spinner,
} from '@fluentui/react-components';
import { ArrowSyncRegular, ChevronLeft24Regular, ChevronRight24Regular, HistoryRegular } from '@fluentui/react-icons';
import { useFile, useReprocessFile, useFileContent } from '../hooks/useFiles';
import LoadingSpinner from '../components/LoadingSpinner';
import { StatusBadge } from '../components/shared/StatusBadge';
import { useNavigate } from 'react-router-dom';

/**
 * FileDetailPage styles per docs/UX-UI/02-design-system.md
 * - Sidebar per organisms spec in 03-figma-components.md
 * - DataGrid per section 10.1
 * - No hardcoded colors - use Fluent tokens
 */
const useStyles = makeStyles({
    container: {
        padding: tokens.spacingHorizontalL,
    },
    header: {
        display: 'flex',
        alignItems: 'center',
        gap: tokens.spacingHorizontalM,
        marginBottom: tokens.spacingHorizontalL,
    },
    content: {
        display: 'grid',
        gridTemplateColumns: '1fr 300px',
        gap: tokens.spacingHorizontalL,
    },
    /**
     * Slide viewer - solid background per design system
     * No glassmorphism for data content per 02-design-system.md section 1.2
     */
    slideViewer: {
        backgroundColor: tokens.colorNeutralBackground2,
        borderRadius: tokens.borderRadiusMedium,
        minHeight: '400px',
        padding: tokens.spacingHorizontalM,
        overflowX: 'auto',
    },
    sidebar: {
        backgroundColor: tokens.colorNeutralBackground2,
        padding: tokens.spacingHorizontalM,
        borderRadius: tokens.borderRadiusMedium,
    },
    metadataItem: {
        marginBottom: tokens.spacingVerticalM,
    },
    metadataLabel: {
        color: tokens.colorNeutralForeground2,
        fontSize: tokens.fontSizeBase300,
    },
    metadataValue: {
        color: tokens.colorNeutralForeground1,
        fontSize: tokens.fontSizeBase200,
    },
    navigation: {
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        gap: tokens.spacingHorizontalM,
        marginTop: tokens.spacingVerticalM,
    },
    tableSection: {
        marginTop: tokens.spacingHorizontalL,
    },
    /**
     * DataGrid styles per section 10.1
     */
    tableHeader: {
        backgroundColor: tokens.colorNeutralBackground2,
    },
    headerCell: {
        fontWeight: '600',
        textTransform: 'none',
    },
    row: {
        minHeight: '40px',
    },
    rowEven: {
        backgroundColor: tokens.colorNeutralBackground1,
    },
    rowOdd: {
        backgroundColor: tokens.colorNeutralBackground2,
    },
    cell: {
        padding: `${tokens.spacingVerticalXS} ${tokens.spacingHorizontalS}`,
    },
    emptyState: {
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        minHeight: '200px',
        color: tokens.colorNeutralForeground2,
    },
});

export default function FileDetailPage() {
    const styles = useStyles();
    const navigate = useNavigate();
    const { fileId } = useParams<{ fileId: string }>();
    const { data: file, isLoading } = useFile(fileId || '', { pollingInterval: 5000 });
    const { data: content, isLoading: isContentLoading } = useFileContent(fileId || '', file?.mime_type);
    const reprocess = useReprocessFile();
    const [currentIndex, setCurrentIndex] = useState(1);

    if (isLoading) {
        return <LoadingSpinner label="Loading file details..." />;
    }

    if (!file) {
        return <Body1>File not found</Body1>;
    }

    const isExcel = file.mime_type?.includes('spreadsheetml') || file.mime_type?.includes('csv');
    const isPptx = file.mime_type?.includes('presentationml');
    const isPdf = file.mime_type?.includes('pdf');

    // Determine total pages/sheets/slides from actual content metadata
    const total = content?.metadata?.total_sheets
        ?? content?.metadata?.total_pages
        ?? (content?.slides?.length)
        ?? 0;

    const pageLabel = isExcel ? 'Sheet' : isPptx ? 'Slide' : isPdf ? 'Page' : 'Section';

    // Current sheet/slide/page data
    const currentSheet = isExcel && content?.sheets ? content.sheets[currentIndex - 1] : null;
    const currentSlide = isPptx && content?.slides ? content.slides[currentIndex - 1] : null;

    return (
        <div className={styles.container}>
            <div className={styles.header}>
                <Button
                    appearance="subtle"
                    icon={<ChevronLeft24Regular />}
                    onClick={() => window.history.back()}
                >
                    Back
                </Button>
                <Title2>{file.filename}</Title2>
                <div style={{ marginLeft: 'auto', display: 'flex', gap: tokens.spacingHorizontalS }}>
                    <Button
                        appearance="primary"
                        icon={<ArrowSyncRegular />}
                        disabled={reprocess.isPending}
                        onClick={() => fileId && reprocess.mutate(fileId)}
                    >
                        {reprocess.isPending ? 'Triggering...' : 'Reprocess'}
                    </Button>
                    <Button
                        appearance="subtle"
                        icon={<HistoryRegular />}
                        onClick={() => navigate(`/admin/audit?entity_type=FILE&entity_id=${fileId}`)}
                    >
                        View audit trail
                    </Button>
                </div>
            </div>

            {reprocess.isSuccess && (
                <Badge appearance="filled" color="success" style={{ marginBottom: tokens.spacingVerticalS }}>
                    Reprocessing triggered — status will update automatically
                </Badge>
            )}
            {reprocess.isError && (
                <Badge appearance="filled" color="danger" style={{ marginBottom: tokens.spacingVerticalS }}>
                    Failed to trigger reprocessing: {(reprocess.error as Error).message}
                </Badge>
            )}

            <div className={styles.content}>
                <div>
                    <div className={styles.slideViewer}>
                        {isContentLoading ? (
                            <div className={styles.emptyState}>
                                <Spinner label="Loading content..." />
                            </div>
                        ) : currentSheet ? (
                            <div>
                                <Title3 style={{ marginBottom: tokens.spacingVerticalS }}>
                                    {currentSheet.sheet_name}
                                </Title3>
                                {currentSheet.headers.length > 0 ? (
                                    <Table>
                                        <TableHeader className={styles.tableHeader}>
                                            <TableRow>
                                                {currentSheet.headers.map((h, i) => (
                                                    <TableHeaderCell key={i} className={styles.headerCell}>{h}</TableHeaderCell>
                                                ))}
                                            </TableRow>
                                        </TableHeader>
                                        <TableBody>
                                            {currentSheet.rows.slice(0, 100).map((row, ri) => (
                                                <TableRow key={ri} className={`${styles.row} ${ri % 2 === 0 ? styles.rowEven : styles.rowOdd}`}>
                                                    {currentSheet.headers.map((h, ci) => (
                                                        <TableCell key={ci} className={styles.cell}>
                                                            {String(row[h] ?? '')}
                                                        </TableCell>
                                                    ))}
                                                </TableRow>
                                            ))}
                                            {currentSheet.row_count > 100 && (
                                                <TableRow>
                                                    <TableCell colSpan={currentSheet.headers.length} className={styles.cell}>
                                                        <Body1 italic>
                                                            Showing 100 of {currentSheet.row_count} rows
                                                        </Body1>
                                                    </TableCell>
                                                </TableRow>
                                            )}
                                        </TableBody>
                                    </Table>
                                ) : (
                                    <div className={styles.emptyState}>
                                        <Body1 italic>No data in this sheet.</Body1>
                                    </div>
                                )}
                            </div>
                        ) : currentSlide ? (
                            <div>
                                <Title3 style={{ marginBottom: tokens.spacingVerticalS }}>
                                    Slide {currentSlide.slide_number}
                                </Title3>
                                {currentSlide.text && (
                                    <Body1 style={{ whiteSpace: 'pre-wrap', display: 'block', marginBottom: tokens.spacingVerticalM }}>
                                        {currentSlide.text}
                                    </Body1>
                                )}
                                {currentSlide.tables.length > 0 && currentSlide.tables.map((tbl, ti) => (
                                    <div key={ti} style={{ marginTop: tokens.spacingVerticalM }}>
                                        <Table>
                                            <TableHeader className={styles.tableHeader}>
                                                <TableRow>
                                                    {tbl.headers.map((h, i) => (
                                                        <TableHeaderCell key={i} className={styles.headerCell}>{h}</TableHeaderCell>
                                                    ))}
                                                </TableRow>
                                            </TableHeader>
                                            <TableBody>
                                                {tbl.rows.map((row, ri) => (
                                                    <TableRow key={ri} className={`${styles.row} ${ri % 2 === 0 ? styles.rowEven : styles.rowOdd}`}>
                                                        {tbl.headers.map((h, ci) => (
                                                            <TableCell key={ci} className={styles.cell}>
                                                                {String(row[h] ?? '')}
                                                            </TableCell>
                                                        ))}
                                                    </TableRow>
                                                ))}
                                            </TableBody>
                                        </Table>
                                    </div>
                                ))}
                                {currentSlide.speaker_notes && (
                                    <div style={{ marginTop: tokens.spacingVerticalL }}>
                                        <Title3>Speaker Notes</Title3>
                                        <div style={{
                                            padding: tokens.spacingHorizontalM,
                                            backgroundColor: tokens.colorNeutralBackground3,
                                            borderRadius: tokens.borderRadiusMedium,
                                            marginTop: tokens.spacingVerticalS
                                        }}>
                                            <Body1>{currentSlide.speaker_notes}</Body1>
                                        </div>
                                    </div>
                                )}
                            </div>
                        ) : (
                            <div className={styles.emptyState}>
                                <Body1 italic>
                                    {content
                                        ? 'No content available for this file.'
                                        : 'Content not yet processed. Try reprocessing the file.'}
                                </Body1>
                            </div>
                        )}
                    </div>

                    {total > 1 && (
                        <div className={styles.navigation}>
                            <Button
                                icon={<ChevronLeft24Regular />}
                                disabled={currentIndex <= 1}
                                onClick={() => setCurrentIndex(s => s - 1)}
                            >
                                Previous
                            </Button>
                            <Body1>{pageLabel} {currentIndex} of {total}</Body1>
                            <Button
                                icon={<ChevronRight24Regular />}
                                disabled={currentIndex >= total}
                                onClick={() => setCurrentIndex(s => s + 1)}
                            >
                                Next
                            </Button>
                        </div>
                    )}
                    {total === 1 && (
                        <div className={styles.navigation}>
                            <Body1>{pageLabel} 1 of 1</Body1>
                        </div>
                    )}
                </div>

                <aside className={styles.sidebar}>
                    <Title3>File Details</Title3>
                    <div className={styles.metadataItem}>
                        <Body1 className={styles.metadataLabel}><strong>Status:</strong></Body1>
                        <div style={{ marginTop: tokens.spacingVerticalXS }}>
                            <StatusBadge status={file.status} />
                        </div>
                    </div>
                    <div className={styles.metadataItem}>
                        <Body1 className={styles.metadataLabel}><strong>Size:</strong></Body1>
                        <Body1 className={styles.metadataValue}>{(file.size_bytes / 1024).toFixed(1)} KB</Body1>
                    </div>
                    <div className={styles.metadataItem}>
                        <Body1 className={styles.metadataLabel}><strong>Uploaded:</strong></Body1>
                        <Body1 className={styles.metadataValue}>
                            {file.uploaded_at ? new Date(file.uploaded_at).toLocaleString() : 'N/A'}
                        </Body1>
                    </div>
                    <div className={styles.metadataItem}>
                        <Body1 className={styles.metadataLabel}><strong>File Type:</strong></Body1>
                        <Body1 className={styles.metadataValue}>{file.mime_type}</Body1>
                    </div>
                    {total > 0 && (
                        <div className={styles.metadataItem}>
                            <Body1 className={styles.metadataLabel}><strong>{pageLabel}s:</strong></Body1>
                            <Body1 className={styles.metadataValue}>{total}</Body1>
                        </div>
                    )}
                    {content?.metadata?.total_rows != null && (
                        <div className={styles.metadataItem}>
                            <Body1 className={styles.metadataLabel}><strong>Total rows:</strong></Body1>
                            <Body1 className={styles.metadataValue}>{content.metadata.total_rows}</Body1>
                        </div>
                    )}

                    <Title3 style={{ marginTop: tokens.spacingHorizontalL }}>Processing Log</Title3>
                    {file.workflow_status?.steps ? (
                        <div style={{ display: 'flex', flexDirection: 'column', gap: tokens.spacingVerticalS, marginTop: tokens.spacingVerticalS }}>
                            {file.workflow_status.steps.map((step, idx) => (
                                <div key={idx} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                    <Body1>{step.step_name}</Body1>
                                    <Badge appearance="outline" color={step.status === 'COMPLETED' ? 'success' : 'warning'}>
                                        {step.status}
                                    </Badge>
                                </div>
                            ))}
                        </div>
                    ) : (
                        <Body1 style={{ marginTop: tokens.spacingVerticalS }}>No processing history available.</Body1>
                    )}
                </aside>
            </div>
        </div>
    );
}
