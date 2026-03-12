import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
    Title2,
    Table,
    TableHeader,
    TableRow,
    TableHeaderCell,
    TableBody,
    TableCell,
    TableSelectionCell,
    TableCellLayout,
    Button,
    makeStyles,
    tokens,
    Badge,
    Checkbox,
} from '@fluentui/react-components';
import { ArrowUpload24Regular } from '@fluentui/react-icons';
import { useFiles } from '../hooks/useFiles';
import LoadingSpinner from '../components/LoadingSpinner';
import { getStatusColors } from '../theme/statusColors';
import { FileTypeIcon, getFileExtension } from '../components/FileTypeIcon/FileTypeIcon';
import { FileFilters } from '../components/FileFilters/FileFilters';
import type { FileStatus, FileListParams } from '@reportplatform/types';

/**
 * FilesPage styles per docs/UX-UI/02-design-system.md section 10.1 (DataGrid)
 * - Alternating rows (Background1 / Background2)
 * - Header: Background2, Title 3 weight 600
 * - Row height: 40px minimum
 * - Cell padding: spacingS horizontal, spacingXS vertical
 */
const useStyles = makeStyles({
    container: {
        padding: tokens.spacingHorizontalL,
    },
    header: {
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginBottom: tokens.spacingHorizontalL,
    },
    filtersSection: {
        marginBottom: tokens.spacingHorizontalL,
    },
    table: {
        marginTop: tokens.spacingHorizontalM,
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
        cursor: 'pointer',
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
    badge: {
        display: 'inline-flex',
        alignItems: 'center',
        padding: `${tokens.spacingVerticalXXS} ${tokens.spacingHorizontalS}`,
        borderRadius: tokens.borderRadiusSmall,
        fontSize: tokens.fontSizeBody2,
        fontWeight: '500',
    },
    filenameCell: {
        display: 'flex',
        alignItems: 'center',
        gap: tokens.spacingHorizontalS,
    },
    selectionCell: {
        width: '40px',
    },
    batchActions: {
        display: 'flex',
        gap: tokens.spacingHorizontalM,
        marginBottom: tokens.spacingHorizontalM,
        padding: tokens.spacingHorizontalM,
        backgroundColor: tokens.colorNeutralBackground3,
        borderRadius: tokens.borderRadiusMedium,
    },
});

export default function FilesPage() {
    const styles = useStyles();
    const navigate = useNavigate();

    const [filters, setFilters] = useState<FileListParams>({
        sort_by: 'uploaded_at',
        sort_order: 'desc',
    });
    const [selectedFiles, setSelectedFiles] = useState<Set<string>>(new Set());

    const { data: filesResponse, isLoading } = useFiles(filters);

    if (isLoading) {
        return <LoadingSpinner label="Loading files..." />;
    }

    const files = filesResponse?.items || [];

    const handleFilterChange = (newFilters: FileListParams) => {
        setFilters(newFilters);
    };

    const toggleFileSelection = (fileId: string) => {
        const newSelection = new Set(selectedFiles);
        if (newSelection.has(fileId)) {
            newSelection.delete(fileId);
        } else {
            newSelection.add(fileId);
        }
        setSelectedFiles(newSelection);
    };

    const toggleAllSelection = () => {
        if (selectedFiles.size === files.length) {
            setSelectedFiles(new Set());
        } else {
            setSelectedFiles(new Set(files.map(f => f.file_id)));
        }
    };

    const handleBatchAction = (action: string) => {
        console.log(`Batch action: ${action}`, Array.from(selectedFiles));
        // TODO: Implement batch actions (delete, reprocess, etc.)
    };

    const handleRowClick = (fileId: string) => {
        navigate(`/files/${fileId}`);
    };

    return (
        <div className={styles.container}>
            <div className={styles.header}>
                <Title2>Files</Title2>
                <Button
                    appearance="primary"
                    icon={<ArrowUpload24Regular />}
                    onClick={() => navigate('/upload')}
                >
                    Upload
                </Button>
            </div>

            <div className={styles.filtersSection}>
                <FileFilters filters={filters} onChange={handleFilterChange} />
            </div>

            {selectedFiles.size > 0 && (
                <div className={styles.batchActions}>
                    <strong>{selectedFiles.size} selected</strong>
                    <Button appearance="subtle" onClick={() => handleBatchAction('delete')}>
                        Delete Selected
                    </Button>
                    <Button appearance="subtle" onClick={() => handleBatchAction('reprocess')}>
                        Reprocess
                    </Button>
                    <Button appearance="subtle" onClick={() => setSelectedFiles(new Set())}>
                        Clear Selection
                    </Button>
                </div>
            )}

            <Table className={styles.table}>
                <TableHeader className={styles.tableHeader}>
                    <TableRow>
                        <TableSelectionCell
                            checkboxIndicator={{ onClick: toggleAllSelection }}
                            checked={selectedFiles.size === files.length && files.length > 0}
                            intermediate={selectedFiles.size > 0 && selectedFiles.size < files.length}
                        />
                        <TableHeaderCell className={styles.headerCell}>File</TableHeaderCell>
                        <TableHeaderCell className={styles.headerCell}>Size</TableHeaderCell>
                        <TableHeaderCell className={styles.headerCell}>Status</TableHeaderCell>
                        <TableHeaderCell className={styles.headerCell}>Uploaded</TableHeaderCell>
                    </TableRow>
                </TableHeader>
                <TableBody>
                    {files.length === 0 ? (
                        <TableRow>
                            <TableCell colSpan={5}>No files found. Upload a file to get started.</TableCell>
                        </TableRow>
                    ) : (
                        files.map((file, index) => {
                            const statusColors = getStatusColors(file.status);
                            return (
                                <TableRow
                                    key={file.file_id}
                                    className={`${styles.row} ${index % 2 === 0 ? styles.rowEven : styles.rowOdd}`}
                                >
                                    <TableSelectionCell
                                        checkboxIndicator={{
                                            onClick: (e) => {
                                                e.stopPropagation();
                                                toggleFileSelection(file.file_id);
                                            },
                                        }}
                                        checked={selectedFiles.has(file.file_id)}
                                    />
                                    <TableCell
                                        className={styles.cell}
                                        onClick={() => handleRowClick(file.file_id)}
                                    >
                                        <div className={styles.filenameCell}>
                                            <FileTypeIcon mimeType={file.mime_type} />
                                            <span>{file.filename}</span>
                                            <Badge appearance="outline" size="small">
                                                {getFileExtension(file.mime_type)}
                                            </Badge>
                                        </div>
                                    </TableCell>
                                    <TableCell className={styles.cell}>{formatFileSize(file.size_bytes)}</TableCell>
                                    <TableCell className={styles.cell}>
                                        <Badge
                                            appearance="filled"
                                            color={statusColors.color as any}
                                        >
                                            {file.status}
                                        </Badge>
                                    </TableCell>
                                    <TableCell className={styles.cell}>
                                        {new Date(file.uploaded_at).toLocaleDateString()}
                                    </TableCell>
                                </TableRow>
                            );
                        })
                    )}
                </TableBody>
            </Table>
        </div>
    );
}

function formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return `${parseFloat((bytes / Math.pow(k, i)).toFixed(1))} ${sizes[i]}`;
}
