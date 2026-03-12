import React, { useState } from 'react';
import {
    Table,
    TableHeader,
    TableRow,
    TableHeaderCell,
    TableBody,
    TableCell,
    TableCellLayout,
    TableSelectionCell,
    Button,
    Toolbar,
    ToolbarContent,
    ToolbarGroup,
    Input,
    Dropdown,
    Option,
    Spinner,
    Body1,
    Caption1,
    makeStyles,
    tokens,
    Title3,
} from '@fluentui/react-components';
import {
    CheckmarkRegular,
    DismissRegular,
    EyeRegular,
    FilterRegular,
} from '@fluentui/react-icons';
import { useNavigate } from 'react-router-dom';
import { 
    useReports, 
    useSubmitReport, 
    useApproveReport, 
    useBulkApprove, 
    useBulkReject 
} from '../hooks/useReports';
import { usePeriods } from '../hooks/usePeriods';
import { useOrganizations } from '../hooks/useAdmin';
import { StatusBadge } from '../components/Lifecycle/StatusBadge';
import { ReportStatus } from '@reportplatform/types';
import RejectionDialog from '../components/Lifecycle/RejectionDialog';

const useStyles = makeStyles({
    container: {
        padding: tokens.spacingHorizontalL,
    },
    header: {
        marginBottom: tokens.spacingHorizontalXL,
    },
    toolbar: {
        marginBottom: tokens.spacingHorizontalM,
    },
    filterInput: {
        width: '200px',
    },
    filterDropdown: {
        width: '180px',
    },
    tableContainer: {
        marginTop: tokens.spacingHorizontalM,
    },
    loadingContainer: {
        display: 'flex',
        justifyContent: 'center',
        padding: '48px',
    },
    actionButton: {
        marginLeft: tokens.spacingHorizontalXS,
    },
    orgCell: {
        color: tokens.colorNeutralForeground4,
    }
});

const statusOptions = [
    { key: '', text: 'All Statuses' },
    { key: ReportStatus.DRAFT, text: 'Draft' },
    { key: ReportStatus.SUBMITTED, text: 'Submitted' },
    { key: ReportStatus.UNDER_REVIEW, text: 'Under Review' },
    { key: ReportStatus.APPROVED, text: 'Approved' },
    { key: ReportStatus.REJECTED, text: 'Rejected' },
];

/**
 * Reports list page with filtering, actions, and bulk operations
 */
export const ReportsPage: React.FC = () => {
    const styles = useStyles();
    const navigate = useNavigate();

    const [selectedReports, setSelectedReports] = useState<string[]>([]);
    const [statusFilter, setStatusFilter] = useState<string>('');
    const [orgFilter, setOrgFilter] = useState<string>('');
    const [periodFilter, setPeriodFilter] = useState<string>('');
    const [searchQuery, setSearchQuery] = useState('');
    const [rejectDialogOpen, setRejectDialogOpen] = useState(false);
    const [rejectingReportId, setRejectingReportId] = useState<string | null>(null);
    const [rejectComment, setRejectComment] = useState('');

    // Fetch data using hooks
    const { data: reportsData, isLoading: reportsLoading } = useReports({ 
        status: statusFilter || undefined,
        org_id: orgFilter || undefined,
        period_id: periodFilter || undefined,
    });
    
    const { data: orgsData } = useOrganizations();
    const { data: periodsData } = usePeriods();

    // Mutations
    const submitMutation = useSubmitReport();
    const approveMutation = useApproveReport();
    const bulkApproveMutation = useBulkApprove();
    const bulkRejectMutation = useBulkReject();

    const handleRowSelect = (reportId: string, checked: boolean) => {
        if (checked) {
            setSelectedReports([...selectedReports, reportId]);
        } else {
            setSelectedReports(selectedReports.filter(id => id !== reportId));
        }
    };

    const handleSelectAll = (checked: boolean) => {
        if (checked && reportsData?.data) {
            setSelectedReports(reportsData.data.map(r => r.id));
        } else {
            setSelectedReports([]);
        }
    };

    const handleSubmit = (reportId: string) => {
        submitMutation.mutate(reportId);
    };

    const handleApprove = (reportId: string) => {
        approveMutation.mutate(reportId);
    };

    const handleReject = (reportId: string) => {
        setRejectingReportId(reportId);
        setRejectDialogOpen(true);
    };

    const handleBulkReject = () => {
        if (selectedReports.length > 0) {
            setRejectingReportId(null);
            setRejectDialogOpen(true);
        }
    };

    const confirmReject = () => {
        if (rejectingReportId) {
            bulkRejectMutation.mutate({ reportIds: [rejectingReportId], comment: rejectComment }, {
                onSuccess: () => {
                    setRejectDialogOpen(false);
                    setRejectComment('');
                }
            });
        } else {
            bulkRejectMutation.mutate({ reportIds: selectedReports, comment: rejectComment }, {
                onSuccess: () => {
                    setRejectDialogOpen(false);
                    setRejectComment('');
                    setSelectedReports([]);
                }
            });
        }
    };

    const handleBulkApprove = () => {
        if (selectedReports.length > 0) {
            bulkApproveMutation.mutate(selectedReports, {
                onSuccess: () => setSelectedReports([])
            });
        }
    };

    if (reportsLoading) {
        return (
            <div className={styles.loadingContainer}>
                <Spinner label="Loading reports..." />
            </div>
        );
    }

    const reports = reportsData?.data || [];
    const organizations = orgsData || [];
    const periods = periodsData?.data || [];

    return (
        <div className={styles.container}>
            {/* Header */}
            <div className={styles.header}>
                <Title3>Reports</Title3>
                <Body1 block>Manage and track all reports across organizations and periods.</Body1>
            </div>

            {/* Toolbar */}
            <Toolbar className={styles.toolbar}>
                <ToolbarContent>
                    <ToolbarGroup>
                        <Input
                            placeholder="Search report type..."
                            value={searchQuery}
                            onChange={(e, data) => setSearchQuery(data.value)}
                            className={styles.filterInput}
                            contentAfter={<FilterRegular />}
                        />
                        <Dropdown
                            placeholder="Organization"
                            value={organizations.find(o => o.id === orgFilter)?.name}
                            onOptionSelect={(_, data) => setOrgFilter(data.optionValue as string)}
                            className={styles.filterDropdown}
                            style={{ marginLeft: tokens.spacingHorizontalS }}
                        >
                            <Option key="all-orgs" value="">All Organizations</Option>
                            {organizations.map(org => (
                                <Option key={org.id} value={org.id}>{org.name}</Option>
                            ))}
                        </Dropdown>
                        <Dropdown
                            placeholder="Period"
                            value={periods.find(p => p.id === periodFilter)?.name}
                            onOptionSelect={(_, data) => setPeriodFilter(data.optionValue as string)}
                            className={styles.filterDropdown}
                            style={{ marginLeft: tokens.spacingHorizontalS }}
                        >
                            <Option key="all-periods" value="">All Periods</Option>
                            {periods.map(p => (
                                <Option key={p.id} value={p.id}>{p.name}</Option>
                            ))}
                        </Dropdown>
                        <Dropdown
                            placeholder="Status"
                            value={statusOptions.find(o => o.key === statusFilter)?.text}
                            onOptionSelect={(_, data) => setStatusFilter(data.optionValue as string)}
                            className={styles.filterDropdown}
                            style={{ marginLeft: tokens.spacingHorizontalS }}
                        >
                            {statusOptions.map(opt => (
                                <Option key={opt.key} value={opt.key}>{opt.text}</Option>
                            ))}
                        </Dropdown>
                    </ToolbarGroup>

                    <ToolbarGroup>
                        {selectedReports.length > 0 && (
                            <>
                                <Button
                                    appearance="primary"
                                    icon={<CheckmarkRegular />}
                                    onClick={handleBulkApprove}
                                >
                                    Approve ({selectedReports.length})
                                </Button>
                                <Button
                                    icon={<DismissRegular />}
                                    onClick={handleBulkReject}
                                    style={{ marginLeft: tokens.spacingHorizontalS, backgroundColor: tokens.colorPaletteRedBackground3, color: 'white' }}
                                >
                                    Reject ({selectedReports.length})
                                </Button>
                            </>
                        )}
                    </ToolbarGroup>
                </ToolbarContent>
            </Toolbar>

            {/* Reports Table */}
            <div className={styles.tableContainer}>
                <Table>
                    <TableHeader>
                        <TableRow>
                            <TableSelectionCell
                                checked={selectedReports.length === reports.length && reports.length > 0}
                                onChange={(_, data) => handleSelectAll(data.checked as boolean)}
                            />
                            <TableHeaderCell>Report</TableHeaderCell>
                            <TableHeaderCell>Organization</TableHeaderCell>
                            <TableHeaderCell>Period</TableHeaderCell>
                            <TableHeaderCell>Status</TableHeaderCell>
                            <TableHeaderCell>Last Updated</TableHeaderCell>
                            <TableHeaderCell>Actions</TableHeaderCell>
                        </TableRow>
                    </TableHeader>
                    <TableBody>
                        {reports.map((report) => (
                            <TableRow key={report.id}>
                                <TableSelectionCell
                                    checked={selectedReports.includes(report.id)}
                                    onChange={(_, data) => handleRowSelect(report.id, data.checked as boolean)}
                                />
                                <TableCell>
                                    <TableCellLayout>
                                        <Body1 strong>{report.report_type}</Body1>
                                    </TableCellLayout>
                                </TableCell>
                                <TableCell>
                                    <TableCellLayout>
                                        <Caption1 className={styles.orgCell}>{report.org_id}</Caption1>
                                    </TableCellLayout>
                                </TableCell>
                                <TableCell>
                                    <TableCellLayout>
                                        <Caption1>{report.period_id}</Caption1>
                                    </TableCellLayout>
                                </TableCell>
                                <TableCell>
                                    <TableCellLayout>
                                        <StatusBadge status={report.status} />
                                    </TableCellLayout>
                                </TableCell>
                                <TableCell>
                                    <TableCellLayout>
                                        <Caption1>{new Date(report.updated_at).toLocaleDateString()}</Caption1>
                                    </TableCellLayout>
                                </TableCell>
                                <TableCell>
                                    <TableCellLayout>
                                        <Button
                                            size="small"
                                            appearance="subtle"
                                            icon={<EyeRegular />}
                                            onClick={() => navigate(`/reports/${report.id}`)}
                                        />
                                        {report.status === ReportStatus.DRAFT && (
                                            <Button
                                                size="small"
                                                appearance="primary"
                                                onClick={() => handleSubmit(report.id)}
                                                className={styles.actionButton}
                                            >
                                                Submit
                                            </Button>
                                        )}
                                        {report.status === ReportStatus.UNDER_REVIEW && (
                                            <>
                                                <Button
                                                    size="small"
                                                    appearance="primary"
                                                    icon={<CheckmarkRegular />}
                                                    onClick={() => handleApprove(report.id)}
                                                    className={styles.actionButton}
                                                >
                                                    Approve
                                                </Button>
                                                <Button
                                                    size="small"
                                                    icon={<DismissRegular />}
                                                    onClick={() => handleReject(report.id)}
                                                    className={styles.actionButton}
                                                    style={{ backgroundColor: tokens.colorPaletteRedBackground3, color: 'white' }}
                                                >
                                                    Reject
                                                </Button>
                                            </>
                                        )}
                                    </TableCellLayout>
                                </TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </div>

            {/* Rejection Dialog */}
            <RejectionDialog
                open={rejectDialogOpen}
                onClose={() => setRejectDialogOpen(false)}
                onConfirm={confirmReject}
                comment={rejectComment}
                onCommentChange={setRejectComment}
                count={rejectingReportId ? 1 : selectedReports.length}
            />
        </div>
    );
};

export default ReportsPage;
