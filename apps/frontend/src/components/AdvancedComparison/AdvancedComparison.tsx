/**
 * AdvancedComparison Component
 * Multi-org comparison dashboard with horizontal bar charts and drill-down
 * Per docs/UX-UI/02-design-system.md and 03-figma-components.md
 */

import { useState } from 'react';
import {
    Title1,
    Title2,
    Title3,
    Body1,
    Body2,
    Caption1,
    Button,
    Card,
    CardHeader,
    makeStyles,
    tokens,
    Input,
    Spinner,
    Table,
    TableHeader,
    TableRow,
    TableHeaderCell,
    TableBody,
    TableCell,
    Badge,
    Breadcrumb,
    BreadcrumbButton,
    BreadcrumbDivider,
} from '@fluentui/react-components';
import {
    ArrowRight24Regular,
    ChartMultiple24Regular,
    Organization24Regular,
} from '@fluentui/react-icons';
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
import { ResponsiveHeatMap } from '@nivo/heatmap';
import { reportBrand } from '../../theme/brandTokens';
import { useAdvancedComparison, useMultiOrgComparison } from '../../hooks/useFeatureFlags';
 
// Mapped from design system or local fallback
const chartPalette = {
    chart1: '#0078d4',
    chart2: '#107c10',
    chart3: '#d83b01',
    chart4: '#002050',
    chart5: '#5c2d91',
    chart6: '#008272',
    chart7: '#00188f',
    chart8: '#004b50',
};

// Styles per design system
const useStyles = makeStyles({
    container: {
        padding: tokens.spacingHorizontalL,
        maxWidth: '1400px',
        margin: '0 auto',
    },
    header: {
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginBottom: tokens.spacingHorizontalL,
    },
    headerLeft: {
        display: 'flex',
        flexDirection: 'column',
        gap: tokens.spacingVerticalXS,
    },
    controls: {
        display: 'flex',
        gap: tokens.spacingHorizontalM,
        marginBottom: tokens.spacingHorizontalL,
        flexWrap: 'wrap',
    },
    controlGroup: {
        display: 'flex',
        flexDirection: 'column',
        gap: tokens.spacingVerticalXS,
        minWidth: '200px',
    },
    cardsGrid: {
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(400px, 1fr))',
        gap: tokens.spacingHorizontalL,
        marginBottom: tokens.spacingHorizontalL,
    },
    chartCard: {
        padding: tokens.spacingHorizontalM,
    },
    chartContainer: {
        height: '350px',
        marginTop: tokens.spacingVerticalM,
    },
    tableCard: {
        padding: tokens.spacingHorizontalM,
    },
    metricCards: {
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
        gap: tokens.spacingHorizontalM,
        marginBottom: tokens.spacingHorizontalL,
    },
    metricCard: {
        padding: tokens.spacingHorizontalM,
        textAlign: 'center',
    },
    metricValue: {
        fontSize: tokens.fontSizeHero900,
        fontWeight: tokens.fontWeightBold,
    },
    metricLabel: {
        color: tokens.colorNeutralForeground2,
        marginTop: tokens.spacingVerticalXS,
    },
    positive: {
        color: 'var(--colorSuccessForeground1)',
    },
    negative: {
        color: 'var(--colorDangerForeground1)',
    },
    breadcrumb: {
        marginBottom: tokens.spacingHorizontalM,
    },
    compareButton: {
        marginTop: tokens.spacingVerticalM,
    },
    emptyState: {
        textAlign: 'center',
        padding: '60px 20px',
        backgroundColor: tokens.colorNeutralBackground2,
        borderRadius: tokens.borderRadiusMedium,
    },
});

// Types for comparison data
interface OrganizationData {
    org_id: string;
    org_name: string;
    value: number;
    previous_value?: number;
    change_percent?: number;
}

interface PeriodData {
    period_id: string;
    period_name: string;
    value: number;
}

interface ComparisonResult {
    metric: string;
    organizations: OrganizationData[];
    periods: PeriodData[];
    timeframe: {
        start: string;
        end: string;
    };
}

// Mock data generator for demonstration (will be replaced with API calls)
const generateMockData = (metric: string, orgs: string[]): ComparisonResult => {
    const organizations: OrganizationData[] = orgs.map((org, index) => {
        const value = Math.random() * 100000 + 50000;
        const previousValue = value * (0.8 + Math.random() * 0.4);
        return {
            org_id: `org-${index}`,
            org_name: org,
            value: Math.round(value),
            previous_value: Math.round(previousValue),
            change_percent: Math.round(((value - previousValue) / previousValue) * 100 * 10) / 10,
        };
    }).sort((a, b) => b.value - a.value);

    return {
        metric,
        organizations,
        periods: [
            { period_id: 'p1', period_name: 'Q1 2024', value: Math.random() * 500000 + 300000 },
            { period_id: 'p2', period_name: 'Q2 2024', value: Math.random() * 500000 + 300000 },
            { period_id: 'p3', period_name: 'Q3 2024', value: Math.random() * 500000 + 300000 },
            { period_id: 'p4', period_name: 'Q4 2024', value: Math.random() * 500000 + 300000 },
        ],
        timeframe: {
            start: '2024-01-01',
            end: '2024-12-31',
        },
    };
};

interface AdvancedComparisonProps {
    onClose?: () => void;
    embedded?: boolean;
}

export function AdvancedComparison({ onClose, embedded = false }: AdvancedComparisonProps) {
    const styles = useStyles();
    const enableAdvanced = useAdvancedComparison();
    const enableMultiOrg = useMultiOrgComparison();

    const [metric, setMetric] = useState('total_amount');
    const [selectedOrgs, setSelectedOrgs] = useState<string[]>([]);
    const [isLoading, setIsLoading] = useState(false);
    const [result, setResult] = useState<ComparisonResult | null>(null);
    const [drillDownOrg, setDrillDownOrg] = useState<string | null>(null);

    // Sample organizations (will come from API)
    const availableOrgs = ['Acme Corp', 'Beta Inc', 'Gamma LLC', 'Delta Co', 'Epsilon Ltd'];

    const handleCompare = async () => {
        setIsLoading(true);
        try {
            // Simulate API call - replace with actual API
            await new Promise(resolve => setTimeout(resolve, 1000));
            const data = generateMockData(metric, selectedOrgs.length > 0 ? selectedOrgs : availableOrgs);
            setResult(data);
        } catch (error) {
            console.error('Failed to compare:', error);
        } finally {
            setIsLoading(false);
        }
    };

    const handleOrgClick = (orgName: string) => {
        setDrillDownOrg(orgName);
    };

    const handleBreadcrumbClick = () => {
        setDrillDownOrg(null);
    };

    const formatNumber = (num: number): string => {
        return new Intl.NumberFormat('cs-CZ').format(Math.round(num));
    };

    const formatPercent = (num: number): string => {
        const sign = num >= 0 ? '+' : '';
        return `${sign}${num.toFixed(1)}%`;
    };

    // Show feature not enabled message
    if (!enableAdvanced) {
        return (
            <div className={styles.emptyState}>
                <ChartMultiple24Regular style={{ fontSize: '48px', color: '#ccc' }} />
                <Title1 style={{ marginTop: tokens.spacingHorizontalM }}>
                    Advanced Comparison
                </Title1>
                <Body1 style={{ color: tokens.colorNeutralForeground2, marginTop: tokens.spacingVerticalS }}>
                    This feature is not enabled. Contact your administrator to enable it.
                </Body1>
            </div>
        );
    }

    return (
        <div className={embedded ? undefined : styles.container}>
            {/* Breadcrumb for drill-down */}
            {drillDownOrg && (
                <Breadcrumb className={styles.breadcrumb}>
                    <BreadcrumbButton onClick={handleBreadcrumbClick}>
                        All Organizations
                    </BreadcrumbButton>
                    <BreadcrumbDivider />
                    <BreadcrumbButton current>
                        {drillDownOrg}
                    </BreadcrumbButton>
                </Breadcrumb>
            )}

            {/* Header */}
            {!embedded && (
                <div className={styles.header}>
                    <div className={styles.headerLeft}>
                        <Title3>Advanced Comparison</Title3>
                        <Body2 style={{ color: tokens.colorNeutralForeground2 }}>
                            Compare metrics across organizations and time periods
                        </Body2>
                    </div>
                    {onClose && (
                        <Button appearance="subtle" onClick={onClose}>
                            Close
                        </Button>
                    )}
                </div>
            )}

            {/* Controls */}
            <div className={styles.controls}>
                <div className={styles.controlGroup}>
                    <Body1><strong>Metric</strong></Body1>
                    <Input
                        value={metric}
                        onChange={(_ev: any, d: any) => setMetric(d.value)}
                        placeholder="e.g., total_amount, revenue, count"
                    />
                </div>

                {enableMultiOrg && (
                    <div className={styles.controlGroup}>
                        <Body1><strong>Organizations</strong></Body1>
                        <div style={{ display: 'flex', gap: tokens.spacingHorizontalS, flexWrap: 'wrap' }}>
                            {availableOrgs.map((org) => (
                                <Button
                                    key={org}
                                    appearance={selectedOrgs.includes(org) ? 'primary' : 'secondary'}
                                    size="small"
                                    onClick={() => {
                                        setSelectedOrgs(prev =>
                                            prev.includes(org)
                                                ? prev.filter(o => o !== org)
                                                : [...prev, org]
                                        );
                                    }}
                                >
                                    {org}
                                </Button>
                            ))}
                        </div>
                    </div>
                )}
            </div>

            {/* Compare Button */}
            <div style={{ display: 'flex', gap: tokens.spacingHorizontalM, alignItems: 'center', marginTop: tokens.spacingVerticalM }}>
                <Button
                    appearance="primary"
                    icon={<ArrowRight24Regular />}
                    onClick={handleCompare}
                    disabled={isLoading}
                    style={{ backgroundColor: reportBrand[90] }}
                >
                    {isLoading ? 'Comparing...' : 'Compare'}
                </Button>

                {result && (
                    <Button
                        appearance="secondary"
                        icon={<ChartMultiple24Regular />}
                        onClick={() => alert('Exporting to PPTX...')}
                    >
                        Export to PPTX
                    </Button>
                )}
            </div>

            {/* Loading State */}
            {isLoading && (
                <div style={{ textAlign: 'center', padding: '40px' }}>
                    <Spinner label="Comparing data..." />
                </div>
            )}

            {/* Results */}
            {result && !isLoading && (
                <>
                    {/* Metric Cards */}
                    <div className={styles.metricCards}>
                        <Card className={styles.metricCard}>
                            <div className={styles.metricValue} style={{ color: chartPalette.chart1 }}>
                                {formatNumber(result.organizations.reduce((sum, org) => sum + org.value, 0))}
                            </div>
                            <div className={styles.metricLabel}>Total Value</div>
                        </Card>
                        <Card className={styles.metricCard}>
                            <div className={styles.metricValue} style={{ color: chartPalette.chart2 }}>
                                {formatNumber(result.organizations.reduce((sum, org) => sum + (org.change_percent || 0), 0) / result.organizations.length)}
                            </div>
                            <div className={styles.metricLabel}>Avg Change %</div>
                        </Card>
                        <Card className={styles.metricCard}>
                            <div className={styles.metricValue} style={{ color: chartPalette.chart3 }}>
                                {result.organizations.length}
                            </div>
                            <div className={styles.metricLabel}>Organizations</div>
                        </Card>
                    </div>

                    {/* Horizontal Bar Chart */}
                    <Card className={styles.chartCard}>
                        <CardHeader
                            header={<Title2>Organization Comparison</Title2>}
                            description={<Caption1>Horizontal bar chart with chart palette colors</Caption1>}
                        />
                        <div className={styles.chartContainer}>
                            <ResponsiveContainer width="100%" height="100%">
                                <BarChart
                                    data={result.organizations}
                                    layout="vertical"
                                    margin={{ top: 20, right: 30, left: 100, bottom: 5 }}
                                >
                                    <CartesianGrid strokeDasharray="3 3" horizontal={true} vertical={false} />
                                    <XAxis type="number" />
                                    <YAxis
                                        dataKey="org_name"
                                        type="category"
                                        width={90}
                                        tick={{ fontSize: 12 }}
                                    />
                                    <Tooltip
                                        formatter={(value: number) => [formatNumber(value), metric]}
                                        contentStyle={{
                                            borderRadius: '8px',
                                            border: 'none',
                                            boxShadow: '0 4px 12px rgba(0,0,0,0.1)'
                                        }}
                                    />
                                    <Bar
                                        dataKey="value"
                                        radius={[0, 4, 4, 0]}
                                        onClick={(data) => handleOrgClick(data.org_name)}
                                        style={{ cursor: 'pointer' }}
                                    >
                                        {result.organizations.map((_, index) => (
                                            <Cell
                                                key={`cell-${index}`}
                                                fill={chartPalette[`chart${(index % 8) + 1}` as keyof typeof chartPalette]}
                                            />
                                        ))}
                                    </Bar>
                                </BarChart>
                            </ResponsiveContainer>
                        </div>
                    </Card>

                    {/* Heatmap Chart */}
                    <Card className={styles.chartCard} style={{ marginTop: tokens.spacingHorizontalL }}>
                        <CardHeader
                            header={<Title2>Heatmap: Organizations vs Periods</Title2>}
                            description={<Caption1>Color density represents metric value</Caption1>}
                        />
                        <div className={styles.chartContainer} style={{ height: '450px' }}>
                            <ResponsiveHeatMap
                                data={result.organizations.map((org: any) => ({
                                    id: org.org_name,
                                    data: result.periods.map((p: any) => ({
                                        x: p.period_name,
                                        y: Math.round(org.value * (0.8 + Math.random() * 0.4)) // Simulated per-period data
                                    }))
                                }))}
                                margin={{ top: 60, right: 90, bottom: 60, left: 90 }}
                                valueFormat=">-.2s"
                                axisTop={{
                                    tickSize: 5,
                                    tickPadding: 5,
                                    tickRotation: -45,
                                    legend: '',
                                    legendOffset: 46
                                }}
                                axisLeft={{
                                    tickSize: 5,
                                    tickPadding: 5,
                                    tickRotation: 0,
                                    legend: 'Organizations',
                                    legendPosition: 'middle',
                                    legendOffset: -72
                                }}
                                colors={{
                                    type: 'sequential',
                                    scheme: 'blues'
                                }}
                                emptyColor="#555555"
                                opacity={0.9}
                                labelTextColor={{
                                    from: 'color',
                                    modifiers: [['darker', 2]]
                                }}
                                // mesh={true} // Commented out as it might not be supported in this version
                                activeOpacity={1}
                                inactiveOpacity={0.4}
                                theme={{
                                    axis: {
                                        ticks: {
                                            text: {
                                                fill: tokens.colorNeutralForeground1
                                            }
                                        },
                                        legend: {
                                            text: {
                                                fill: tokens.colorNeutralForeground1,
                                                fontWeight: 'bold'
                                            }
                                        }
                                    },
                                    grid: {
                                        line: {
                                            stroke: tokens.colorNeutralStroke1
                                        }
                                    }
                                }}
                            />
                        </div>
                    </Card>

                    {/* Period Trend Chart */}
                    <Card className={styles.chartCard} style={{ marginTop: tokens.spacingHorizontalL }}>
                        <CardHeader
                            header={<Title2>Period Trend</Title2>}
                            description={<Caption1>Quarterly comparison across all organizations</Caption1>}
                        />
                        <div className={styles.chartContainer}>
                            <ResponsiveContainer width="100%" height="100%">
                                <BarChart
                                    data={result.periods}
                                    margin={{ top: 20, right: 30, left: 20, bottom: 5 }}
                                >
                                    <CartesianGrid strokeDasharray="3 3" vertical={false} />
                                    <XAxis dataKey="period_name" />
                                    <YAxis />
                                    <Tooltip
                                        formatter={(value: number) => [formatNumber(value), 'Value']}
                                        contentStyle={{
                                            borderRadius: '8px',
                                            border: 'none',
                                            boxShadow: '0 4px 12px rgba(0,0,0,0.1)'
                                        }}
                                    />
                                    <Bar dataKey="value" radius={[4, 4, 0, 0]} fill={reportBrand[90]} />
                                </BarChart>
                            </ResponsiveContainer>
                        </div>
                    </Card>

                    {/* Detailed Table */}
                    <Card className={styles.tableCard} style={{ marginTop: tokens.spacingHorizontalL }}>
                        <CardHeader
                            header={<Title2>Detailed Comparison</Title2>}
                            description={<Caption1>Click on any organization to see detailed breakdown</Caption1>}
                        />
                        <Table>
                            <TableHeader>
                                <TableRow>
                                    <TableHeaderCell>Organization</TableHeaderCell>
                                    <TableHeaderCell>Value</TableHeaderCell>
                                    <TableHeaderCell>Previous</TableHeaderCell>
                                    <TableHeaderCell>Change</TableHeaderCell>
                                    <TableHeaderCell>Trend</TableHeaderCell>
                                </TableRow>
                            </TableHeader>
                            <TableBody>
                                {result.organizations.map((org) => (
                                    <TableRow
                                        key={org.org_id}
                                        onClick={() => handleOrgClick(org.org_name)}
                                        style={{ cursor: 'pointer' }}
                                    >
                                        <TableCell>
                                            <div style={{ display: 'flex', alignItems: 'center', gap: tokens.spacingHorizontalS }}>
                                                <Organization24Regular />
                                                <Body2>{org.org_name}</Body2>
                                            </div>
                                        </TableCell>
                                        <TableCell>
                                            <Body2><strong>{formatNumber(org.value)}</strong></Body2>
                                        </TableCell>
                                        <TableCell>
                                            <Body2>{formatNumber(org.previous_value || 0)}</Body2>
                                        </TableCell>
                                        <TableCell>
                                            <Badge
                                                appearance="filled"
                                                color={org.change_percent! >= 0 ? 'success' : 'danger'}
                                            >
                                                {formatPercent(org.change_percent || 0)}
                                            </Badge>
                                        </TableCell>
                                        <TableCell>
                                            {org.change_percent! >= 0 ? (
                                                <span className={styles.positive}>↑</span>
                                            ) : (
                                                <span className={styles.negative}>↓</span>
                                            )}
                                        </TableCell>
                                    </TableRow>
                                ))}
                            </TableBody>
                        </Table>
                    </Card>
                </>
            )}
        </div>
    );
}

export default AdvancedComparison;