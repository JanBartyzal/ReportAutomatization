import { useSearchParams, useNavigate } from 'react-router-dom';
import {
    makeStyles,
    tokens,
    Title2,
    Body1,
    Button,
    Dropdown,
    Option,
    Badge,
    DropdownProps,
} from '@fluentui/react-components';
import {
    Search24Regular,
    Document24Regular,
    Folder24Regular,
    Sport24Regular,
    Form24Regular,
} from '@fluentui/react-icons';
import { useSearch } from '../hooks/useSearch';
import type { SearchMode, EntityType } from '../api/search';
import LoadingSpinner from '../components/LoadingSpinner';

const useStyles = makeStyles({
    container: {
        padding: tokens.spacingHorizontalL,
        maxWidth: '1000px',
    },
    header: {
        marginBottom: tokens.spacingHorizontalL,
    },
    searchInfo: {
        display: 'flex',
        alignItems: 'center',
        gap: tokens.spacingHorizontalM,
        marginTop: tokens.spacingVerticalM,
    },
    filters: {
        display: 'flex',
        gap: tokens.spacingHorizontalM,
        marginBottom: tokens.spacingHorizontalL,
        flexWrap: 'wrap',
    },
    filterGroup: {
        display: 'flex',
        alignItems: 'center',
        gap: tokens.spacingHorizontalS,
    },
    resultsList: {
        display: 'flex',
        flexDirection: 'column',
        gap: tokens.spacingVerticalM,
    },
    resultItem: {
        display: 'flex',
        gap: tokens.spacingHorizontalM,
        padding: tokens.spacingHorizontalM,
        border: `1px solid ${tokens.colorNeutralStroke1}`,
        borderRadius: tokens.borderRadiusMedium,
        cursor: 'pointer',
        transition: 'background-color 0.15s ease-in-out',
        '&:hover': {
            backgroundColor: tokens.colorNeutralBackground1Hover,
        },
    },
    resultIcon: {
        flexShrink: 0,
        width: '40px',
        height: '40px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        backgroundColor: tokens.colorNeutralBackground2,
        borderRadius: tokens.borderRadiusMedium,
    },
    resultContent: {
        flex: 1,
        minWidth: 0,
    },
    resultTitle: {
        fontWeight: tokens.fontWeightSemibold,
        fontSize: tokens.fontSizeBase400,
        color: tokens.colorBrandForeground1,
        marginBottom: tokens.spacingVerticalXS,
    },
    resultSnippet: {
        fontSize: tokens.fontSizeBase300,
        color: tokens.colorNeutralForeground2,
        marginBottom: tokens.spacingVerticalXS,
    },
    resultMeta: {
        display: 'flex',
        gap: tokens.spacingHorizontalS,
        fontSize: tokens.fontSizeBase200,
        color: tokens.colorNeutralForeground3,
    },
    resultScore: {
        marginLeft: 'auto',
    },
    emptyState: {
        textAlign: 'center',
        padding: tokens.spacingHorizontalXL,
        color: tokens.colorNeutralForeground2,
    },
    pagination: {
        display: 'flex',
        justifyContent: 'center',
        gap: tokens.spacingHorizontalS,
        marginTop: tokens.spacingHorizontalL,
    },
});

const TYPE_OPTIONS: { value: EntityType | ''; label: string }[] = [
    { value: '', label: 'All Types' },
    { value: 'FILE', label: 'Files' },
    { value: 'REPORT', label: 'Reports' },
    { value: 'FORM', label: 'Forms' },
    { value: 'DOCUMENT', label: 'Documents' },
];

const MODE_OPTIONS: { value: SearchMode; label: string }[] = [
    { value: 'text', label: 'Text Search' },
    { value: 'semantic', label: 'Semantic Search (AI)' },
];

const getTypeIcon = (type: string) => {
    switch (type) {
        case 'FILE':
            return <Document24Regular />;
        case 'FOLDER':
            return <Folder24Regular />;
        case 'REPORT':
            return <Sport24Regular />;
        case 'FORM':
            return <Form24Regular />;
        default:
            return <Document24Regular />;
    }
};

export default function SearchResultsPage() {
    const styles = useStyles();
    const navigate = useNavigate();
    const [searchParams, setSearchParams] = useSearchParams();

    const query = searchParams.get('q') || '';
    const type = (searchParams.get('type') as EntityType) || '';
    const mode = (searchParams.get('mode') as SearchMode) || 'text';
    const page = parseInt(searchParams.get('page') || '1', 10);

    const { data, isLoading } = useSearch({
        q: query,
        type: type || undefined,
        mode,
        page,
        page_size: 20,
    });

    const handleTypeChange: DropdownProps['onOptionSelect'] = (_ev, data) => {
        const newParams = new URLSearchParams(searchParams);
        if (data.optionValue) {
            newParams.set('type', data.optionValue);
        } else {
            newParams.delete('type');
        }
        newParams.set('page', '1');
        setSearchParams(newParams);
    };

    const handleModeChange: DropdownProps['onOptionSelect'] = (_ev, data) => {
        const newParams = new URLSearchParams(searchParams);
        if (data.optionValue) {
            newParams.set('mode', data.optionValue);
            setSearchParams(newParams);
        }
    };

    const handlePageChange = (newPage: number) => {
        const newParams = new URLSearchParams(searchParams);
        newParams.set('page', String(newPage));
        setSearchParams(newParams);
    };

    const handleResultClick = (result: { type: string; id: string }) => {
        const routeMap: Record<string, string> = {
            FILE: `/files/${result.id}`,
            REPORT: `/reports/${result.id}`,
            FORM: `/forms/${result.id}`,
            FORM_RESPONSE: `/forms/${result.id}/fill`,
        };

        const route = routeMap[result.type];
        if (route) {
            navigate(route);
        }
    };

    const groupedResults = data?.data.reduce((acc, result) => {
        if (!acc[result.type]) {
            acc[result.type] = [];
        }
        acc[result.type].push(result);
        return acc;
    }, {} as Record<string, any[]>) || {};

    return (
        <div className={styles.container}>
            <div className={styles.header}>
                <Title2 block>Search Results</Title2>
                <div className={styles.searchInfo}>
                    <Search24Regular />
                    <Body1>"{query}"</Body1>
                    {data && (
                        <Body1 style={{ color: tokens.colorNeutralForeground2 }}>
                            ({data.pagination.total_items} results)
                        </Body1>
                    )}
                </div>
            </div>

            <div className={styles.filters}>
                <div className={styles.filterGroup}>
                    <span>Type:</span>
                    <Dropdown
                        value={TYPE_OPTIONS.find(o => o.value === type)?.label || 'All Types'}
                        onOptionSelect={handleTypeChange}
                    >
                        {TYPE_OPTIONS.map((opt) => (
                            <Option key={opt.value} value={opt.value}>
                                {opt.label}
                            </Option>
                        ))}
                    </Dropdown>
                </div>

                <div className={styles.filterGroup}>
                    <span>Mode:</span>
                    <Dropdown
                        value={MODE_OPTIONS.find(o => o.value === mode)?.label || 'Text Search'}
                        onOptionSelect={handleModeChange}
                    >
                        {MODE_OPTIONS.map((opt) => (
                            <Option key={opt.value} value={opt.value}>
                                {opt.label}
                            </Option>
                        ))}
                    </Dropdown>
                </div>
            </div>

            {isLoading ? (
                <LoadingSpinner label="Searching..." />
            ) : !query ? (
                <div className={styles.emptyState}>
                    <Search24Regular style={{ fontSize: '48px', opacity: 0.5, marginBottom: '16px' }} />
                    <p>Enter a search query to find files, reports, and forms</p>
                </div>
            ) : data?.data.length === 0 ? (
                <div className={styles.emptyState}>
                    <Search24Regular style={{ fontSize: '48px', opacity: 0.5, marginBottom: '16px' }} />
                    <p>No results found for "{query}"</p>
                    <p>Try different keywords or remove filters</p>
                </div>
            ) : (
                <>
                    <div className={styles.resultsList}>
                        {Object.entries(groupedResults).map(([type, results]) => (
                            <div key={type}>
                                <div style={{
                                    display: 'flex',
                                    alignItems: 'center',
                                    gap: '8px',
                                    marginBottom: '8px',
                                    marginTop: '16px'
                                }}>
                                    {getTypeIcon(type)}
                                    <strong>{type}s</strong>
                                    <Badge appearance="filled">{(results as any[]).length}</Badge>
                                </div>
                                {(results as any[]).map((result) => (
                                    <div
                                        key={result.id}
                                        className={styles.resultItem}
                                        onClick={() => handleResultClick(result as any)}
                                    >
                                        <div className={styles.resultIcon}>
                                            {getTypeIcon(result.type)}
                                        </div>
                                        <div className={styles.resultContent}>
                                            <div className={styles.resultTitle}>{result.title}</div>
                                            <div className={styles.resultSnippet}>{result.snippet}</div>
                                            <div className={styles.resultMeta}>
                                                {result.metadata?.org_name && (
                                                    <span>{result.metadata.org_name as string}</span>
                                                )}
                                                {result.metadata?.created_at && (
                                                    <span>{new Date(result.metadata.created_at as string).toLocaleDateString()}</span>
                                                )}
                                            </div>
                                        </div>
                                        <div className={styles.resultScore}>
                                            <Badge appearance="filled" color="brand">
                                                {Math.round(result.score * 100)}%
                                            </Badge>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        ))}
                    </div>

                    {data && data.pagination.total_pages > 1 && (
                        <div className={styles.pagination}>
                            <Button
                                appearance="subtle"
                                disabled={page === 1}
                                onClick={() => handlePageChange(page - 1)}
                            >
                                Previous
                            </Button>
                            <span style={{ alignSelf: 'center' }}>
                                Page {page} of {data.pagination.total_pages}
                            </span>
                            <Button
                                appearance="subtle"
                                disabled={page === data.pagination.total_pages}
                                onClick={() => handlePageChange(page + 1)}
                            >
                                Next
                            </Button>
                        </div>
                    )}
                </>
            )}
        </div>
    );
}
