import {
    makeStyles,
    tokens,
    Button,
    Dialog,
    DialogSurface,
    DialogTitle,
    DialogBody,
    DialogContent,
    DialogActions,
    Body1,
    Title3,
} from '@fluentui/react-components';
import { QuestionCircle24Regular } from '@fluentui/react-icons';

const useStyles = makeStyles({
    content: {
        display: 'flex',
        flexDirection: 'column',
        gap: tokens.spacingVerticalL,
    },
    section: {
        display: 'flex',
        flexDirection: 'column',
        gap: tokens.spacingVerticalS,
    },
    codeBlock: {
        backgroundColor: tokens.colorNeutralBackground2,
        padding: tokens.spacingHorizontalM,
        borderRadius: tokens.borderRadiusMedium,
        fontFamily: 'monospace',
        fontSize: tokens.fontSizeBase200,
        overflowX: 'auto',
        whiteSpace: 'pre-wrap',
        wordBreak: 'break-word',
        color: tokens.colorNeutralForeground1,
    },
    highlight: {
        color: tokens.colorBrandForeground1,
        fontWeight: tokens.fontWeightSemibold,
    },
    note: {
        color: tokens.colorNeutralForeground2,
        fontStyle: 'italic',
    },
    axisTable: {
        width: '100%',
        borderCollapse: 'collapse',
        marginTop: tokens.spacingVerticalS,
    },
    axisTableTh: {
        textAlign: 'left',
        padding: `${tokens.spacingVerticalS} ${tokens.spacingHorizontalM}`,
        backgroundColor: tokens.colorNeutralBackground2,
        border: `1px solid ${tokens.colorNeutralStroke1}`,
        fontWeight: tokens.fontWeightSemibold,
    },
    axisTableTd: {
        padding: `${tokens.spacingVerticalS} ${tokens.spacingHorizontalM}`,
        border: `1px solid ${tokens.colorNeutralStroke1}`,
        fontFamily: 'monospace',
        color: tokens.colorNeutralForeground1,
    },
});

interface SqlQueryHelperDialogProps {
    open: boolean;
    onClose: () => void;
}

const EXAMPLE_BAR_CHART = `SELECT 
  category AS LabelX, 
  SUM(amount) AS LabelY 
FROM parsed_tables 
GROUP BY category 
ORDER BY LabelY DESC`;

const EXAMPLE_LINE_CHART = `SELECT 
  date AS LabelX, 
  SUM(revenue) AS LabelY 
FROM parsed_tables 
GROUP BY date 
ORDER BY date`;

const EXAMPLE_PIE_CHART = `SELECT 
  status AS LabelX, 
  COUNT(*) AS LabelY 
FROM parsed_tables 
GROUP BY status`;

const EXAMPLE_TABLE = `SELECT 
  category, 
  SUM(amount) AS total_amount,
  COUNT(*) AS record_count 
FROM parsed_tables 
GROUP BY category 
ORDER BY total_amount DESC`;

export default function SqlQueryHelperDialog({ open, onClose }: SqlQueryHelperDialogProps) {
    const styles = useStyles();

    return (
        <Dialog open={open} onOpenChange={(_ev, data) => !data.open && onClose()}>
            <DialogSurface>
                <DialogBody>
                    <DialogTitle>
                        <div style={{ display: 'flex', alignItems: 'center', gap: tokens.spacingHorizontalS }}>
                            <QuestionCircle24Regular />
                            SQL Query Helper for Charts
                        </div>
                    </DialogTitle>
                    <DialogContent>
                        <div className={styles.content}>
                            <div className={styles.section}>
                                <Title3>Axis Naming Convention</Title3>
                                <Body1>
                                    For chart widgets, use specific column aliases to define axis mapping:
                                </Body1>
                                <table className={styles.axisTable}>
                                    <thead>
                                        <tr>
                                            <th className={styles.axisTableTh}>Alias</th>
                                            <th className={styles.axisTableTh}>Purpose</th>
                                            <th className={styles.axisTableTh}>Used In</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <tr>
                                            <td className={styles.axisTableTd}>LabelX</td>
                                            <td className={styles.axisTableTd}>X-Axis (category/label)</td>
                                            <td className={styles.axisTableTd}>Bar, Line, Pie charts</td>
                                        </tr>
                                        <tr>
                                            <td className={styles.axisTableTd}>LabelY</td>
                                            <td className={styles.axisTableTd}>Y-Axis (value)</td>
                                            <td className={styles.axisTableTd}>Bar, Line charts</td>
                                        </tr>
                                    </tbody>
                                </table>
                                <Body1 className={styles.note}>
                                    If these aliases are not used, charts will automatically use the first column as LabelX
                                    and subsequent columns as data series.
                                </Body1>
                            </div>

                            <div className={styles.section}>
                                <Title3>Bar Chart Example</Title3>
                                <Body1>
                                    Use <span className={styles.highlight}>LabelX</span> for categories and{' '}
                                    <span className={styles.highlight}>LabelY</span> for aggregated values:
                                </Body1>
                                <div className={styles.codeBlock}>{EXAMPLE_BAR_CHART}</div>
                            </div>

                            <div className={styles.section}>
                                <Title3>Line Chart Example</Title3>
                                <Body1>
                                    Similar to bar chart - <span className={styles.highlight}>LabelX</span> for time/sequence
                                    and <span className={styles.highlight}>LabelY</span> for values:
                                </Body1>
                                <div className={styles.codeBlock}>{EXAMPLE_LINE_CHART}</div>
                            </div>

                            <div className={styles.section}>
                                <Title3>Pie Chart Example</Title3>
                                <Body1>
                                    Use <span className={styles.highlight}>LabelX</span> for segment names and{' '}
                                    <span className={styles.highlight}>LabelY</span> for values:
                                </Body1>
                                <div className={styles.codeBlock}>{EXAMPLE_PIE_CHART}</div>
                            </div>

                            <div className={styles.section}>
                                <Title3>Table Widget</Title3>
                                <Body1>
                                    Tables display all columns returned by your query. No special aliases needed:
                                </Body1>
                                <div className={styles.codeBlock}>{EXAMPLE_TABLE}</div>
                            </div>

                            <div className={styles.section}>
                                <Title3>Important Notes</Title3>
                                <ul style={{ margin: 0, paddingLeft: tokens.spacingHorizontalL }}>
                                    <li>
                                        <Body1>Only SELECT queries are allowed for security reasons.</Body1>
                                    </li>
                                    <li>
                                        <Body1>Results are limited to 1000 rows.</Body1>
                                    </li>
                                    <li>
                                        <Body1>
                                            The query automatically filters by your organization ID (org_id).
                                        </Body1>
                                    </li>
                                    <li>
                                        <Body1>
                                            If no WHERE clause is provided, org_id filtering is added automatically.
                                        </Body1>
                                    </li>
                                </ul>
                            </div>
                        </div>
                    </DialogContent>
                    <DialogActions>
                        <Button appearance="primary" onClick={onClose}>
                            Got it
                        </Button>
                    </DialogActions>
                </DialogBody>
            </DialogSurface>
        </Dialog>
    );
}