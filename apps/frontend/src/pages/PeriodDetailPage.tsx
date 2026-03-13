import React from 'react';
import {
    makeStyles,
    tokens,
    Title3,
    Body1,
    Body2,
    Caption1,
    Button,
    Card,
    Spinner,
    ProgressBar,
    Table,
    TableHeader,
    TableRow,
    TableHeaderCell,
    TableBody,
    TableCell,
    TableCellLayout,
    Divider,
} from '@fluentui/react-components';
import {
    ArrowLeftRegular,
    CalendarMonthRegular,
    DocumentPdfRegular,
    TableRegular,
} from '@fluentui/react-icons';
import { useParams, useNavigate } from 'react-router-dom';
import { usePeriods } from '../hooks/usePeriods';
import { useReportMatrix } from '../hooks/useReports';
import { getStatusColors, getStatusLabel } from '../theme/statusColors';

const useStyles = makeStyles({
    container: {
        padding: tokens.spacingHorizontalL,
        maxWidth: '1200px',
        margin: '0 auto',
    },
    header: {
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'baseline',
        marginBottom: tokens.spacingHorizontalXL,
    },
    backButton: {
        marginBottom: tokens.spacingHorizontalM,
    },
    contentGrid: {
        display: 'grid',
        gridTemplateColumns: '1fr',
        gap: tokens.spacingHorizontalL,
    },
    card: {
        padding: tokens.spacingHorizontalL,
        marginBottom: tokens.spacingHorizontalL,
    },
    infoGrid: {
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
        gap: tokens.spacingHorizontalXL,
        marginBottom: tokens.spacingHorizontalL,
    },
    label: {
        color: tokens.colorNeutralForeground4,
        marginBottom: tokens.spacingHorizontalXXS,
        display: 'flex',
        alignItems: 'center',
        gap: '4px',
    },
    progressSection: {
        marginTop: tokens.spacingHorizontalXL,
        marginBottom: tokens.spacingHorizontalXL,
    },
    matrixContainer: {
        marginTop: tokens.spacingHorizontalL,
        overflowX: 'auto',
    },
    statusBox: {
        width: '24px',
        height: '24px',
        borderRadius: tokens.borderRadiusSmall,
        margin: '0 auto',
    },
    loadingContainer: {
        display: 'flex',
        justifyContent: 'center',
        padding: '100px',
    }
});

export const PeriodDetailPage: React.FC = () => {
    const styles = useStyles();
    const { periodId } = useParams<{ periodId: string }>();
    const navigate = useNavigate();

    const { data: periodsData, isLoading: isPeriodLoading } = usePeriods();
    const { data: matrix, isLoading: isMatrixLoading } = useReportMatrix();

    const period = periodsData?.data?.find((p: any) => p.id === periodId);

    if (isPeriodLoading || isMatrixLoading) {
        return (
            <div className={styles.loadingContainer}>
                <Spinner label="Loading period details..." />
            </div>
        );
    }

    if (!period) {
        return (
            <div className={styles.container}>
                <Button icon={<ArrowLeftRegular />} onClick={() => navigate('/periods')}>Back to Periods</Button>
                <Title3>Period not found</Title3>
            </div>
        );
    }

    const rows = matrix?.rows || [];

    return (
        <div className={styles.container}>
            <Button appearance="subtle" icon={<ArrowLeftRegular />} onClick={() => navigate('/periods')} className={styles.backButton}>
                Back to Periods
            </Button>

            <div className={styles.header}>
                <div>
                    <Title3>{period.name}</Title3>
                    <Caption1>Code: {period.period_code} | Type: {period.type}</Caption1>
                </div>
                <div style={{ display: 'flex', gap: tokens.spacingHorizontalS }}>
                    <Button icon={<DocumentPdfRegular />}>PDF Export</Button>
                    <Button icon={<TableRegular />}>Excel Export</Button>
                </div>
            </div>

            <Card className={styles.card}>
                <div className={styles.infoGrid}>
                    <div>
                        <div className={styles.label}>
                            <CalendarMonthRegular style={{ fontSize: 16 }} />
                            Duration
                        </div>
                        <Body1 block>
                            <strong>
                                {new Date((period as any).start_date).toLocaleDateString()} - {new Date((period as any).submission_deadline).toLocaleDateString()}
                            </strong>
                        </Body1>
                    </div>
                    <div>
                        <div className={styles.label}>Submission Deadline</div>
                        <Body1 block style={{ color: tokens.colorPaletteRedForeground1 }}>
                            <strong>
                                {new Date((period as any).submission_deadline).toLocaleDateString()}
                            </strong>
                        </Body1>
                    </div>
                    <div>
                        <div className={styles.label}>Review Deadline</div>
                        <Body1 block>
                            <strong>
                                {new Date((period as any).review_deadline).toLocaleDateString()}
                            </strong>
                        </Body1>
                    </div>
                    <div>
                        <div className={styles.label}>Overall Status</div>
                        <Body1 block><strong>{(period as any).status}</strong></Body1>
                    </div>
                </div>

                <Divider style={{ margin: `${tokens.spacingHorizontalM} 0` }} />

                <div className={styles.progressSection}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: tokens.spacingHorizontalXS }}>
                        <Body1 block><strong>Submission Progress</strong></Body1>
                        <Body1>65%</Body1>
                    </div>
                    <ProgressBar value={0.65} color="brand" />
                    <Caption1 block style={{ marginTop: tokens.spacingHorizontalXS }}>
                        42 of 64 organizations have submitted their reports.
                    </Caption1>
                </div>
            </Card>

            <Title3>Organization Status Matrix</Title3>
            <Body2 block>Submission status for all organizations in this period.</Body2>

            <Card className={styles.matrixContainer}>
                <Table>
                    <TableHeader>
                        <TableRow>
                            <TableHeaderCell>Organization</TableHeaderCell>
                            <TableHeaderCell style={{ textAlign: 'center' }}>Status</TableHeaderCell>
                            <TableHeaderCell>Last Transition</TableHeaderCell>
                            <TableHeaderCell>Actions</TableHeaderCell>
                        </TableRow>
                    </TableHeader>
                    <TableBody>
                        {rows.map((row: any) => {
                            const status = row.periods[period.id];
                            const style = status ? getStatusColors(status) : { bg: tokens.colorNeutralBackground3, text: tokens.colorNeutralForegroundDisabled };
                            
                            return (
                                <TableRow key={row.org_id}>
                                    <TableCell>
                                        <TableCellLayout>
                                            <Body1 block><strong>{row.org_name}</strong></Body1>
                                            <Caption1 block>{row.org_id}</Caption1>
                                        </TableCellLayout>
                                    </TableCell>
                                    <TableCell>
                                        <div 
                                            className={styles.statusBox}
                                            style={{ backgroundColor: style.bg }}
                                            title={status ? getStatusLabel(status) : 'Missing'}
                                        />
                                        <Caption1 block style={{ textAlign: 'center', marginTop: '4px' }}>
                                            {status ? getStatusLabel(status) : 'N/A'}
                                        </Caption1>
                                    </TableCell>
                                    <TableCell>
                                        <Caption1>2 days ago by System</Caption1>
                                    </TableCell>
                                    <TableCell>
                                        <Button 
                                            appearance="subtle" 
                                            size="small"
                                            onClick={() => navigate(`/reports?org_id=${row.org_id}&period_id=${period.id}`)}
                                        >
                                            View Report
                                        </Button>
                                    </TableCell>
                                </TableRow>
                            );
                        })}
                    </TableBody>
                </Table>
            </Card>
        </div>
    );
};

export default PeriodDetailPage;
