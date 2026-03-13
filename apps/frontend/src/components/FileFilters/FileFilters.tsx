import {
    makeStyles,
    tokens,
    Button,
    Dropdown,
    Option,
    Body1,
} from '@fluentui/react-components';
import { Dismiss24Regular } from '@fluentui/react-icons';
import { FileStatus, FileListParams } from '@reportplatform/types';

/**
 * FileFilters styles per docs/UX-UI/02-design-system.md
 */
const useStyles = makeStyles({
    container: {
        display: 'flex',
        flexDirection: 'column',
        gap: tokens.spacingVerticalM,
    },
    row: {
        display: 'flex',
        flexWrap: 'wrap',
        gap: tokens.spacingHorizontalM,
        alignItems: 'flex-end',
    },
    field: {
        display: 'flex',
        flexDirection: 'column',
        gap: tokens.spacingVerticalXS,
        minWidth: '150px',
    },
    fieldLabel: {
        color: tokens.colorNeutralForeground2,
        fontSize: tokens.fontSizeBase200,
    },
    activeFilters: {
        display: 'flex',
        flexWrap: 'wrap',
        gap: tokens.spacingVerticalS,
    },
    filterBadge: {
        display: 'inline-flex',
        alignItems: 'center',
        gap: tokens.spacingVerticalXS,
        padding: `${tokens.spacingVerticalXXS} ${tokens.spacingHorizontalS}`,
        borderRadius: tokens.borderRadiusSmall,
        backgroundColor: tokens.colorBrandBackground,
        color: tokens.colorBrandForeground1,
        fontSize: tokens.fontSizeBase200,
    },
    clearButton: {
        padding: tokens.spacingVerticalXXS,
        minWidth: 'auto',
    },
});

const FILE_TYPES = [
    { value: '', label: 'All Types' },
    { value: 'application/pdf', label: 'PDF' },
    { value: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', label: 'Excel (XLSX)' },
    { value: 'application/vnd.ms-excel', label: 'Excel (XLS)' },
    { value: 'text/csv', label: 'CSV' },
    { value: 'application/vnd.openxmlformats-officedocument.presentationml.presentation', label: 'PowerPoint (PPTX)' },
    { value: 'application/vnd.ms-powerpoint', label: 'PowerPoint (PPT)' },
];

const FILE_STATUSES: { value: FileStatus | ''; label: string }[] = [
    { value: '', label: 'All Statuses' },
    { value: FileStatus.UPLOADED, label: 'Uploaded' },
    { value: FileStatus.PROCESSING, label: 'Processing' },
    { value: FileStatus.COMPLETED, label: 'Completed' },
    { value: FileStatus.FAILED, label: 'Failed' },
    { value: FileStatus.PARTIAL, label: 'Partial' },
];

const SORT_OPTIONS = [
    { value: 'uploaded_at', label: 'Upload Date' },
    { value: 'filename', label: 'Filename' },
    { value: 'size_bytes', label: 'Size' },
];

interface FileFiltersProps {
    filters: FileListParams;
    onChange: (filters: FileListParams) => void;
}

export function FileFilters({ filters, onChange }: FileFiltersProps) {
    const styles = useStyles();

    const hasActiveFilters = !!(filters.status || filters.mime_type);

    const clearFilters = () => {
        onChange({
            status: undefined,
            mime_type: filters.mime_type,
            sort_by: filters.sort_by,
            sort_order: filters.sort_order,
        });
    };

    return (
        <div className={styles.container}>
            <div className={styles.row}>
                <div className={styles.field}>
                    <Body1 className={styles.fieldLabel}>File Type</Body1>
                    <Dropdown
                        value={FILE_TYPES.find((t: any) => t.value === filters.mime_type)?.label || 'All Types'}
                        onOptionSelect={(_ev: any, d: any) => onChange({ ...filters, mime_type: d.optionValue as string })}
                    >
                        {FILE_TYPES.map((type: any) => (
                            <Option key={type.value} value={type.value}>
                                {type.label}
                            </Option>
                        ))}
                    </Dropdown>
                </div>

                <div className={styles.field}>
                    <Body1 className={styles.fieldLabel}>Status</Body1>
                    <Dropdown
                        value={FILE_STATUSES.find((s: any) => s.value === filters.status)?.label || 'All Statuses'}
                        onOptionSelect={(_ev: any, d: any) => onChange({ ...filters, status: (d.optionValue as FileStatus) || undefined })}
                    >
                        {FILE_STATUSES.map((status: any) => (
                            <Option key={status.value} value={status.value}>
                                {status.label}
                            </Option>
                        ))}
                    </Dropdown>
                </div>

                <div className={styles.field}>
                    <Body1 className={styles.fieldLabel}>Sort By</Body1>
                    <Dropdown
                        value={SORT_OPTIONS.find((s: any) => s.value === filters.sort_by)?.label || 'Upload Date'}
                        onOptionSelect={(_ev: any, d: any) => onChange({ ...filters, sort_by: d.optionValue as FileListParams['sort_by'] })}
                    >
                        {SORT_OPTIONS.map((option: any) => (
                            <Option key={option.value} value={option.value}>
                                {option.label}
                            </Option>
                        ))}
                    </Dropdown>
                </div>

                <div className={styles.field}>
                    <Body1 className={styles.fieldLabel}>Order</Body1>
                    <Dropdown
                        value={filters.sort_order === 'asc' ? 'Ascending' : 'Descending'}
                        onOptionSelect={(_ev: any, d: any) => onChange({ ...filters, sort_order: d.optionValue === 'asc' ? 'asc' : 'desc' })}
                    >
                        <Option value="desc">Descending</Option>
                        <Option value="asc">Ascending</Option>
                    </Dropdown>
                </div>

                {hasActiveFilters && (
                    <Button
                        appearance="subtle"
                        icon={<Dismiss24Regular />}
                        onClick={clearFilters}
                        className={styles.clearButton}
                    >
                        Clear Filters
                    </Button>
                )}
            </div>

            {hasActiveFilters && (
                <div className={styles.activeFilters}>
                    {filters.status && (
                        <Body1 className={styles.filterBadge}>
                            Status: {FILE_STATUSES.find((s: any) => s.value === filters.status)?.label}
                        </Body1>
                    )}
                    {filters.mime_type && (
                        <Body1 className={styles.filterBadge}>
                            Type: {FILE_TYPES.find((t: any) => t.value === filters.mime_type)?.label}
                        </Body1>
                    )}
                </div>
            )}
        </div>
    );
}
