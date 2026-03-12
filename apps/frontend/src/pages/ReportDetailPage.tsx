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
    CardHeader,
    Spinner,
    Divider,
    Badge,
    Toolbar,
    ToolbarButton,
    ToolbarDivider,
} from '@fluentui/react-components';
import {
    ArrowLeftRegular,
    SendRegular,
    CheckmarkRegular,
    DismissRegular,
    HistoryRegular,
    InfoRegular,
    OrganizationRegular,
    CalendarLtrRegular,
} from '@fluentui/react-icons';
import { useParams, useNavigate } from 'react-router-dom';
import { useReport, useReportHistory, useSubmitReport, useApproveReport, useRejectReport } from '../hooks/useReports';
import { StatusBadge } from '../components/Lifecycle/StatusBadge';
import { Timeline } from '../components/Lifecycle/Timeline';
import { RejectionDialog } from '../components/Lifecycle/RejectionDialog';
import { GenerateButton } from '../components/Generation';
import { ReportStatus } from '@reportplatform/types';

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
        gridTemplateColumns: 'minmax(0, 2fr) minmax(0, 1fr)',
        gap: tokens.spacingHorizontalL,
    },
    card: {
        padding: tokens.spacingHorizontalL,
        height: 'fit-content',
    },
    sectionTitle: {
        marginBottom: tokens.spacingHorizontalM,
        display: 'flex',
        alignItems: 'center',
        gap: tokens.spacingHorizontalS,
    },
    infoGrid: {
        display: 'grid',
        gridTemplateColumns: '1fr 1fr',
        gap: tokens.spacingHorizontalM,
    },
    infoItem: {
        display: 'flex',
        flexDirection: 'column',
        gap: tokens.spacingHorizontalXXS,
    },
    label: {
        color: tokens.colorNeutralForeground4,
        fontSize: tokens.fontSizeBase200,
    },
    actions: {
        marginTop: tokens.spacingHorizontalXL,
        display: 'flex',
        gap: tokens.spacingHorizontalM,
    },
    historyCard: {
        maxHeight: '600px',
        overflowY: 'auto',
    },
    loadingContainer: {
        display: 'flex',
        justifyContent: 'center',
        padding: '100px',
    },
    divider: {
        margin: `${tokens.spacingHorizontalL} 0`,
    }
});

export const ReportDetailPage: React.FC = () => {
    const styles = useStyles();
    const { reportId } = useParams<{ reportId: string }>();
    const navigate = useNavigate();
    const [isRejectionDialogOpen, setIsRejectionDialogOpen] = useState(false);

    const { data: report, isLoading: isReportLoading } = useReport(reportId!);
    const { data: history, isLoading: isHistoryLoading } = useReportHistory(reportId!);

    const submitMutation = useSubmitReport();
    const approveMutation = useApproveReport();
    const rejectMutation = useRejectReport();

    const handleBack = () => navigate('/reports');

    const handleSubmit = () => {
        if (reportId) submitMutation.mutate(reportId);
    };

    const handleApprove = () => {
        if (reportId) approveMutation.mutate(reportId);
    };

    const handleReject = (comment: string) => {
        if (reportId) {
            rejectMutation.mutate({ reportId, comment }, {
                onSuccess: () => setIsRejectionDialogOpen(false)
            });
        }
    };

    if (isReportLoading) {
        return (
            <div className={styles.loadingContainer}>
                <Spinner label="Loading report details..." />
            </div>
        );
    }

    if (!report) {
        return (
            <div className={styles.container}>
                <Button icon={<ArrowLeftRegular />} onClick={handleBack} className={styles.backButton}>Back to Reports</Button>
                <Title3>Report not found</Title3>
            </div>
        );
    }

    const canSubmit = report.status === ReportStatus.DRAFT;
    const canReview = report.status === ReportStatus.SUBMITTED;

    return (
        <div className={styles.container}>
            <Button appearance="subtle" icon={<ArrowLeftRegular />} onClick={handleBack} className={styles.backButton}>
                Back to Reports
            </Button>

            <div className={styles.header}>
                <div>
                    <Title3>{report.report_type} Report</Title3>
                    <div style={{ display: 'flex', alignItems: 'center', gap: tokens.spacingHorizontalS, marginTop: tokens.spacingHorizontalXS }}>
                        <StatusBadge status={report.status} />
                        <Caption1>ID: {report.id}</Caption1>
                        <ToolbarDivider />
                        <Button
                            appearance="subtle"
                            size="small"
                            icon={<HistoryRegular />}
                            onClick={() => navigate(`/admin/audit?entity_type=REPORT&entity_id=${reportId}`)}
                        >
                            View audit trail
                        </Button>
                    </div>
                </div>

                <div className={styles.actions}>
                    {canSubmit && (
                        <Button 
                            appearance="primary" 
                            icon={<SendRegular />} 
                            onClick={handleSubmit}
                            loading={submitMutation.isPending}
                        >
                            Submit Report
                        </Button>
                    )}
                    {canReview && (
                        <>
                            <Button 
                                appearance="primary" 
                                icon={<CheckmarkRegular />} 
                                onClick={handleApprove}
                                loading={approveMutation.isPending}
                            >
                                Approve
                            </Button>
                            <Button 
                                appearance="secondary" 
                                icon={<DismissRegular />} 
                                onClick={() => setIsRejectionDialogOpen(true)}
                                loading={rejectMutation.isPending}
                            >
                                Reject
                            </Button>
                        </>
                    )}
                </div>
            </div>

            <div className={styles.contentGrid}>
                {/* Main Content */}
                <div>
                    <Card className={styles.card}>
                        <div className={styles.sectionTitle}>
                            <InfoRegular />
                            <Title3>Report Information</Title3>
                        </div>
                        
                        <div className={styles.infoGrid}>
                            <div className={styles.infoItem}>
                                <div className={styles.label}>
                                    <OrganizationRegular style={{ marginRight: '4px' }} />
                                    Organization
                                </div>
                                <Body1 strong>{report.org_name || 'N/A'}</Body1>
                                <Caption1>{report.org_id}</Caption1>
                            </div>
                            <div className={styles.infoItem}>
                                <div className={styles.label}>
                                    <CalendarLtrRegular style={{ marginRight: '4px' }} />
                                    Period
                                </div>
                                <Body1 strong>{report.period_name || 'N/A'}</Body1>
                                <Caption1>{report.period_id}</Caption1>
                            </div>
                        </div>

                        <Divider className={styles.divider} />

                        <div className={styles.infoGrid}>
                            <div className={styles.infoItem}>
                                <div className={styles.label}>Report Code</div>
                                <Body1>{report.report_code || 'N/A'}</Body1>
                            </div>
                            <div className={styles.infoItem}>
                                <div className={styles.label}>Completeness</div>
                                <Body1>{(report.completeness_score * 100).toFixed(0)}%</Body1>
                                {/* Progress bar or similar could go here */}
                            </div>
                        </div>

                        <Divider className={styles.divider} />

                        <div>
                            <div className={styles.sectionTitle}>
                                <CheckmarkRegular />
                                <Title3>Submission Checklist</Title3>
                            </div>
                            <Body2>The following items are required for submission:</Body2>
                            <ul style={{ paddingLeft: '20px', marginTop: '8px' }}>
                                <li>All mandatory data fields filled</li>
                                <li>No validation errors</li>
                                <li>Supporting documents attached (if required)</li>
                            </ul>
                        </div>
                    </Card>
                </div>

                {/* Sidebar History */}
                <div>
                    <Card className={styles.card}>
                        <div className={styles.sectionTitle}>
                            <HistoryRegular />
                            <Title3>Status History</Title3>
                        </div>
                        {isHistoryLoading ? (
                            <Spinner size="small" label="Loading history..." />
                        ) : (
                            <div className={styles.historyCard}>
                                <Timeline transitions={history || []} />
                            </div>
                        )}
                    </Card>
                </div>
            </div>

            <RejectionDialog 
                isOpen={isRejectionDialogOpen}
                onOpenChange={setIsRejectionDialogOpen}
                onConfirm={handleReject}
            />
        </div>
    );
};

export default ReportDetailPage;
