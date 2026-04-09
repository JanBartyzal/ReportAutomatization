import { useState } from 'react';
import {
    makeStyles,
    tokens,
    Tab,
    TabList,
    Title3,
    Body1,
} from '@fluentui/react-components';
import { FileContentType } from '@reportplatform/types';
import type { FileContent, ExcelSheet, PdfPage, CsvContent } from '@reportplatform/types';
import { UnifiedTableView } from '../UnifiedTableView/UnifiedTableView';

/**
 * FileViewer styles per docs/UX-UI/02-design-system.md
 */
const useStyles = makeStyles({
    container: {
        display: 'flex',
        flexDirection: 'column',
        gap: tokens.spacingHorizontalM,
    },
    header: {
        display: 'flex',
        alignItems: 'center',
        gap: tokens.spacingHorizontalM,
    },
    contentArea: {
        backgroundColor: tokens.colorNeutralBackground2,
        borderRadius: tokens.borderRadiusMedium,
        padding: tokens.spacingHorizontalM,
        minHeight: '300px',
    },
    tabs: {
        marginBottom: tokens.spacingHorizontalM,
    },
    pageNavigation: {
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        gap: tokens.spacingHorizontalM,
        marginTop: tokens.spacingVerticalM,
    },
    pageInfo: {
        textAlign: 'center',
    },
    ocrBadge: {
        display: 'inline-flex',
        alignItems: 'center',
        padding: `${tokens.spacingVerticalXXS} ${tokens.spacingHorizontalS}`,
        borderRadius: tokens.borderRadiusSmall,
        fontSize: tokens.fontSizeBase200,
        fontWeight: '500',
    },
    textContent: {
        whiteSpace: 'pre-wrap',
        fontFamily: tokens.fontFamilyMonospace,
        fontSize: tokens.fontSizeBase200,
        lineHeight: tokens.lineHeightBase200,
        maxHeight: '500px',
        overflowY: 'auto',
    },
    metadata: {
        display: 'flex',
        gap: tokens.spacingHorizontalL,
        flexWrap: 'wrap',
        marginBottom: tokens.spacingVerticalM,
    },
    metadataItem: {
        display: 'flex',
        alignItems: 'center',
        gap: tokens.spacingVerticalXS,
    },
    metadataLabel: {
        color: tokens.colorNeutralForeground2,
        fontSize: tokens.fontSizeBase200,
    },
    metadataValue: {
        color: tokens.colorNeutralForeground1,
        fontSize: tokens.fontSizeBase200,
        fontWeight: '500',
    },
    sheetInfo: {
        marginBottom: tokens.spacingVerticalM,
    },
});

interface FileViewerProps {
    content: FileContent;
}

type ViewTab = 'content' | 'tables';

function getContentTypeLabel(type: FileContentType): string {
    switch (type) {
        case 'EXCEL': return 'Excel Workbook';
        case 'PDF': return 'PDF Document';
        case 'CSV': return 'CSV File';
        case 'PPTX': return 'PowerPoint Presentation';
        default: return 'File Content';
    }
}

/**
 * Excel Viewer - shows sheet tabs and table data
 */
function ExcelViewer({ sheets }: { sheets: ExcelSheet[] }) {
    const styles = useStyles();
    const [selectedSheet, setSelectedSheet] = useState(0);

    if (!sheets || sheets.length === 0) {
        return <Body1>No sheets found in this workbook.</Body1>;
    }

    const currentSheet = sheets[selectedSheet];

    return (
        <div className={styles.container}>
            <div className={styles.tabs}>
                <TabList 
                    selectedValue={String(selectedSheet)} 
                    onTabSelect={(_event: any, data: any) => setSelectedSheet(Number(data.value))}
                >
                    {sheets.map((sheet: ExcelSheet, idx: number) => (
                        <Tab key={idx} value={String(idx)}>
                            {sheet.sheet_name}
                        </Tab>
                    ))}
                </TabList>
            </div>

            <div className={styles.sheetInfo}>
                <Body1>
                    <strong>Sheet:</strong> {currentSheet.sheet_name} |
                    <strong> Rows:</strong> {currentSheet.row_count}
                </Body1>
            </div>

            {currentSheet.headers.length > 0 ? (
                <UnifiedTableView
                    tables={[
                        {
                            table_id: `sheet-${selectedSheet}`,
                            source_type: FileContentType.EXCEL,
                            source_sheet: currentSheet.sheet_name,
                            headers: currentSheet.headers,
                            rows: currentSheet.rows,
                            row_count: currentSheet.row_count,
                            data_types: currentSheet.data_types,
                        },
                    ]}
                    title=""
                />
            ) : (
                <Body1>This sheet is empty.</Body1>
            )}
        </div>
    );
}

/**
 * PDF Viewer - shows page-by-page text with OCR confidence
 */
function PdfViewer({ pages }: { pages: PdfPage[] }) {
    const styles = useStyles();
    const [currentPage, setCurrentPage] = useState(0);

    if (!pages || pages.length === 0) {
        return <Body1>No pages found in this PDF.</Body1>;
    }

    const page = pages[currentPage];

    return (
        <div className={styles.container}>
            <div className={styles.contentArea}>
                <Title3 block>Page {page.page_number}</Title3>

                {page.is_ocr && page.ocr_confidence !== undefined && (
                    <div style={{ marginTop: tokens.spacingVerticalS, marginBottom: tokens.spacingVerticalS }}>
                        <Body1
                            className={styles.ocrBadge}
                            style={{
                                backgroundColor: page.ocr_confidence >= 0.8 ? 'var(--colorSuccessBackground)' : 'var(--colorWarningBackground)',
                                color: page.ocr_confidence >= 0.8 ? 'var(--colorSuccessForeground1)' : 'var(--colorWarningForeground1)',
                            }}
                        >
                            OCR: {Math.round(page.ocr_confidence * 100)}% confidence
                        </Body1>
                    </div>
                )}

                {page.text ? (
                    <div className={styles.textContent}>
                        {page.text}
                    </div>
                ) : (
                    <Body1>No text on this page.</Body1>
                )}
            </div>

            {pages.length > 1 && (
                <div className={styles.pageNavigation}>
                    <Body1>Page {currentPage + 1} of {pages.length}</Body1>
                    <div style={{ display: 'flex', gap: tokens.spacingHorizontalS }}>
                        <Body1
                            style={{ cursor: 'pointer', color: currentPage > 0 ? tokens.colorBrandForeground1 : tokens.colorNeutralForeground3 }}
                            onClick={() => setCurrentPage((p: number) => Math.max(0, p - 1))}
                        >
                            ← Previous
                        </Body1>
                        <Body1
                            style={{ cursor: 'pointer', color: currentPage < pages.length - 1 ? tokens.colorBrandForeground1 : tokens.colorNeutralForeground3 }}
                            onClick={() => setCurrentPage((p: number) => Math.min(pages.length - 1, p + 1))}
                        >
                            Next →
                        </Body1>
                    </div>
                </div>
            )}

            {page.tables && page.tables.length > 0 && (
                <UnifiedTableView tables={page.tables} title="Tables on this page" />
            )}
        </div>
    );
}

/**
 * CSV Viewer - shows formatted table with detected headers
 */
function CsvViewer({ csv }: { csv: CsvContent }) {
    const styles = useStyles();

    if (!csv || csv.headers.length === 0) {
        return <Body1>No data found in this CSV file.</Body1>;
    }

    return (
        <div className={styles.container}>
            <div className={styles.metadata}>
                <div className={styles.metadataItem}>
                    <Body1 className={styles.metadataLabel}>Delimiter:</Body1>
                    <Body1 className={styles.metadataValue}>
                        {csv.delimiter === '\t' ? 'Tab' : csv.delimiter === ';' ? 'Semicolon' : csv.delimiter === ',' ? 'Comma' : csv.delimiter}
                    </Body1>
                </div>
                <div className={styles.metadataItem}>
                    <Body1 className={styles.metadataLabel}>Encoding:</Body1>
                    <Body1 className={styles.metadataValue}>{csv.encoding}</Body1>
                </div>
                <div className={styles.metadataItem}>
                    <Body1 className={styles.metadataLabel}>Rows:</Body1>
                    <Body1 className={styles.metadataValue}>{csv.row_count}</Body1>
                </div>
            </div>

            <UnifiedTableView
                tables={[
                    {
                        table_id: 'csv-main',
                        source_type: FileContentType.CSV,
                        headers: csv.headers,
                        rows: csv.rows,
                        row_count: csv.row_count,
                        data_types: csv.data_types,
                    },
                ]}
                title=""
            />
        </div>
    );
}

/**
 * PPTX Viewer - shows slide-by-slide view with speaker notes
 */
function PptxViewer({ slides }: { slides: any[] }) {
    const styles = useStyles();
    const [currentSlide, setCurrentSlide] = useState(0);

    if (!slides || slides.length === 0) {
        return <Body1>No slides found in this presentation.</Body1>;
    }

    const slide = slides[currentSlide];

    return (
        <div className={styles.container}>
            <div className={styles.contentArea}>
                <Title3 block>Slide {slide.slide_number}</Title3>

                {slide.text ? (
                    <div className={styles.textContent}>
                        {slide.text}
                    </div>
                ) : (
                    <Body1 block>No text on this slide.</Body1>
                )}

                {slide.speaker_notes && (
                    <div style={{ marginTop: tokens.spacingVerticalL }}>
                        <Title3 block>Speaker Notes</Title3>
                        <div style={{ 
                            padding: tokens.spacingHorizontalM, 
                            backgroundColor: tokens.colorNeutralBackground3,
                            borderRadius: tokens.borderRadiusMedium,
                            marginTop: tokens.spacingVerticalS
                        }}>
                            <Body1>{slide.speaker_notes}</Body1>
                        </div>
                    </div>
                )}
            </div>

            {slides.length > 1 && (
                <div className={styles.pageNavigation}>
                    <Body1>Slide {currentSlide + 1} of {slides.length}</Body1>
                    <div style={{ display: 'flex', gap: tokens.spacingHorizontalS }}>
                        <Body1
                            style={{ cursor: 'pointer', color: currentSlide > 0 ? tokens.colorBrandForeground1 : tokens.colorNeutralForeground3 }}
                            onClick={() => setCurrentSlide((s: number) => Math.max(0, s - 1))}
                        >
                            ← Previous
                        </Body1>
                        <Body1
                            style={{ cursor: 'pointer', color: currentSlide < slides.length - 1 ? tokens.colorBrandForeground1 : tokens.colorNeutralForeground3 }}
                            onClick={() => setCurrentSlide((s: number) => Math.min(slides.length - 1, s + 1))}
                        >
                            Next →
                        </Body1>
                    </div>
                </div>
            )}

            {slide.tables && slide.tables.length > 0 && (
                <UnifiedTableView tables={slide.tables} title="Tables on this slide" />
            )}
        </div>
    );
}

/**
 * Main FileViewer component - routes to appropriate viewer based on content type
 */
export function FileViewer({ content }: FileViewerProps) {
    const styles = useStyles();
    const [selectedTab, setSelectedTab] = useState<ViewTab>('content');

    if (!content) {
        return (
            <div className={styles.contentArea}>
                <Body1>No content available.</Body1>
            </div>
        );
    }

    const renderContent = () => {
        switch (content.content_type) {
            case 'EXCEL':
                return content.sheets ? <ExcelViewer sheets={content.sheets} /> : <Body1>No sheets found.</Body1>;
            case 'PDF':
                return content.pages ? <PdfViewer pages={content.pages} /> : <Body1>No pages found.</Body1>;
            case 'CSV':
                return content.csv ? <CsvViewer csv={content.csv} /> : <Body1>No CSV data found.</Body1>;
            case 'PPTX':
                return content.slides ? <PptxViewer slides={content.slides} /> : <Body1>No slides found.</Body1>;
            default:
                return <Body1>Unsupported file type: {content.content_type}</Body1>;
        }
    };

    return (
        <div className={styles.container}>
            <div className={styles.header}>
                <Title3>{getContentTypeLabel(content.content_type)}</Title3>
            </div>

            {content.metadata && (
                <div className={styles.metadata}>
                    {content.metadata.total_pages && (
                        <div className={styles.metadataItem}>
                            <Body1 className={styles.metadataLabel}>Pages:</Body1>
                            <Body1 className={styles.metadataValue}>{content.metadata.total_pages}</Body1>
                        </div>
                    )}
                    {content.metadata.total_sheets && (
                        <div className={styles.metadataItem}>
                            <Body1 className={styles.metadataLabel}>Sheets:</Body1>
                            <Body1 className={styles.metadataValue}>{content.metadata.total_sheets}</Body1>
                        </div>
                    )}
                    {content.metadata.total_rows && (
                        <div className={styles.metadataItem}>
                            <Body1 className={styles.metadataLabel}>Total Rows:</Body1>
                            <Body1 className={styles.metadataValue}>{content.metadata.total_rows}</Body1>
                        </div>
                    )}
                    {content.metadata.has_ocr && (
                        <div className={styles.metadataItem}>
                            <Body1 className={styles.metadataLabel}>OCR:</Body1>
                            <Body1 className={styles.metadataValue}>Yes</Body1>
                            {content.metadata.ocr_languages && (
                                <Body1 className={styles.metadataValue}>({content.metadata.ocr_languages.join(', ')})</Body1>
                            )}
                        </div>
                    )}
                </div>
            )}

            <TabList
                className={styles.tabs}
                selectedValue={selectedTab}
                onTabSelect={(_e: any, d: any) => setSelectedTab(d.value as ViewTab)}
            >
                <Tab value="content">Content</Tab>
                <Tab value="tables">Extracted Tables ({content.tables?.length || 0})</Tab>
            </TabList>

            {selectedTab === 'content' ? (
                renderContent()
            ) : (
                <UnifiedTableView tables={content.tables || []} />
            )}
        </div>
    );
}
