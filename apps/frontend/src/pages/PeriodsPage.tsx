import React, { useState } from 'react';
import {
    Table,
    TableHeader,
    TableRow,
    TableHeaderCell,
    TableBody,
    TableCell,
    TableCellLayout,
    Button,
    Input,
    Dropdown,
    Option,
    Spinner,
    Body1,
    Title3,
    Caption1,
    ProgressBar,
    makeStyles,
    tokens,
    Divider,
} from '@fluentui/react-components';
import {
    AddRegular,
    CopyRegular,
    EyeRegular,
    CalendarMonthRegular,
    FilterRegular,
} from '@fluentui/react-icons';
import { useNavigate } from 'react-router-dom';
import { usePeriods, useClonePeriod } from '../hooks/usePeriods';

import { StatusBadge } from '../components/Lifecycle/StatusBadge';

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
    filterBar: {
        display: 'flex',
        gap: tokens.spacingHorizontalS,
        marginBottom: tokens.spacingHorizontalL,
        alignItems: 'center',
    },
    filterInput: {
        width: '300px',
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
    emptyState: {
        textAlign: 'center',
        padding: '48px',
        color: tokens.colorNeutralForeground4,
    },
    emptyIcon: {
        fontSize: '48px',
        marginBottom: tokens.spacingHorizontalM,
    },
    actionButton: {
        marginLeft: tokens.spacingHorizontalXS,
    },
    periodCode: {
        color: tokens.colorNeutralForeground4,
    },
    progressBar: {
        width: '100px',
    },
    progressLabel: {
        marginLeft: tokens.spacingHorizontalXS,
    },
});

const typeOptions = [
    { key: '', text: 'All Types' },
    { key: 'MONTHLY', text: 'Monthly' },
    { key: 'QUARTERLY', text: 'Quarterly' },
    { key: 'ANNUAL', text: 'Annual' },
];

const statusOptions = [
    { key: '', text: 'All Statuses' },
    { key: 'OPEN', text: 'Open' },
    { key: 'COLLECTING', text: 'Collecting' },
    { key: 'REVIEWING', text: 'Reviewing' },
    { key: 'CLOSED', text: 'Closed' },
];

export const PeriodsPage: React.FC = () => {
    const styles = useStyles();
    const navigate = useNavigate();

    const [typeFilter, setTypeFilter] = useState<string>('');
    const [statusFilter, setStatusFilter] = useState<string>('');
    const [searchQuery, setSearchQuery] = useState('');

    const { data: periodsData, isLoading } = usePeriods({
        type: typeFilter || undefined,
    });

    const cloneMutation = useClonePeriod();

    const handleClone = (periodId: string) => cloneMutation.mutate(periodId);

    if (isLoading) {
        return (
            <div className={styles.loadingContainer}>
                <Spinner label="Loading periods..." />
            </div>
        );
    }

    const periods = periodsData?.data || [];

    const filteredPeriods = periods.filter((p: any) => {
        if (statusFilter && p.status !== statusFilter) return false;
        if (searchQuery && !p.name.toLowerCase().includes(searchQuery.toLowerCase())) return false;
        return true;
    });

    return (
        <div className={styles.container}>
            <div className={styles.header}>
                <div>
                    <Title3>Reporting Periods</Title3>
                    <Caption1>Manage reporting periods and track completion across organizations.</Caption1>
                </div>
                <Button
                    appearance="primary"
                    icon={<AddRegular />}
                    onClick={() => navigate('/periods/new')}
                >
                    Create Period
                </Button>
            </div>

            <Divider style={{ marginBottom: tokens.spacingHorizontalL }} />

            <div className={styles.filterBar}>
                <Input
                    placeholder="Search periods..."
                    value={searchQuery}
                    onChange={(_, data) => setSearchQuery(data.value)}
                    className={styles.filterInput}
                    contentAfter={<FilterRegular />}
                />
                <Dropdown
                    placeholder="Filter by type"
                    value={typeOptions.find(o => o.key === typeFilter)?.text}
                    onOptionSelect={(_, data) => setTypeFilter(data.optionValue as string)}
                    className={styles.filterDropdown}
                >
                    {typeOptions.map(opt => (
                        <Option key={opt.key} value={opt.key}>{opt.text}</Option>
                    ))}
                </Dropdown>
                <Dropdown
                    placeholder="Filter by status"
                    value={statusOptions.find(o => o.key === statusFilter)?.text}
                    onOptionSelect={(_, data) => setStatusFilter(data.optionValue as string)}
                    className={styles.filterDropdown}
                >
                    {statusOptions.map(opt => (
                        <Option key={opt.key} value={opt.key}>{opt.text}</Option>
                    ))}
                </Dropdown>
            </div>

            <div className={styles.tableContainer}>
                <Table>
                    <TableHeader>
                        <TableRow>
                            <TableHeaderCell>Period</TableHeaderCell>
                            <TableHeaderCell>Type</TableHeaderCell>
                            <TableHeaderCell>Dates</TableHeaderCell>
                            <TableHeaderCell>Deadlines</TableHeaderCell>
                            <TableHeaderCell>Status</TableHeaderCell>
                            <TableHeaderCell>Progress</TableHeaderCell>
                            <TableHeaderCell>Actions</TableHeaderCell>
                        </TableRow>
                    </TableHeader>
                    <TableBody>
                        {filteredPeriods.map((period: any) => (
                            <TableRow key={period.id}>
                                <TableCell>
                                    <TableCellLayout>
                                        <Body1 block><strong>{period.name}</strong></Body1>
                                        <Caption1 block className={styles.periodCode}>{period.period_code}</Caption1>
                                    </TableCellLayout>
                                </TableCell>
                                <TableCell>
                                    <TableCellLayout>
                                        <Caption1>{period.type}</Caption1>
                                    </TableCellLayout>
                                </TableCell>
                                <TableCell>
                                    <TableCellLayout>
                                        <Caption1 block>
                                            {new Date(period.start_date).toLocaleDateString()} - {new Date(period.end_date).toLocaleDateString()}
                                        </Caption1>
                                    </TableCellLayout>
                                </TableCell>
                                <TableCell>
                                    <TableCellLayout>
                                        <Caption1 block>Sub: {new Date(period.submission_deadline).toLocaleDateString()}</Caption1>
                                        <Caption1 block>Rev: {new Date(period.review_deadline).toLocaleDateString()}</Caption1>
                                    </TableCellLayout>
                                </TableCell>
                                <TableCell>
                                    <TableCellLayout>
                                        <StatusBadge status={period.status} />
                                    </TableCellLayout>
                                </TableCell>
                                <TableCell>
                                    <TableCellLayout>
                                        <ProgressBar
                                            value={60}
                                            className={styles.progressBar}
                                            color="brand"
                                        />
                                        <Caption1 className={styles.progressLabel}>60%</Caption1>
                                    </TableCellLayout>
                                </TableCell>
                                <TableCell>
                                    <TableCellLayout>
                                        <Button
                                            size="small"
                                            appearance="subtle"
                                            icon={<EyeRegular />}
                                            onClick={() => navigate(`/periods/${period.id}`)}
                                        />
                                        <Button
                                            size="small"
                                            appearance="subtle"
                                            icon={<CopyRegular />}
                                            onClick={() => handleClone(period.id)}
                                            className={styles.actionButton}
                                        />
                                    </TableCellLayout>
                                </TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </div>

            {filteredPeriods.length === 0 && (
                <div className={styles.emptyState}>
                    <CalendarMonthRegular className={styles.emptyIcon} />
                    <Body1 block>No periods found matching your criteria.</Body1>
                </div>
            )}
        </div>
    );
};

export default PeriodsPage;
