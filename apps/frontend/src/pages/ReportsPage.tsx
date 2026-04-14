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
    ToolbarGroup,
    Input,
    Dropdown,
    Option,
    Spinner,
    Body1,
    Caption1,
    makeStyles,
    tokens,
    Dialog,
    DialogSurface,
    DialogTitle,
    DialogBody,
    DialogContent,
    DialogActions,
    DialogTrigger,
    Label,
} from '@fluentui/react-components';
import {
    CheckmarkRegular,
    DismissRegular,
    EyeRegular,
    FilterRegular,
    AddRegular,
} from '@fluentui/react-icons';
import { useNavigate } from 'react-router-dom';
import {
    useReports,
    useCreateReport,
    useSubmitReport,
    useApproveReport,
    useBulkApprove,
    useBulkReject
} from '../hooks/useReports';
import { usePeriods } from '../hooks/usePeriods';
import { useOrganizations } from '../hooks/useAdmin';
import { StatusBadge } from '../components/shared/StatusBadge';
import { PageHeader } from '../components/shared/PageHeader';
import { ReportStatus } from '@reportplatform/types';
import RejectionDialog from '../components/Lifecycle/RejectionDialog';
import { useToast } from '../components/NotificationCenter/ToastContainer';

const useStyles = makeStyles({
    container: {
        padding: tokens.spacingHorizontalL,
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
    },
    filterGap: {
        marginLeft: tokens.spacingHorizontalS,
    },
    warningBanner: {
        padding: tokens.spacingHorizontalM,
        backgroundColor: tokens.colorStatusWarningBackground1,
        border: `1px solid ${tokens.colorStatusWarningBorder1}`,
        borderRadius: tokens.borderRadiusMedium,
    },
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
 * Reports list page — migrated to shared StatusBadge + PageHeader per P9-W2-002
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
    const [createDialogOpen, setCreateDialogOpen] = useState(false);
    const [newReportOrg, setNewReportOrg] = useState('');
    const [newReportPeriod, setNewReportPeriod] = useState('');
    const [newReportType, setNewReportType] = useState('OPEX');

    const { data: reportsData, isLoading: reportsLoading } = useReports({ 
        status: statusFilter || undefined,
        org_id: orgFilter || undefined,
        period_id: periodFilter || undefined,
    });
    
    const { data: orgsData } = useOrganizations();
    const { data: periodsData } = usePeriods();

    const toast = useToast();
    const createMutation = useCreateReport();
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

    const handleCreate = () => {
        if (!newReportOrg || !newReportPeriod) return;
        createMutation.mutate(
            { org_id: newReportOrg, period_id: newReportPeriod, report_type: newReportType },
            {
                onSuccess: (report) => {
                    setCreateDialogOpen(false);
                    setNewReportOrg('');
                    setNewReportPeriod('');
                    setNewReportType('OPEX');
                    toast('success', 'Report created', `${newReportType} report was created successfully.`);
                    navigate(`/reports/${report.id}`);
                },
                onError: () => {
                    toast('error', 'Failed to create report', 'Please try again or check that the organization and period are valid.');
                },
            }
        );
    };

    const handleSubmit = (reportId: string) => submitMutation.mutate(reportId);
    const handleApprove = (reportId: string) => approveMutation.mutate(reportId);

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
                onSuccess: () => { setRejectDialogOpen(false); setRejectComment(''); }
            });
        } else {
            bulkRejectMutation.mutate({ reportIds: selectedReports, comment: rejectComment }, {
                onSuccess: () => { setRejectDialogOpen(false); setRejectComment(''); setSelectedReports([]); }
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
    const filteredReports = searchQuery
        ? reports.filter(r => r.report_type.toLowerCase().includes(searchQuery.toLowerCase()))
        : reports;

    return (
        <div className={styles.container}>
            <PageHeader
                title="Reports"
                subtitle="Manage and track all reports across organizations and periods."
                actions={
                    <Button
                        appearance="primary"
                        icon={<AddRegular />}
                        onClick={() => setCreateDialogOpen(true)}
                    >
                        New Report
                    </Button>
                }
            />

            {/* Toolbar */}
            <Toolbar className={styles.toolbar}>
                <ToolbarGroup>
                        <Input
                            placeholder="Search report type..."
                            value={searchQuery}
                            onChange={(_, data) => setSearchQuery(data.value)}
                            className={styles.filterInput}
                            contentAfter={<FilterRegular />}
                        />
                        <Dropdown
                            placeholder="Organization"
                            value={organizations.find(o => o.id === orgFilter)?.name}
                            onOptionSelect={(_, data) => setOrgFilter(data.optionValue as string)}
                            className={`${styles.filterDropdown} ${styles.filterGap}`}
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
                            className={`${styles.filterDropdown} ${styles.filterGap}`}
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
                            className={`${styles.filterDropdown} ${styles.filterGap}`}
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
                                    appearance="secondary"
                                    icon={<DismissRegular />}
                                    onClick={handleBulkReject}
                                    className={styles.actionButton}
                                >
                                    Reject ({selectedReports.length})
                                </Button>
                            </>
                        )}
                    </ToolbarGroup>
            </Toolbar>

            {/* Reports Table */}
            <div className={styles.tableContainer}>
                <Table>
                    <TableHeader>
                        <TableRow>
                            <TableSelectionCell
                                checked={selectedReports.length === reports.length && reports.length > 0}
                                // @ts-expect-error Selection events mismatch
                                onChange={(_: any, data: any) => handleSelectAll(data.checked === true || data.checked === 'mixed')}
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
                        {filteredReports.map((report) => (
                            <TableRow key={report.id}>
                                <TableSelectionCell
                                    checked={selectedReports.includes(report.id)}
                                    // @ts-expect-error Selection events mismatch
                                    onChange={(_: any, data: any) => handleRowSelect(report.id, data.checked === true)}
                                />
                                <TableCell>
                                    <TableCellLayout>
                                        <Body1><strong>{report.report_type}</strong></Body1>
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
                                                    appearance="secondary"
                                                    icon={<DismissRegular />}
                                                    onClick={() => handleReject(report.id)}
                                                    className={styles.actionButton}
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

            <RejectionDialog
                open={rejectDialogOpen}
                onClose={() => setRejectDialogOpen(false)}
                onConfirm={confirmReject}
                comment={rejectComment}
                onCommentChange={setRejectComment}
                count={rejectingReportId ? 1 : selectedReports.length}
            />

            {/* Create Report Dialog */}
            <Dialog open={createDialogOpen} onOpenChange={(_ev, data) => setCreateDialogOpen(data.open)}>
                <DialogSurface>
                    <DialogBody>
                        <DialogTitle>Create New Report</DialogTitle>
                        <DialogContent style={{ display: 'flex', flexDirection: 'column', gap: tokens.spacingVerticalM, paddingTop: tokens.spacingVerticalM }}>
                            {(organizations.length === 0 || periods.length === 0) && (
                                <div className={styles.warningBanner}>
                                    <Body1>
                                        {organizations.length === 0 && periods.length === 0
                                            ? 'No organizations or periods configured. Please set them up in Admin first.'
                                            : organizations.length === 0
                                                ? 'No organizations configured. Please create one in Admin \u2192 Manage first.'
                                                : 'No reporting periods configured. Please create one in Admin \u2192 Manage first.'}
                                    </Body1>
                                </div>
                            )}
                            <div>
                                <Label required>Organization</Label>
                                <Dropdown
                                    placeholder="Select organization"
                                    value={organizations.find(o => o.id === newReportOrg)?.name}
                                    onOptionSelect={(_ev, data) => setNewReportOrg(data.optionValue as string)}
                                    style={{ width: '100%' }}
                                >
                                    {organizations.map(org => (
                                        <Option key={org.id} value={org.id}>{org.name}</Option>
                                    ))}
                                </Dropdown>
                            </div>
                            <div>
                                <Label required>Reporting Period</Label>
                                <Dropdown
                                    placeholder="Select period"
                                    value={periods.find(p => p.id === newReportPeriod)?.name}
                                    onOptionSelect={(_ev, data) => setNewReportPeriod(data.optionValue as string)}
                                    style={{ width: '100%' }}
                                >
                                    {periods.map(p => (
                                        <Option key={p.id} value={p.id}>{p.name}</Option>
                                    ))}
                                </Dropdown>
                            </div>
                            <div>
                                <Label required>Report Type</Label>
                                <Dropdown
                                    value={newReportType}
                                    onOptionSelect={(_ev, data) => setNewReportType(data.optionValue as string)}
                                    style={{ width: '100%' }}
                                >
                                    <Option value="OPEX">OPEX Report</Option>
                                    <Option value="CAPEX">CAPEX Report</Option>
                                    <Option value="FINANCIAL">Financial Report</Option>
                                    <Option value="GENERAL">General Report</Option>
                                </Dropdown>
                            </div>
                        </DialogContent>
                        <DialogActions>
                            <DialogTrigger disableButtonEnhancement>
                                <Button appearance="secondary">Cancel</Button>
                            </DialogTrigger>
                            <Button
                                appearance="primary"
                                onClick={handleCreate}
                                disabled={!newReportOrg || !newReportPeriod || createMutation.isPending}
                            >
                                {createMutation.isPending ? 'Creating...' : 'Create Report'}
                            </Button>
                        </DialogActions>
                    </DialogBody>
                </DialogSurface>
            </Dialog>
        </div>
    );
};

export default ReportsPage;
