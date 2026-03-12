import React from 'react';
import {
    tokens,
    makeStyles,
    Title3,
    Body1,
    Table,
    TableHeader,
    TableHeaderCell,
    TableRow,
    TableBody,
    TableCell,
} from '@fluentui/react-components';
import type { ColumnDataType, ExtractedTable, FileContentType } from '@reportplatform/types';

/**
 * UnifiedTableView styles per docs/UX-UI/02-design-system.md section 10.1
 * - Alternating rows (Background1 / Background2)
 * - Header: Background2, Title 3 weight 600
 * - Row height: 40px minimum
 * - Cell padding: spacingS horizontal, spacingXS vertical
 */
const useStyles = makeStyles({
    container: {
        display: 'flex',
        flexDirection: 'column',
        gap: tokens.spacingHorizontalL,
    },
    tableSection: {
        marginBottom: tokens.spacingHorizontalL,
    },
    tableHeader: {
        backgroundColor: tokens.colorNeutralBackground2,
    },
    headerCell: {
        fontWeight: '600',
        textTransform: 'none',
    },
    row: {
        minHeight: '40px',
    },
    rowEven: {
        backgroundColor: tokens.colorNeutralBackground1,
    },
    rowOdd: {
        backgroundColor: tokens.colorNeutralBackground2,
    },
    cell: {
        padding: `${tokens.spacingVerticalXS} ${tokens.spacingHorizontalS}`,
    },
    sourceLabel: {
        display: 'inline-flex',
        alignItems: 'center',
        padding: `${tokens.spacingVerticalXXS} ${tokens.spacingHorizontalS}`,
        borderRadius: tokens.borderRadiusSmall,
        fontSize: tokens.fontSizeBody2,
        fontWeight: '500',
        marginBottom: tokens.spacingVerticalS,
    },
    typeBadge: {
        fontSize: tokens.fontSizeCaption1,
        color: tokens.colorNeutralForeground4,
        fontWeight: 'normal',
        marginLeft: tokens.spacingHorizontalXS,
        fontFamily: tokens.fontFamilyMonospace,
    },
    numericCell: {
        textAlign: 'right',
        fontFamily: tokens.fontFamilyMonospace,
    },
    tableWrapper: {
        overflowX: 'auto',
        borderRadius: tokens.borderRadiusMedium,
        border: `1px solid ${tokens.colorNeutralStroke1}`,
    },
});

interface UnifiedTableViewProps {
    tables: ExtractedTable[];
    title?: string;
}

function getSourceTypeColor(sourceType: FileContentType): { bg: string; text: string } {
    switch (sourceType) {
        case 'EXCEL':
            return { bg: 'var(--chart-2)', text: '#fff' };
        case 'PDF':
            return { bg: 'var(--colorSemanticError)', text: '#fff' };
        case 'CSV':
            return { bg: 'var(--chart-3)', text: '#fff' };
        case 'PPTX':
            return { bg: 'var(--chart-4)', text: '#fff' };
        default:
            return { bg: tokens.colorNeutralBackground3, text: tokens.colorNeutralForeground1 };
    }
}

function getSourceLabel(sourceType: FileContentType, page?: number, sheet?: string): string {
    const source = sourceType.charAt(0) + sourceType.slice(1).toLowerCase();
    if (page) return `${source} - Page ${page}`;
    if (sheet) return `${source} - Sheet "${sheet}"`;
    return source;
}

export function UnifiedTableView({ tables, title = 'Extracted Tables' }: UnifiedTableViewProps) {
    const styles = useStyles();

    if (!tables || tables.length === 0) {
        return (
            <div className={styles.container}>
                <Title3>{title}</Title3>
                <Body1>No tables extracted from this file.</Body1>
            </div>
        );
    }

    return (
        <div className={styles.container}>
            <Title3>{title}</Title3>
            {tables.map((table: ExtractedTable, _index: number) => {
                const sourceColors = getSourceTypeColor(table.source_type);
                const hasData = table.headers.length > 0 && table.rows.length > 0;

                return (
                    <div key={table.table_id} className={styles.tableSection}>
                        <div
                            className={styles.sourceLabel}
                            style={{ backgroundColor: sourceColors.bg, color: sourceColors.text }}
                        >
                            {getSourceLabel(table.source_type, table.source_page, table.source_sheet)}
                        </div>
                        {hasData ? (
                            <div className={styles.tableWrapper}>
                                <Table>
                                    <TableHeader className={styles.tableHeader}>
                                        <TableRow>
                                            {table.headers.map((header, idx) => {
                                                const dataType = (table.data_types as ColumnDataType[])?.find((dt: ColumnDataType) => dt.column_name === header)?.detected_type;
                                                const isNumeric = dataType && ['NUMBER', 'CURRENCY', 'PERCENTAGE'].includes(dataType);
                                                
                                                return (
                                                    <TableHeaderCell 
                                                        key={idx} 
                                                        className={`${styles.headerCell} ${isNumeric ? styles.numericCell : ''}`}
                                                    >
                                                        {header}
                                                        {dataType && (
                                                            <Body1 className={styles.typeBadge}>[{dataType}]</Body1>
                                                        )}
                                                    </TableHeaderCell>
                                                );
                                            })}
                                        </TableRow>
                                    </TableHeader>
                                    <TableBody>
                                        {table.rows.map((row: any[], rowIdx: number) => (
                                            <TableRow
                                                key={rowIdx}
                                                className={`${styles.row} ${rowIdx % 2 === 0 ? styles.rowEven : styles.rowOdd}`}
                                            >
                                                {table.headers.map((header: string, colIdx: number) => {
                                                    const dataType = (table.data_types as ColumnDataType[])?.find((dt: ColumnDataType) => dt.column_name === header)?.detected_type;
                                                    const isNumeric = dataType && ['NUMBER', 'CURRENCY', 'PERCENTAGE'].includes(dataType);
                                                    
                                                    return (
                                                        <TableCell 
                                                            key={colIdx} 
                                                            className={`${styles.cell} ${isNumeric ? styles.numericCell : ''}`}
                                                        >
                                                            {String(row[colIdx] ?? '')}
                                                        </TableCell>
                                                    );
                                                })}
                                            </TableRow>
                                        ))}
                                    </TableBody>
                                </Table>
                            </div>
                        ) : (
                            <Body1>No data in this table.</Body1>
                        )}
                        <Body1 style={{ marginTop: tokens.spacingVerticalXS, color: tokens.colorNeutralForeground2 }}>
                            {table.row_count} rows
                        </Body1>
                    </div>
                );
            })}
        </div>
    );
}
