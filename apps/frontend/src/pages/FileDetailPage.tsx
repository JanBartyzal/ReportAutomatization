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
} from '@fluentui/react-components';
import { ChevronLeft24Regular, ChevronRight24Regular, HistoryRegular } from '@fluentui/react-icons';
import { useFile } from '../hooks/useFiles';
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
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        color: tokens.colorNeutralForeground2,
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
});

export default function FileDetailPage() {
    const styles = useStyles();
    const navigate = useNavigate();
    const { fileId } = useParams<{ fileId: string }>();
    const { data: file, isLoading } = useFile(fileId || '');
    const [currentSlide, setCurrentSlide] = useState(1);

    if (isLoading) {
        return <LoadingSpinner label="Loading file details..." />;
    }

    if (!file) {
        return <Body1>File not found</Body1>;
    }

    const totalSlides = 10; // Mocked for now, should come from file.metadata or slide API

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
                        appearance="subtle"
                        icon={<HistoryRegular />}
                        onClick={() => navigate(`/admin/audit?entity_type=FILE&entity_id=${fileId}`)}
                    >
                        View audit trail
                    </Button>
                </div>
            </div>

            <div className={styles.content}>
                <div>
                    <div className={styles.slideViewer}>
                        {/* Slide image would go here: <img src={`${file.blob_url}/slide_${currentSlide}.png`} /> */}
                        <div style={{ textAlign: 'center' }}>
                            <Body1 style={{ display: 'block', marginBottom: tokens.spacingVerticalM }}>
                                <strong>Slide {currentSlide} Preview</strong>
                            </Body1>
                            <Body1 italic>Slide content and text extraction would be displayed here.</Body1>
                        </div>
                    </div>

                    <div className={styles.navigation}>
                        <Button 
                            icon={<ChevronLeft24Regular />} 
                            disabled={currentSlide <= 1}
                            onClick={() => setCurrentSlide(s => s - 1)}
                        >
                            Previous
                        </Button>
                        <Body1>Slide {currentSlide} of {totalSlides}</Body1>
                        <Button 
                            icon={<ChevronRight24Regular />}
                            disabled={currentSlide >= totalSlides}
                            onClick={() => setCurrentSlide(s => s + 1)}
                        >
                            Next
                        </Button>
                    </div>

                    <div className={styles.tableSection}>
                        <Title3>Speaker Notes</Title3>
                        <div style={{ 
                            padding: tokens.spacingHorizontalM, 
                            backgroundColor: tokens.colorNeutralBackground3,
                            borderRadius: tokens.borderRadiusMedium,
                            marginTop: tokens.spacingVerticalS
                        }}>
                            <Body1>These are the speaker notes for slide {currentSlide}. They provide additional context for the presenter.</Body1>
                        </div>
                    </div>

                    <div className={styles.tableSection}>
                        <Title3>Extracted Tables</Title3>
                        <Table>
                            <TableHeader className={styles.tableHeader}>
                                <TableRow>
                                    <TableHeaderCell className={styles.headerCell}>Sheet/Table</TableHeaderCell>
                                    <TableHeaderCell className={styles.headerCell}>Data Point</TableHeaderCell>
                                    <TableHeaderCell className={styles.headerCell}>Value</TableHeaderCell>
                                </TableRow>
                            </TableHeader>
                            <TableBody>
                                <TableRow className={`${styles.row} ${styles.rowEven}`}>
                                    <TableCell colSpan={3} className={styles.cell}>
                                        No tables extracted for slide {currentSlide}.
                                    </TableCell>
                                </TableRow>
                            </TableBody>
                        </Table>
                    </div>
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
