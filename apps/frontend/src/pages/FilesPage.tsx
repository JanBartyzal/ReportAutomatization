import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
    Button,
    makeStyles,
    tokens,
    Checkbox,
    type TableColumnDefinition,
    createTableColumn,
    TableCellLayout,
    Badge
} from '@fluentui/react-components';
import { ArrowUpload24Regular } from '@fluentui/react-icons';
import { useFiles } from '../hooks/useFiles';
import LoadingSpinner from '../components/LoadingSpinner';
import { StatusBadge } from '../components/shared/StatusBadge';
import { DataTable } from '../components/shared/DataTable';
import { PageHeader } from '../components/shared/PageHeader';
import { FileTypeIcon, getFileExtension } from '../components/FileTypeIcon/FileTypeIcon';
import { FileFilters } from '../components/FileFilters/FileFilters';
import type { FileListParams } from '@reportplatform/types';

/**
 * FilesPage — migrated to shared DataTable + StatusBadge per P9-W2-001
 */
const useStyles = makeStyles({
    container: {
        padding: tokens.spacingHorizontalL,
    },
    filtersSection: {
        marginBottom: tokens.spacingHorizontalL,
    },
    filenameCell: {
        display: 'flex',
        alignItems: 'center',
        gap: tokens.spacingHorizontalS,
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

interface FileRow {
    file_id: string;
    filename: string;
    mime_type: string;
    size_bytes: number;
    status: string;
    uploaded_at: string;
}

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

    const files: FileRow[] = filesResponse?.data || [];

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
    };

    const columns: TableColumnDefinition<FileRow>[] = [
        createTableColumn<FileRow>({
            columnId: 'select',
            renderHeaderCell: () => (
                <Checkbox
                    checked={selectedFiles.size === files.length && files.length > 0
                        ? true
                        : selectedFiles.size > 0 ? 'mixed' : false}
                    onChange={toggleAllSelection}
                />
            ),
            renderCell: (file) => (
                <Checkbox
                    checked={selectedFiles.has(file.file_id)}
                    onChange={(e) => { e.stopPropagation(); toggleFileSelection(file.file_id); }}
                />
            ),
        }),
        createTableColumn<FileRow>({
            columnId: 'filename',
            compare: (a, b) => a.filename.localeCompare(b.filename),
            renderHeaderCell: () => 'File',
            renderCell: (file) => (
                <TableCellLayout>
                    <div className={styles.filenameCell}>
                        <FileTypeIcon mimeType={file.mime_type} />
                        <span
                            style={{ cursor: 'pointer', color: tokens.colorBrandForeground1 }}
                            onClick={() => navigate(`/files/${file.file_id}`)}
                        >
                            {file.filename}
                        </span>
                        <Badge appearance="outline" size="small">
                            {getFileExtension(file.mime_type)}
                        </Badge>
                    </div>
                </TableCellLayout>
            ),
        }),
        createTableColumn<FileRow>({
            columnId: 'size',
            renderHeaderCell: () => 'Size',
            renderCell: (file) => <TableCellLayout>{formatFileSize(file.size_bytes)}</TableCellLayout>,
        }),
        createTableColumn<FileRow>({
            columnId: 'status',
            renderHeaderCell: () => 'Status',
            renderCell: (file) => (
                <TableCellLayout>
                    <StatusBadge status={file.status} />
                </TableCellLayout>
            ),
        }),
        createTableColumn<FileRow>({
            columnId: 'uploaded_at',
            compare: (a, b) => a.uploaded_at.localeCompare(b.uploaded_at),
            renderHeaderCell: () => 'Uploaded',
            renderCell: (file) => (
                <TableCellLayout>{new Date(file.uploaded_at).toLocaleDateString()}</TableCellLayout>
            ),
        }),
    ];

    return (
        <div className={styles.container}>
            <PageHeader
                title="Files"
                actions={
                    <Button
                        appearance="primary"
                        icon={<ArrowUpload24Regular />}
                        onClick={() => navigate('/upload')}
                    >
                        Upload
                    </Button>
                }
            />

            <div className={styles.filtersSection}>
                <FileFilters filters={filters} onChange={setFilters} />
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

            <DataTable
                items={files}
                columns={columns}
                sortable
                getRowId={(file) => file.file_id}
                emptyMessage="No files found. Upload a file to get started."
            />
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
