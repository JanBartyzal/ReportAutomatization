import React, { useState } from 'react';
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
    Dialog,
    DialogSurface,
    DialogTitle,
    DialogBody,
    DialogContent,
    DialogActions,
    Input,
    Spinner as FluentSpinner,
} from '@fluentui/react-components';
import {
    ArrowLeftRegular,
    CalendarMonthRegular,
    DocumentPdfRegular,
    TableRegular,
    Edit24Regular,
} from '@fluentui/react-icons';
import { useParams, useNavigate } from 'react-router-dom';
import { usePeriods, useUpdatePeriod } from '../hooks/usePeriods';
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
    },
    editForm: {
        display: 'flex',
        flexDirection: 'column',
        gap: tokens.spacingVerticalM,
    },
    field: {
        display: 'flex',
        flexDirection: 'column',
        gap: tokens.spacingVerticalXS,
    },
});

export const PeriodDetailPage: React.FC = () => {
    const styles = useStyles();
    const { periodId } = useParams<{ periodId: string }>();
    const navigate = useNavigate();

    const { data: periodsData, isLoading: isPeriodLoading } = usePeriods();
    const { data: matrix, isLoading: isMatrixLoading } = useReportMatrix();
    const updatePeriod = useUpdatePeriod();

    const [showEditDialog, setShowEditDialog] = useState(false);
    const [editForm, setEditForm] = useState({
        name: '',
        start_date: '',
        submission_deadline: '',
        review_deadline: '',
    });

    const period = periodsData?.data?.find((p: any) => p.id === periodId);

    const openEditDialog = () => {
        if (period) {
            setEditForm({
                name: period.name || '',
                start_date: period.start_date ? period.start_date.split('T')[0] : '',
                submission_deadline: period.submission_deadline ? period.submission_deadline.split('T')[0] : '',
                review_deadline: period.review_deadline ? period.review_deadline.split('T')[0] : '',
            });
            setShowEditDialog(true);
        }
    };

    const handleSaveEdit = async () => {
        if (!periodId) return;
        try {
            await updatePeriod.mutateAsync({
                periodId,
                period: {
                    name: editForm.name,
                    start_date: editForm.start_date,
                    submission_deadline: editForm.submission_deadline,
                    review_deadline: editForm.review_deadline,
                },
            });
            setShowEditDialog(false);
        } catch (error) {
            console.error('Failed to update period:', error);
        }
    };

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
                    <Title3 block>{period.name}</Title3>
                    <Caption1 block>Code: {period.period_code} | Type: {period.type}</Caption1>
                </div>
                <div style={{ display: 'flex', gap: tokens.spacingHorizontalS }}>
                    <Button icon={<Edit24Regular />} onClick={openEditDialog}>Edit Period</Button>
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

            <Title3 block>Organization Status Matrix</Title3>
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

            <Dialog open={showEditDialog} onOpenChange={(_ev, data) => !data.open && setShowEditDialog(false)}>
                <DialogSurface>
                    <DialogBody>
                        <DialogTitle>Edit Reporting Period</DialogTitle>
                        <DialogContent>
                            <div className={styles.editForm}>
                                <div className={styles.field}>
                                    <Body1>Period Name</Body1>
                                    <Input
                                        value={editForm.name}
                                        onChange={(_ev, data) => setEditForm(f => ({ ...f, name: data.value }))}
                                        placeholder="e.g., Q1 2024"
                                    />
                                </div>
                                <div className={styles.field}>
                                    <Body1>Start Date</Body1>
                                    <Input
                                        type="date"
                                        value={editForm.start_date}
                                        onChange={(_ev, data) => setEditForm(f => ({ ...f, start_date: data.value }))}
                                    />
                                </div>
                                <div className={styles.field}>
                                    <Body1>Submission Deadline</Body1>
                                    <Input
                                        type="date"
                                        value={editForm.submission_deadline}
                                        onChange={(_ev, data) => setEditForm(f => ({ ...f, submission_deadline: data.value }))}
                                    />
                                </div>
                                <div className={styles.field}>
                                    <Body1>Review Deadline</Body1>
                                    <Input
                                        type="date"
                                        value={editForm.review_deadline}
                                        onChange={(_ev, data) => setEditForm(f => ({ ...f, review_deadline: data.value }))}
                                    />
                                </div>
                            </div>
                        </DialogContent>
                        <DialogActions>
                            <Button appearance="subtle" onClick={() => setShowEditDialog(false)}>Cancel</Button>
                            <Button
                                appearance="primary"
                                onClick={handleSaveEdit}
                                disabled={updatePeriod.isPending}
                            >
                                {updatePeriod.isPending ? <FluentSpinner size="tiny" /> : 'Save Changes'}
                            </Button>
                        </DialogActions>
                    </DialogBody>
                </DialogSurface>
            </Dialog>
        </div>
    );
};

export default PeriodDetailPage;
