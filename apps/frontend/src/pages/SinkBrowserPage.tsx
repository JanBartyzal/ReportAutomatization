import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  makeStyles,
  tokens,
  Badge,
  Button,
  Input,
  type TableColumnDefinition,
  createTableColumn,
  TableCellLayout,
} from '@fluentui/react-components';
import { Search24Regular } from '@fluentui/react-icons';
import { useSinks } from '../hooks/useSinks';
import LoadingSpinner from '../components/LoadingSpinner';
import { DataTable } from '../components/shared/DataTable';
import { PageHeader } from '../components/shared/PageHeader';
import type { SinkListFilters, SinkListItem } from '@reportplatform/types';

const useStyles = makeStyles({
  container: {
    padding: tokens.spacingHorizontalL,
  },
  filters: {
    display: 'flex',
    gap: tokens.spacingHorizontalM,
    marginBottom: tokens.spacingVerticalL,
    flexWrap: 'wrap',
  },
  correctionBadge: {
    cursor: 'default',
  },
});

export default function SinkBrowserPage() {
  const styles = useStyles();
  const navigate = useNavigate();
  const [filters, setFilters] = useState<SinkListFilters>({ page: 0, size: 20 });

  const { data, isLoading } = useSinks(filters);
  const sinks = data?.sinks ?? [];

  const columns: TableColumnDefinition<SinkListItem>[] = [
    createTableColumn<SinkListItem>({
      columnId: 'sourceSheet',
      renderHeaderCell: () => 'Source / Sheet',
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
      columnId: 'fileId',
      renderHeaderCell: () => 'File ID',
      renderCell: (item) => (
        <TableCellLayout truncate>
          {item.fileId.substring(0, 8)}...
        </TableCellLayout>
      ),
    }),
    createTableColumn<SinkListItem>({
      columnId: 'dimensions',
      renderHeaderCell: () => 'Size',
      renderCell: (item) => (
        <TableCellLayout>
          {item.rowCount} rows × {item.columnCount} cols
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
            <span>—</span>
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
            <span>—</span>
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

      <div className={styles.filters}>
        <Input
          placeholder="Filter by file ID..."
          contentBefore={<Search24Regular />}
          onChange={(_, d) => setFilters((f) => ({ ...f, file_id: d.value || undefined, page: 0 }))}
        />
        <Input
          placeholder="Filter by sheet name..."
          contentBefore={<Search24Regular />}
          onChange={(_, d) => setFilters((f) => ({ ...f, source_sheet: d.value || undefined, page: 0 }))}
        />
      </div>

      <DataTable
        items={sinks}
        columns={columns}
        getRowId={(item: SinkListItem) => item.id}
      />

      {data && (
        <div style={{ marginTop: tokens.spacingVerticalM, color: tokens.colorNeutralForeground3 }}>
          Page {data.page + 1} of {data.totalPages} ({data.totalElements} total sinks)
        </div>
      )}
    </div>
  );
}
