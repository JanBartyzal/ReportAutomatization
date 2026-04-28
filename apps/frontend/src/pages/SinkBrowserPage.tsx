import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  makeStyles,
  tokens,
  Badge,
  Button,
  Input,
  Divider,
  type TableColumnDefinition,
  createTableColumn,
  TableCellLayout,
} from '@fluentui/react-components';
import { Search24Regular, DocumentRegular } from '@fluentui/react-icons';
import { useSinks } from '../hooks/useSinks';
import { useDataSourceStats } from '../hooks/useStorageRouting';
import LoadingSpinner from '../components/LoadingSpinner';
import { DataTable } from '../components/shared/DataTable';
import { PageHeader } from '../components/shared/PageHeader';
import { DataSourceSwitcher } from '../components/DataSource/DataSourceSwitcher';
import type { DataSourceMode, SinkListFilters, SinkListItem, StorageBackend } from '@reportplatform/types';

const BACKEND_BADGE_COLOR: Record<StorageBackend, 'brand' | 'warning' | 'informative'> = {
  POSTGRES: 'brand',
  SPARK: 'warning',
  BLOB: 'informative',
};

const useStyles = makeStyles({
  container: {
    padding: tokens.spacingHorizontalL,
  },
  filters: {
    display: 'flex',
    gap: tokens.spacingHorizontalM,
    marginBottom: tokens.spacingVerticalM,
    flexWrap: 'wrap',
    alignItems: 'center',
  },
  filterGroup: {
    display: 'flex',
    flexDirection: 'column',
    gap: tokens.spacingVerticalXS,
  },
  filterLabel: {
    fontSize: tokens.fontSizeBase200,
    color: tokens.colorNeutralForeground3,
    fontWeight: tokens.fontWeightSemibold,
  },
  sourceSwitcherRow: {
    marginBottom: tokens.spacingVerticalM,
  },
  correctionBadge: {
    cursor: 'default',
  },
  filenameCell: {
    display: 'flex',
    alignItems: 'center',
    gap: tokens.spacingHorizontalXS,
    color: tokens.colorNeutralForeground3,
    fontSize: tokens.fontSizeBase200,
  },
  pagination: {
    marginTop: tokens.spacingVerticalM,
    display: 'flex',
    gap: tokens.spacingHorizontalM,
    alignItems: 'center',
    color: tokens.colorNeutralForeground3,
  },
  dimText: {
    color: tokens.colorNeutralForeground4,
  },
});

export default function SinkBrowserPage() {
  const styles = useStyles();
  const navigate = useNavigate();
  const [filters, setFilters] = useState<SinkListFilters>({ page: 0, size: 20 });
  const [dataSourceMode, setDataSourceMode] = useState<DataSourceMode>('ALL');

  // Effective filter sent to API: omit storage_backend when 'ALL' is selected
  const effectiveFilters: SinkListFilters = {
    ...filters,
    storage_backend: dataSourceMode !== 'ALL' ? dataSourceMode : undefined,
  };

  const { data, isLoading } = useSinks(effectiveFilters);
  const { data: stats, isLoading: loadingStats } = useDataSourceStats();
  const sinks = data?.sinks ?? [];

  const handleModeChange = (mode: DataSourceMode) => {
    setDataSourceMode(mode);
    setFilters((f) => ({ ...f, page: 0 }));
  };

  const columns: TableColumnDefinition<SinkListItem>[] = [
    createTableColumn<SinkListItem>({
      columnId: 'sourceSheet',
      renderHeaderCell: () => 'Sheet / Name',
      renderCell: (item) => (
        <TableCellLayout>
          <Button
            appearance="transparent"
            size="small"
            onClick={() => navigate(`/sinks/${item.id}`)}
          >
            {item.sourceSheet || `Sink ${item.id.substring(0, 8)}`}
          </Button>
        </TableCellLayout>
      ),
    }),
    createTableColumn<SinkListItem>({
      columnId: 'filename',
      renderHeaderCell: () => 'File (Batch)',
      renderCell: (item) => (
        <TableCellLayout>
          <span className={styles.filenameCell}>
            <DocumentRegular fontSize={14} />
            {item.filename}
          </span>
        </TableCellLayout>
      ),
    }),
    createTableColumn<SinkListItem>({
      columnId: 'backend',
      renderHeaderCell: () => 'Storage',
      renderCell: (item) => (
        <TableCellLayout>
          {item.storageBackend ? (
            <Badge
              appearance="outline"
              color={BACKEND_BADGE_COLOR[item.storageBackend] ?? 'subtle'}
              size="small"
            >
              {item.storageBackend}
            </Badge>
          ) : (
            <span className={styles.dimText}>POSTGRES</span>
          )}
        </TableCellLayout>
      ),
    }),
    createTableColumn<SinkListItem>({
      columnId: 'dimensions',
      renderHeaderCell: () => 'Size',
      renderCell: (item) => (
        <TableCellLayout>
          {item.rowCount} × {item.columnCount}
        </TableCellLayout>
      ),
    }),
    createTableColumn<SinkListItem>({
      columnId: 'corrections',
      renderHeaderCell: () => 'Corrections',
      renderCell: (item) => (
        <TableCellLayout>
          {item.correctionCount > 0 ? (
            <Badge appearance="filled" color="warning" className={styles.correctionBadge}>
              {item.correctionCount}
            </Badge>
          ) : (
            <span className={styles.dimText}>—</span>
          )}
        </TableCellLayout>
      ),
    }),
    createTableColumn<SinkListItem>({
      columnId: 'selected',
      renderHeaderCell: () => 'Selected',
      renderCell: (item) => (
        <TableCellLayout>
          {item.hasSelections ? (
            <Badge appearance="filled" color="success">Yes</Badge>
          ) : (
            <span className={styles.dimText}>—</span>
          )}
        </TableCellLayout>
      ),
    }),
    createTableColumn<SinkListItem>({
      columnId: 'createdAt',
      renderHeaderCell: () => 'Created',
      renderCell: (item) => (
        <TableCellLayout>
          {new Date(item.createdAt).toLocaleDateString()}
        </TableCellLayout>
      ),
    }),
  ];

  if (isLoading) return <LoadingSpinner />;

  return (
    <div className={styles.container}>
      <PageHeader title="Sink Browser" />

      {/* Data source switcher */}
      <div className={styles.sourceSwitcherRow}>
        <DataSourceSwitcher
          value={dataSourceMode}
          onChange={handleModeChange}
          stats={stats}
          loadingStats={loadingStats}
        />
      </div>

      <Divider style={{ marginBottom: tokens.spacingVerticalM }} />

      <div className={styles.filters}>
        {/* Search by sheet name */}
        <div className={styles.filterGroup}>
          <span className={styles.filterLabel}>Search by name</span>
          <Input
            placeholder="Sheet name..."
            contentBefore={<Search24Regular />}
            style={{ minWidth: 220 }}
            value={(filters.search as string) ?? ''}
            onChange={(_, d) =>
              setFilters((f) => ({ ...f, search: d.value || undefined, page: 0 }))
            }
          />
        </div>

        {/* Filter by batch (file) */}
        <div className={styles.filterGroup}>
          <span className={styles.filterLabel}>Batch (File ID)</span>
          <Input
            placeholder="Paste file ID..."
            style={{ minWidth: 220 }}
            value={(filters.file_id as string) ?? ''}
            onChange={(_, d) =>
              setFilters((f) => ({ ...f, file_id: d.value || undefined, page: 0 }))
            }
          />
        </div>

        {/* Clear filters */}
        {(filters.search || filters.file_id) && (
          <Button
            appearance="subtle"
            size="small"
            style={{ alignSelf: 'flex-end', marginBottom: 2 }}
            onClick={() => setFilters({ page: 0, size: 20 })}
          >
            Clear filters
          </Button>
        )}
      </div>

      <DataTable
        items={sinks}
        columns={columns}
        getRowId={(item: SinkListItem) => item.id}
      />

      {data && (
        <div className={styles.pagination}>
          <Button
            size="small"
            appearance="subtle"
            disabled={!filters.page || filters.page === 0}
            onClick={() => setFilters((f) => ({ ...f, page: Math.max(0, (f.page ?? 0) - 1) }))}
          >
            ← Prev
          </Button>
          <span>
            Page {(filters.page ?? 0) + 1} of {data.totalPages} &nbsp;·&nbsp; {data.totalElements} sinks
          </span>
          <Button
            size="small"
            appearance="subtle"
            disabled={(filters.page ?? 0) + 1 >= data.totalPages}
            onClick={() => setFilters((f) => ({ ...f, page: (f.page ?? 0) + 1 }))}
          >
            Next →
          </Button>
        </div>
      )}
    </div>
  );
}
