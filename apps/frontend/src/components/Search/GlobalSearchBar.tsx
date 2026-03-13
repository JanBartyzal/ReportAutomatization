import React, { useState, useRef, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
    makeStyles,
    tokens,
    Input,
    Button,
    Spinner,
} from '@fluentui/react-components';
import {
    Search24Regular,
    Dismiss24Regular,
    Document24Regular,
    Folder24Regular,
    Book24Regular,
    Form24Regular,
} from '@fluentui/react-icons';
import { useSearchSuggestions } from '../../hooks/useSearch';

const useStyles = makeStyles({
    container: {
        position: 'relative',
        width: '100%',
        maxWidth: '500px',
    },
    searchWrapper: {
        display: 'flex',
        alignItems: 'center',
        gap: tokens.spacingHorizontalXS,
    },
    inputWrapper: {
        flex: 1,
    },
    suggestions: {
        position: 'absolute',
        top: '100%',
        left: 0,
        right: 0,
        marginTop: '4px',
        backgroundColor: tokens.colorNeutralBackground1,
        border: `1px solid ${tokens.colorNeutralStroke1}`,
        borderRadius: tokens.borderRadiusMedium,
        boxShadow: tokens.shadow28, // Level 3 shadow
        maxHeight: '400px',
        overflowY: 'auto',
        zIndex: 1000,
    },
    suggestionItem: {
        display: 'flex',
        alignItems: 'center',
        gap: tokens.spacingHorizontalS,
        padding: `${tokens.spacingVerticalS} ${tokens.spacingHorizontalM}`,
        cursor: 'pointer',
        transition: 'background-color 0.15s ease-in-out',
        '&:hover': {
            backgroundColor: tokens.colorNeutralBackground1Hover,
        },
    },
    suggestionIcon: {
        flexShrink: 0,
        color: tokens.colorNeutralForeground2,
    },
    suggestionText: {
        flex: 1,
        overflow: 'hidden',
        textOverflow: 'ellipsis',
        whiteSpace: 'nowrap',
    },
    suggestionType: {
        fontSize: tokens.fontSizeBase100,
        color: tokens.colorNeutralForeground2,
    },
    divider: {
        height: '1px',
        backgroundColor: tokens.colorNeutralStroke1,
        margin: `${tokens.spacingVerticalXS} 0`,
    },
    searchButton: {
        flexShrink: 0,
    },
});

const getTypeIcon = (type: string) => {
    switch (type) {
        case 'FILE':
            return <Document24Regular />;
        case 'FOLDER':
            return <Folder24Regular />;
        case 'REPORT':
            return <Book24Regular />;
        case 'FORM':
            return <Form24Regular />;
        default:
            return <Document24Regular />;
    }
};

interface GlobalSearchBarProps {
    onSearch?: (query: string) => void;
}

export default function GlobalSearchBar({ onSearch }: GlobalSearchBarProps) {
    const styles = useStyles();
    const navigate = useNavigate();
    const containerRef = useRef<HTMLDivElement>(null);

    const [query, setQuery] = useState('');
    const [showSuggestions, setShowSuggestions] = useState(false);
    const [selectedIndex, setSelectedIndex] = useState(-1);

    const { data: suggestions, isLoading } = useSearchSuggestions(query, 8);

    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
                setShowSuggestions(false);
            }
        };
        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    const handleKeyDown = (e: React.KeyboardEvent) => {
        if (!suggestions) return;

        if (e.key === 'ArrowDown') {
            e.preventDefault();
            setSelectedIndex(prev => Math.min(prev + 1, suggestions.length - 1));
        } else if (e.key === 'ArrowUp') {
            e.preventDefault();
            setSelectedIndex(prev => Math.max(prev - 1, -1));
        } else if (e.key === 'Enter' && selectedIndex >= 0) {
            e.preventDefault();
            handleSuggestionClick(suggestions[selectedIndex]);
        } else if (e.key === 'Escape') {
            setShowSuggestions(false);
        }
    };

    const handleSearch = () => {
        if (query.trim()) {
            setShowSuggestions(false);
            onSearch?.(query);
            navigate(`/search?q=${encodeURIComponent(query)}`);
        }
    };

    const handleSuggestionClick = (suggestion: { type: string; id: string; text: string }) => {
        setShowSuggestions(false);
        setQuery(suggestion.text);

        // Navigate to the appropriate page based on type
        const routeMap: Record<string, string> = {
            FILE: `/files/${suggestion.id}`,
            REPORT: `/reports/${suggestion.id}`,
            FORM: `/forms/${suggestion.id}`,
            FORM_RESPONSE: `/forms/${suggestion.id}/fill`,
        };

        const route = routeMap[suggestion.type];
        if (route) {
            navigate(route);
        }
    };

    return (
        <div className={styles.container} ref={containerRef}>
            <div className={styles.searchWrapper}>
                <Input
                    className={styles.inputWrapper}
                    placeholder="Search files, reports, forms..."
                    value={query}
                    onChange={(_ev, data) => {
                        setQuery(data.value);
                        setShowSuggestions(true);
                        setSelectedIndex(-1);
                    }}
                    onKeyDown={handleKeyDown}
                    onFocus={() => query.length >= 2 && setShowSuggestions(true)}
                    contentBefore={<Search24Regular />}
                    contentAfter={
                        query && (
                            <Button
                                appearance="subtle"
                                size="small"
                                icon={<Dismiss24Regular />}
                                onClick={() => {
                                    setQuery('');
                                    setShowSuggestions(false);
                                }}
                            />
                        )
                    }
                />
                <Button
                    className={styles.searchButton}
                    appearance="primary"
                    onClick={handleSearch}
                    disabled={!query.trim()}
                >
                    Search
                </Button>
            </div>

            {showSuggestions && query.length >= 2 && (
                <div className={styles.suggestions}>
                    {isLoading ? (
                        <div style={{ padding: '16px', textAlign: 'center' }}>
                            <Spinner size="small" />
                        </div>
                    ) : suggestions && suggestions.length > 0 ? (
                        <>
                            {suggestions.map((suggestion, index) => (
                                <div
                                    key={suggestion.id}
                                    className={styles.suggestionItem}
                                    style={{
                                        backgroundColor: index === selectedIndex
                                            ? tokens.colorNeutralBackground1Hover
                                            : 'transparent',
                                    }}
                                    onClick={() => handleSuggestionClick(suggestion)}
                                >
                                    <span className={styles.suggestionIcon}>
                                        {getTypeIcon(suggestion.type)}
                                    </span>
                                    <span className={styles.suggestionText}>{suggestion.text}</span>
                                    <span className={styles.suggestionType}>{suggestion.type}</span>
                                </div>
                            ))}
                            <div className={styles.divider} />
                            <div
                                className={styles.suggestionItem}
                                onClick={handleSearch}
                            >
                                <Search24Regular />
                                <span>Search for "{query}"</span>
                            </div>
                        </>
                    ) : (
                        <div style={{ padding: '16px', textAlign: 'center', color: tokens.colorNeutralForeground2 }}>
                            No results found
                        </div>
                    )}
                </div>
            )}
        </div>
    );
}
