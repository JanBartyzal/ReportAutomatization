import { useState } from 'react';
import {
    makeStyles,
    tokens,
    Button,
    Text,
    Spinner,
} from '@fluentui/react-components';
import {
    Play24Regular,
    Code24Regular,
    Copy24Regular,
    Add24Regular,
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
        fontSize: tokens.fontSizeBase300,
        border: `1px solid ${tokens.colorNeutralStroke1}`,
        borderRadius: tokens.borderRadiusMedium,
        backgroundColor: tokens.colorNeutralBackground2,
        resize: 'vertical',
        outline: 'none',
        color: tokens.colorNeutralForeground1,
        '&:focus': {
            borderColor: tokens.colorBrandStroke1,
        } as any,
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
        fontSize: tokens.fontSizeBase200,
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
        border: `1px solid ${tokens.colorNeutralStroke1}`,
    },
    error: {
        padding: tokens.spacingHorizontalM,
        backgroundColor: tokens.colorStatusDangerBackground1,
        border: `1px solid ${tokens.colorStatusDangerBorder1}`,
        borderRadius: tokens.borderRadiusMedium,
        color: tokens.colorStatusDangerForeground1,
    },
    info: {
        display: 'flex',
        alignItems: 'center',
        gap: tokens.spacingHorizontalS,
        padding: tokens.spacingHorizontalM,
        backgroundColor: tokens.colorNeutralBackground2,
        borderRadius: tokens.borderRadiusMedium,
        fontSize: tokens.fontSizeBase200,
        color: tokens.colorNeutralForeground2,
    },
    snippetBar: {
        display: 'flex',
        flexDirection: 'column',
        gap: tokens.spacingVerticalS,
        padding: tokens.spacingHorizontalM,
        backgroundColor: tokens.colorNeutralBackground2,
        borderRadius: tokens.borderRadiusMedium,
        border: `1px solid ${tokens.colorNeutralStroke1}`,
    },
    snippetLabel: {
        fontSize: tokens.fontSizeBase200,
        fontWeight: tokens.fontWeightSemibold,
        color: tokens.colorNeutralForeground2,
    },
    snippetGroup: {
        display: 'flex',
        flexWrap: 'wrap' as const,
        gap: tokens.spacingHorizontalXS,
    },
    snippetGroupLabel: {
        fontSize: tokens.fontSizeBase100,
        color: tokens.colorNeutralForeground3,
        width: '100%',
        marginTop: tokens.spacingVerticalXS,
    },
});

interface SqlSnippet {
    label: string;
    sql: string;
    description?: string;
}

interface SnippetGroup {
    group: string;
    snippets: SqlSnippet[];
}

const SQL_SNIPPET_GROUPS: SnippetGroup[] = [
    {
        group: 'Files & Data',
        snippets: [
            {
                label: 'All parsed files',
                sql: `SELECT file_id, filename, status, mime_type, uploaded_at\nFROM parsed_files\nORDER BY uploaded_at DESC`,
            },
            {
                label: 'Latest files in batch',
                sql: `SELECT pf.file_id, pf.filename, pf.status, bf.batch_id\nFROM parsed_files pf\nJOIN batch_files bf ON pf.file_id = bf.file_id\nWHERE bf.batch_id = '<BATCH_ID>'\nORDER BY pf.uploaded_at DESC`,
            },
            {
                label: 'Files in period',
                sql: `SELECT pf.file_id, pf.filename, pf.status, pf.uploaded_at\nFROM parsed_files pf\nWHERE pf.period_id = '<PERIOD_ID>'\nORDER BY pf.uploaded_at DESC`,
            },
            {
                label: 'Files by name prefix',
                sql: `SELECT file_id, filename, status, uploaded_at\nFROM parsed_files\nWHERE filename LIKE 'XXX%'\nORDER BY uploaded_at DESC`,
            },
            {
                label: 'Excel files only',
                sql: `SELECT file_id, filename, status, uploaded_at\nFROM parsed_files\nWHERE mime_type = 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'\nORDER BY uploaded_at DESC`,
            },
            {
                label: 'PDF files only',
                sql: `SELECT file_id, filename, status, uploaded_at\nFROM parsed_files\nWHERE mime_type = 'application/pdf'\nORDER BY uploaded_at DESC`,
            },
        ],
    },
    {
        group: 'Aggregations (Charts)',
        snippets: [
            {
                label: 'OPEX by category (bar)',
                sql: `SELECT category AS LabelX, SUM(amount) AS LabelY\nFROM parsed_tables\nGROUP BY category\nORDER BY LabelY DESC`,
            },
            {
                label: 'OPEX by organization (bar)',
                sql: `SELECT org_id AS LabelX, SUM(amount) AS LabelY\nFROM parsed_tables\nGROUP BY org_id\nORDER BY LabelY DESC`,
            },
            {
                label: 'Items by source (stacked bar)',
                sql: `SELECT item AS LabelX,\n  SUM(CASE WHEN source = 'Source1' THEN amount ELSE 0 END) AS Source1,\n  SUM(CASE WHEN source = 'Source2' THEN amount ELSE 0 END) AS Source2,\n  SUM(CASE WHEN source = 'Source3' THEN amount ELSE 0 END) AS Source3\nFROM parsed_tables\nGROUP BY item\nORDER BY item`,
            },
            {
                label: 'Trend over time (line)',
                sql: `SELECT date AS LabelX, SUM(amount) AS LabelY\nFROM parsed_tables\nGROUP BY date\nORDER BY date`,
            },
            {
                label: 'Status distribution (pie)',
                sql: `SELECT status AS LabelX, COUNT(*) AS LabelY\nFROM parsed_files\nGROUP BY status`,
            },
            {
                label: 'File types count (pie)',
                sql: `SELECT mime_type AS LabelX, COUNT(*) AS LabelY\nFROM parsed_files\nGROUP BY mime_type`,
            },
        ],
    },
    {
        group: 'Tables & Details',
        snippets: [
            {
                label: 'Parsed table data',
                sql: `SELECT *\nFROM parsed_tables\nORDER BY id DESC\nLIMIT 100`,
            },
            {
                label: 'Processing errors',
                sql: `SELECT file_id, filename, status, error_message, uploaded_at\nFROM parsed_files\nWHERE status = 'FAILED'\nORDER BY uploaded_at DESC`,
            },
            {
                label: 'Summary by period & org',
                sql: `SELECT period_id, org_id, COUNT(*) AS file_count, SUM(amount) AS total_amount\nFROM parsed_tables\nGROUP BY period_id, org_id\nORDER BY period_id, org_id`,
            },
        ],
    },
];

interface DashboardSqlEditorProps {
    onInsertQuery?: (query: string) => void;
}

export default function DashboardSqlEditor({ onInsertQuery }: DashboardSqlEditorProps) {
    const styles = useStyles();
    const [sql, setSql] = useState('');
    const [results, setResults] = useState<{ columns: string[]; rows: unknown[][] } | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [isExecuting, setIsExecuting] = useState(false);
    const [showSnippets, setShowSnippets] = useState(false);

    const insertSnippet = (snippet: string) => {
        const newSql = sql ? `${sql}\n\n${snippet}` : snippet;
        setSql(newSql);
    };

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
            <div className={styles.actions}>
                <Button
                    appearance={showSnippets ? 'primary' : 'secondary'}
                    icon={<Add24Regular />}
                    onClick={() => setShowSnippets(!showSnippets)}
                    size="small"
                >
                    {showSnippets ? 'Hide Snippets' : 'SQL Snippets'}
                </Button>
            </div>

            {showSnippets && (
                <div className={styles.snippetBar}>
                    <Text className={styles.snippetLabel}>
                        Click a snippet to insert it into the editor:
                    </Text>
                    {SQL_SNIPPET_GROUPS.map((group) => (
                        <div key={group.group} className={styles.snippetGroup}>
                            <Text className={styles.snippetGroupLabel}>{group.group}</Text>
                            {group.snippets.map((snippet) => (
                                <Button
                                    key={snippet.label}
                                    size="small"
                                    appearance="secondary"
                                    onClick={() => insertSnippet(snippet.sql)}
                                    title={snippet.sql}
                                >
                                    {snippet.label}
                                </Button>
                            ))}
                        </div>
                    ))}
                </div>
            )}

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
