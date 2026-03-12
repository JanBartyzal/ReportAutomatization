import { useState } from 'react';
import {
    Title3,
    Title4,
    Body1,
    Card,
    CardHeader,
    makeStyles,
    tokens,
    Button,
    Dropdown,
    DropdownTrigger,
    Option,
    Input,
    Dialog,
    DialogTrigger,
    DialogSurface,
    DialogTitle,
    DialogBody,
    DialogActions,
    DialogContent,
    Table,
    TableHeader,
    TableRow,
    TableHeaderCell,
    TableBody,
    TableCell,
} from '@fluentui/react-components';
import { Dismiss24Regular } from '@fluentui/react-icons';
import {
    BarChart,
    Bar,
    XAxis,
    YAxis,
    CartesianGrid,
    Tooltip,
    ResponsiveContainer,
    Cell,
} from 'recharts';
import { chartPalette } from '../../theme/brandTokens';
import { usePeriodComparison } from '../../hooks/useDashboards';
import { usePeriods } from '../../hooks/usePeriods';
import LoadingSpinner from '../LoadingSpinner';
import type { PeriodComparisonRequest, FileListParams } from '@reportplatform/types';

/**
 * PeriodComparison styles per docs/UX-UI/02-design-system.md
 */
const useStyles = makeStyles({
    overlay: {
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        backgroundColor: 'rgba(0, 0, 0, 0.5)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 1000,
    },
    dialog: {
        width: '90%',
        maxWidth: '800px',
        maxHeight: '90vh',
        overflow: 'auto',
        backgroundColor: tokens.colorNeutralBackground1,
        borderRadius: tokens.borderRadiusLarge,
        padding: tokens.spacingHorizontalL,
    },
    header: {
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginBottom: tokens.spacingHorizontalL,
    },
    form: {
        display: 'flex',
        flexDirection: 'column',
        gap: tokens.spacingHorizontalM,
        marginBottom: tokens.spacingHorizontalL,
    },
    fields: {
        display: 'grid',
        gridTemplateColumns: 'repeat(2, 1fr)',
        gap: tokens.spacingHorizontalM,
    },
    field: {
        display: 'flex',
        flexDirection: 'column',
        gap: tokens.spacingVerticalXS,
    },
    results: {
        marginTop: tokens.spacingHorizontalL,
    },
    chartPlaceholder: {
        height: '300px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        backgroundColor: tokens.colorNeutralBackground3,
        borderRadius: tokens.borderRadiusMedium,
        marginBottom: tokens.spacingHorizontalL,
    },
    deltaTable: {
        marginTop: tokens.spacingHorizontalL,
    },
    positive: {
        color: 'var(--colorSuccessForeground1)',
    },
    negative: {
        color: 'var(--colorDangerForeground1)',
    },
});

interface PeriodComparisonProps {
    onClose: () => void;
}

export function PeriodComparison({ onClose }: PeriodComparisonProps) {
    const styles = useStyles();
    const { data: periodsData, isLoading: periodsLoading } = usePeriods();
    const comparePeriods = usePeriodComparison();

    const [metric, setMetric] = useState('total_amount');
    const [period1, setPeriod1] = useState<string>('');
    const [period2, setPeriod2] = useState<string>('');
    const [result, setResult] = useState<ComparisonData | null>(null);

    const periods = periodsData?.items || [];

    const handleCompare = async () => {
        if (!period1 || !period2) return;

        try {
            const request: PeriodComparisonRequest = {
                metric,
                period_ids: [period1, period2],
            };
            const data = await comparePeriods.mutateAsync(request);
            setResult(data);
        } catch (error) {
            console.error('Failed to compare periods:', error);
        }
    };

    const formatNumber = (num: number): string => {
        return new Intl.NumberFormat('cs-CZ').format(num);
    };

    const formatPercent = (num: number): string => {
        const sign = num >= 0 ? '+' : '';
        return `${sign}${num.toFixed(1)}%`;
    };

    if (periodsLoading) {
        return <LoadingSpinner label="Loading periods..." />;
    }

    return (
        <div className={styles.overlay} onClick={onClose}>
            <div className={styles.dialog} onClick={(e) => e.stopPropagation()}>
                <div className={styles.header}>
                    <Title3>Period Comparison</Title3>
                    <Button appearance="subtle" icon={<Dismiss24Regular />} onClick={onClose} />
                </div>

                <div className={styles.form}>
                    <div className={styles.fields}>
                        <div className={styles.field}>
                            <Body1><strong>Metric</strong></Body1>
                            <Input
                                value={metric}
                                onChange={(_ev: any, d: any) => setMetric(d.value)}
                                placeholder="e.g., total_amount, count"
                            />
                        </div>
                    </div>

                    <div className={styles.fields}>
                        <div className={styles.field}>
                            <Body1><strong>Period 1</strong></Body1>
                            <Dropdown
                                value={periods.find((p: any) => p.id === period1)?.name || 'Select period'}
                                onOptionSelect={(_ev: any, d: any) => setPeriod1(d.optionValue as string)}
                            >
                                {periods.map((period) => (
                                    <Option key={period.id} value={period.id}>
                                        {period.name}
                                    </Option>
                                ))}
                            </Dropdown>
                        </div>

                        <div className={styles.field}>
                            <Body1><strong>Period 2</strong></Body1>
                            <Dropdown
                                value={periods.find((p: any) => p.id === period2)?.name || 'Select period'}
                                onOptionSelect={(_ev: any, d: any) => setPeriod2(d.optionValue as string)}
                            >
                                {periods.map((period) => (
                                    <Option key={period.id} value={period.id}>
                                        {period.name}
                                    </Option>
                                ))}
                            </Dropdown>
                        </div>
                    </div>

                    <Button
                        appearance="primary"
                        onClick={handleCompare}
                        disabled={!period1 || !period2 || comparePeriods.isPending}
                    >
                        {comparePeriods.isPending ? 'Comparing...' : 'Compare Periods'}
                    </Button>
                </div>

                {result && (
                    <div className={styles.results}>
                        <Title4>Results</Title4>

                        <div className={styles.chartPlaceholder} style={{ backgroundColor: 'transparent' }}>
                            <ResponsiveContainer width="100%" height="100%">
                                <BarChart
                                    data={result.periods}
                                    margin={{ top: 20, right: 30, left: 20, bottom: 5 }}
                                >
                                    <CartesianGrid strokeDasharray="3 3" vertical={false} />
                                    <XAxis dataKey="period_name" />
                                    <YAxis />
                                    <Tooltip 
                                        formatter={(value: number) => [formatNumber(value), metric]}
                                        contentStyle={{ 
                                            borderRadius: '8px', 
                                            border: 'none', 
                                            boxShadow: '0 4px 12px rgba(0,0,0,0.1)' 
                                        }}
                                    />
                                    <Bar dataKey="value" radius={[4, 4, 0, 0]}>
                                        {result.periods.map((_entry: any, index: number) => (
                                            <Cell 
                                                key={`cell-${index}`} 
                                                fill={index === 0 ? chartPalette.chart1 : chartPalette.chart3} 
                                            />
                                        ))}
                                    </Bar>
                                </BarChart>
                            </ResponsiveContainer>
                        </div>

                        <div className={styles.deltaTable}>
                            <Table>
                                <TableHeader>
                                    <TableRow>
                                        <TableHeaderCell>From Period</TableHeaderCell>
                                        <TableHeaderCell>To Period</TableHeaderCell>
                                        <TableHeaderCell>Absolute Change</TableHeaderCell>
                                        <TableHeaderCell>Percentage Change</TableHeaderCell>
                                    </TableRow>
                                </TableHeader>
                                <TableBody>
                                    {result.deltas.map((delta, idx) => (
                                        <TableRow key={idx}>
                                            <TableCell>{delta.from_period}</TableCell>
                                            <TableCell>{delta.to_period}</TableCell>
                                            <TableCell className={delta.absolute_change >= 0 ? styles.positive : styles.negative}>
                                                {formatNumber(delta.absolute_change)}
                                            </TableCell>
                                            <TableCell className={delta.percentage_change >= 0 ? styles.positive : styles.negative}>
                                                {formatPercent(delta.percentage_change)}
                                            </TableCell>
                                        </TableRow>
                                    ))}
                                </TableBody>
                            </Table>
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
}
