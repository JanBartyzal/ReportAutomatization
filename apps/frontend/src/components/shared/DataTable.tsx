/**
 * DataTable — wrapper around Fluent DataGrid per design system
 * - Consistent zebra striping (Background1 / Background2)
 * - Header: Background2, Title 3 weight 600
 * - Row height: 40px min, cell padding per design system
 * - Sort icons: ArrowSort24Regular
 */
import {
  DataGrid,
  DataGridHeader,
  DataGridHeaderCell,
  DataGridBody,
  DataGridRow,
  DataGridCell,
  type DataGridProps,
  type TableColumnDefinition,
  makeStyles,
  tokens,
  Button,
} from '@fluentui/react-components';
import {
  ChevronLeft24Regular,
  ChevronRight24Regular,
} from '@fluentui/react-icons';
import { elevation } from '../../theme/tokens';

const useStyles = makeStyles({
  root: {
    backgroundColor: tokens.colorNeutralBackground1,
    borderRadius: '8px',
    border: `1px solid ${tokens.colorNeutralStroke1}`,
    boxShadow: elevation.level1,
    overflow: 'hidden',
  },
  headerCell: {
    backgroundColor: tokens.colorNeutralBackground2,
    fontWeight: tokens.fontWeightSemibold,
    fontSize: tokens.fontSizeBase300,
    lineHeight: tokens.lineHeightBase300,
  },
  row: {
    minHeight: '40px',
    ':nth-child(even)': {
      backgroundColor: tokens.colorNeutralBackground2,
    },
  },
  pagination: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'flex-end',
    gap: tokens.spacingHorizontalS,
    padding: `${tokens.spacingVerticalM} ${tokens.spacingHorizontalM}`,
    borderTop: `1px solid ${tokens.colorNeutralStroke1}`,
    fontSize: tokens.fontSizeBase200,
    color: tokens.colorNeutralForeground3,
  },
  emptyRow: {
    padding: `${tokens.spacingVerticalXXXL} ${tokens.spacingHorizontalXL}`,
    textAlign: 'center',
    color: tokens.colorNeutralForeground3,
    fontSize: tokens.fontSizeBase300,
  },
});

export interface DataTableProps<T> {
  items: T[];
  columns: TableColumnDefinition<T>[];
  sortable?: boolean;
  pagination?: {
    page: number;
    pageSize: number;
    total: number;
    onPageChange: (page: number) => void;
  };
  emptyMessage?: string;
  getRowId?: (item: T) => string;
}

export function DataTable<T extends object>({
  items,
  columns,
  sortable = false,
  pagination,
  emptyMessage = 'No data available',
  getRowId,
}: DataTableProps<T>) {
  const styles = useStyles();

  const gridProps: DataGridProps = {
    items,
    columns,
    ...(sortable ? { sortable: true } : {}),
    ...(getRowId ? { getRowId } : {}),
  };

  const totalPages = pagination ? Math.ceil(pagination.total / pagination.pageSize) : 0;

  return (
    <div className={styles.root}>
      <DataGrid {...gridProps}>
        <DataGridHeader>
          <DataGridRow>
            {({ renderHeaderCell }) => (
              <DataGridHeaderCell className={styles.headerCell}>
                {renderHeaderCell()}
              </DataGridHeaderCell>
            )}
          </DataGridRow>
        </DataGridHeader>
        <DataGridBody<T>>
          {({ item, rowId }) => (
            <DataGridRow<T> key={rowId} className={styles.row}>
              {({ renderCell }) => (
                <DataGridCell>{renderCell(item)}</DataGridCell>
              )}
            </DataGridRow>
          )}
        </DataGridBody>
      </DataGrid>

      {items.length === 0 && (
        <div className={styles.emptyRow}>{emptyMessage}</div>
      )}

      {pagination && totalPages > 1 && (
        <div className={styles.pagination}>
          <span>
            Page {pagination.page} of {totalPages}
          </span>
          <Button
            appearance="subtle"
            icon={<ChevronLeft24Regular />}
            size="small"
            disabled={pagination.page <= 1}
            onClick={() => pagination.onPageChange(pagination.page - 1)}
          />
          <Button
            appearance="subtle"
            icon={<ChevronRight24Regular />}
            size="small"
            disabled={pagination.page >= totalPages}
            onClick={() => pagination.onPageChange(pagination.page + 1)}
          />
        </div>
      )}
    </div>
  );
}
