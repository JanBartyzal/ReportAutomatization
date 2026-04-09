import React from 'react';
import {
    makeStyles,
    tokens,
    Title3,
    Body1,
    Table,
    TableHeader,
    TableRow,
    TableHeaderCell,
    TableBody,
    TableCell,
    TableCellLayout,
    Spinner,
    Tooltip,
} from '@fluentui/react-components';
import { useNavigate } from 'react-router-dom';
import { useReportMatrix } from '../hooks/useReports';
import { usePeriods } from '../hooks/usePeriods';
import { ReportStatus, MatrixRow, Period } from '@reportplatform/types';
import { getStatusColors, getStatusLabel } from '../theme/statusColors';

const useStyles = makeStyles({
    container: {
        padding: tokens.spacingHorizontalL,
        overflowX: 'auto',
    },
    header: {
        marginBottom: tokens.spacingHorizontalXL,
    },
    table: {
        minWidth: '800px',
    },
    stickyHeader: {
        position: 'sticky',
        left: 0,
        backgroundColor: tokens.colorNeutralBackground1,
        zIndex: 1,
        borderRight: `1px solid ${tokens.colorNeutralStroke1}`,
    },
    cell: {
        cursor: 'pointer',
        textAlign: 'center',
        padding: tokens.spacingHorizontalXS,
        transition: 'opacity 0.2s',
        '&:hover': {
            opacity: 0.8,
        },
    },
    statusBox: {
        width: '100%',
        height: '32px',
        borderRadius: tokens.borderRadiusSmall,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        fontSize: '10px',
        fontWeight: tokens.fontWeightSemibold,
    },
    loadingContainer: {
        display: 'flex',
        justifyContent: 'center',
        padding: '100px',
    },
    emptyCell: {
        color: tokens.colorNeutralForeground4,
        fontSize: '10px',
    }
});

export const MatrixDashboard: React.FC = () => {
    const styles = useStyles();
    const navigate = useNavigate();

    const { data: matrix, isLoading: isMatrixLoading } = useReportMatrix();
    const { data: periodsData, isLoading: isPeriodsLoading } = usePeriods();

    if (isMatrixLoading || isPeriodsLoading) {
        return (
            <div className={styles.loadingContainer}>
                <Spinner label="Loading dashboard matrix..." />
            </div>
        );
    }

    const periods = periodsData?.data || [];
    const rows = matrix?.rows || [];

    // Map status to color
    const getStatusStyle = (status: ReportStatus | undefined) => {
        if (!status) return { bg: tokens.colorNeutralBackground3, text: tokens.colorNeutralForegroundDisabled };
        return getStatusColors(status);
    };

    const handleCellClick = (orgId: string, periodId: string, status: ReportStatus | undefined) => {
        if (status) {
            // In a real app, we'd need the report ID. 
            // The matrix API might need to return report IDs as well.
            // For now, let's assume we can navigate to a filtered view or use a deterministic ID if possible.
            // Since the API only returns status, we'll navigate to the reports list filtered by org and period.
            navigate(`/reports?org_id=${orgId}&period_id=${periodId}`);
        }
    };

    return (
        <div className={styles.container}>
            <div className={styles.header}>
                <Title3 block>Reporting Matrix</Title3>
                <Body1 block>High-level overview of report submission status across organizations and periods.</Body1>
            </div>

            <Table className={styles.table}>
                <TableHeader>
                    <TableRow>
                        <TableHeaderCell className={styles.stickyHeader}>Organization</TableHeaderCell>
                        {periods.map(period => (
                            <TableHeaderCell key={period.id} style={{ textAlign: 'center' }}>
                                {period.period_code}
                            </TableHeaderCell>
                        ))}
                    </TableRow>
                </TableHeader>
                <TableBody>
                    {rows.map((row: MatrixRow) => (
                        <TableRow key={row.org_id}>
                            <TableCell className={styles.stickyHeader}>
                                <TableCellLayout>
                                    <Body1><strong>{row.org_name}</strong></Body1>
                                </TableCellLayout>
                            </TableCell>
                            {periods.map((period: Period) => {
                                const status = row.periods[period.id];
                                const style = getStatusStyle(status);
                                
                                return (
                                    <TableCell 
                                        key={period.id} 
                                        className={styles.cell}
                                        onClick={() => handleCellClick(row.org_id, period.id, status)}
                                    >
                                        <Tooltip content={status ? getStatusLabel(status) : 'Missing'} relationship="label">
                                            <div 
                                                className={styles.statusBox}
                                                style={{ backgroundColor: style.bg, color: style.text }}
                                            >
                                                {status ? status.substring(0, 3) : '---'}
                                            </div>
                                        </Tooltip>
                                    </TableCell>
                                );
                            })}
                        </TableRow>
                    ))}
                </TableBody>
            </Table>

            {rows.length === 0 && (
                <div style={{ textAlign: 'center', padding: '48px' }}>
                    <Body1>No data available for the matrix.</Body1>
                </div>
            )}
        </div>
    );
};

export default MatrixDashboard;
