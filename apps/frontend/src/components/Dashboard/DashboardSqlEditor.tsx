import { useState } from 'react';
import {
    makeStyles,
    tokens,
    Button,
    Text,
    Spinner,
    Card,
} from '@fluentui/react-components';
import {
    Play24Regular,
    Code24Regular,
    Copy24Regular,
} from '@fluentui/react-icons';
import { executeRawSql } from '../../api/dashboards';

const useStyles = makeStyles({
    container: {
        display: 'flex',
        flexDirection: 'column',
        gap: tokens.spacingHorizontalM,
    },
    editorWrapper: {
        position: 'relative',
    },
    editor: {
        width: '100%',
        minHeight: '200px',
        padding: tokens.spacingHorizontalM,
        fontFamily: 'monospace',
        fontSize: tokens.fontSizeBase14,
        border: `1px solid ${tokens.colorNeutralStroke1}`,
        borderRadius: tokens.borderRadiusMedium,
        backgroundColor: tokens.colorNeutralBackground2,
        resize: 'vertical',
        outline: 'none',
        '&:focus': {
            borderColor: tokens.colorBrandForeground1,
        },
    },
    actions: {
        display: 'flex',
        gap: tokens.spacingHorizontalS,
    },
    resultsWrapper: {
        marginTop: tokens.spacingHorizontalM,
    },
    resultsTable: {
        width: '100%',
        borderCollapse: 'collapse',
        fontSize: tokens.fontSizeBase13,
    },
    resultsTableTh: {
        textAlign: 'left',
        padding: `${tokens.spacingVerticalS} ${tokens.spacingHorizontalM}`,
        backgroundColor: tokens.colorNeutralBackground2,
        border: `1px solid ${tokens.colorNeutralStroke1}`,
        fontWeight: tokens.fontWeightSemibold,
    },
    resultsTableTd: {
        padding: `${tokens.spacingVerticalS} ${tokens.spacingHorizontalM}`,
        border: `1px solid ${tokens.colorStroke1}`,
    },
    error: {
        padding: tokens.spacingHorizontalM,
        backgroundColor: tokens.colorPaletteRedBackground2,
        border: `1px solid ${tokens.colorRedForeground1}`,
        borderRadius: tokens.borderRadiusMedium,
        color: tokens.colorRedForeground1,
    },
    info: {
        display: 'flex',
        alignItems: 'center',
        gap: tokens.spacingHorizontalS,
        padding: tokens.spacingHorizontalM,
        backgroundColor: tokens.colorNeutralBackground2,
        borderRadius: tokens.borderRadiusMedium,
        fontSize: tokens.fontSizeBase13,
        color: tokens.colorNeutralForeground2,
    },
});

interface DashboardSqlEditorProps {
    onInsertQuery?: (query: string) => void;
}

export default function DashboardSqlEditor({ onInsertQuery }: DashboardSqlEditorProps) {
    const styles = useStyles();
    const [sql, setSql] = useState('');
    const [results, setResults] = useState<{ columns: string[]; rows: unknown[][] } | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [isExecuting, setIsExecuting] = useState(false);

    const handleExecute = async () => {
        if (!sql.trim()) return;

        setIsExecuting(true);
        setError(null);

        try {
            const result = await executeRawSql(sql);
            setResults(result);
        } catch (err: any) {
            setError(err.response?.data?.message || 'Failed to execute SQL');
            setResults(null);
        } finally {
            setIsExecuting(false);
        }
    };

    const handleCopy = () => {
        navigator.clipboard.writeText(sql);
    };

    return (
        <div className={styles.container}>
            <div className={styles.editorWrapper}>
                <textarea
                    className={styles.editor}
                    value={sql}
                    onChange={(e) => setSql(e.target.value)}
                    placeholder="SELECT * FROM ... -- Enter SQL query"
                    spellCheck={false}
                />
            </div>

            <div className={styles.actions}>
                <Button
                    appearance="primary"
                    icon={isExecuting ? <Spinner size="small" /> : <Play24Regular />}
                    onClick={handleExecute}
                    disabled={isExecuting || !sql.trim()}
                >
                    {isExecuting ? 'Executing...' : 'Execute'}
                </Button>
                <Button
                    appearance="subtle"
                    icon={<Copy24Regular />}
                    onClick={handleCopy}
                    disabled={!sql}
                >
                    Copy
                </Button>
                {onInsertQuery && (
                    <Button
                        appearance="subtle"
                        icon={<Code24Regular />}
                        onClick={() => onInsertQuery(sql)}
                        disabled={!sql}
                    >
                        Insert as Widget
                    </Button>
                )}
            </div>

            {error && (
                <div className={styles.error}>
                    <Text style={{ fontWeight: tokens.fontWeightSemibold }}>Error:</Text>
                    <Text>{error}</Text>
                </div>
            )}

            {results && (
                <div className={styles.resultsWrapper}>
                    <div className={styles.info}>
                        <Play24Regular />
                        <span>
                            {results.rows.length} row(s) returned
                        </span>
                    </div>

                    <div style={{ overflowX: 'auto', marginTop: '12px' }}>
                        <table className={styles.resultsTable}>
                            <thead>
                                <tr>
                                    {results.columns.map((col, i) => (
                                        <th key={i} className={styles.resultsTableTh}>
                                            {col}
                                        </th>
                                    ))}
                                </tr>
                            </thead>
                            <tbody>
                                {results.rows.map((row, rowIdx) => (
                                    <tr key={rowIdx}>
                                        {row.map((cell, cellIdx) => (
                                            <td key={cellIdx} className={styles.resultsTableTd}>
                                                {cell === null ? <em style={{ color: tokens.colorNeutralForeground3 }}>NULL</em> : String(cell)}
                                            </td>
                                        ))}
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </div>
            )}
        </div>
    );
}
