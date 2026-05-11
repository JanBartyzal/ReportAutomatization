import { useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  Badge,
  Body1,
  Button,
  Caption1,
  Card,
  CardHeader,
  Drawer,
  DrawerBody,
  DrawerHeader,
  DrawerHeaderTitle,
  Spinner,
  Title2,
  Title3,
  makeStyles,
  tokens,
} from '@fluentui/react-components';
import {
  AddRegular,
  ArrowLeftRegular,
  ArrowSyncRegular,
  ChartMultipleRegular,
  DismissRegular,
  OpenRegular,
} from '@fluentui/react-icons';
import {
  useCreateDrilldownReport,
  useDrilldownAction,
  useDrilldownReport,
  useDrilldownReportData,
  useDrilldownReports,
} from '../hooks/useDrilldownReports';
import type {
  DrilldownReportSection,
  DrilldownResult,
  DrilldownSectionResult,
} from '@reportplatform/types';

const useStyles = makeStyles({
  page: {
    padding: tokens.spacingHorizontalL,
    display: 'flex',
    flexDirection: 'column',
    gap: tokens.spacingVerticalL,
  },
  header: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    gap: tokens.spacingHorizontalM,
    flexWrap: 'wrap',
  },
  headerTitle: {
    display: 'flex',
    alignItems: 'center',
    gap: tokens.spacingHorizontalS,
  },
  grid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))',
    gap: tokens.spacingHorizontalM,
  },
  reportCard: {
    cursor: 'pointer',
    minHeight: '148px',
    ':hover': {
      boxShadow: tokens.shadow8,
    },
  },
  analyticsGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(12, minmax(0, 1fr))',
    gap: tokens.spacingHorizontalM,
  },
  sectionCard: {
    minHeight: '180px',
    gridColumn: 'span 6',
    '@media (max-width: 900px)': {
      gridColumn: '1 / -1',
    },
  },
  tableSectionCard: {
    minHeight: '180px',
    gridColumn: '1 / -1',
  },
  sectionBody: {
    padding: tokens.spacingHorizontalM,
    display: 'flex',
    flexDirection: 'column',
    gap: tokens.spacingVerticalS,
  },
  empty: {
    padding: '48px 24px',
    textAlign: 'center',
    color: tokens.colorNeutralForeground3,
  },
  kpiValue: {
    fontSize: '36px',
    lineHeight: '44px',
    fontWeight: 700,
  },
  filterBar: {
    display: 'flex',
    gap: tokens.spacingHorizontalS,
    alignItems: 'center',
    flexWrap: 'wrap',
  },
  barRow: {
    display: 'grid',
    gridTemplateColumns: 'minmax(90px, 1fr) minmax(120px, 3fr) 56px',
    alignItems: 'center',
    gap: tokens.spacingHorizontalS,
  },
  barTrack: {
    height: '10px',
    backgroundColor: tokens.colorNeutralBackground3,
    borderRadius: '4px',
    overflow: 'hidden',
  },
  barFill: {
    height: '100%',
    backgroundColor: tokens.colorBrandBackground,
  },
  tableWrap: {
    overflowX: 'auto',
  },
  table: {
    width: '100%',
    borderCollapse: 'collapse',
    fontSize: '13px',
  },
  th: {
    textAlign: 'left',
    padding: '8px',
    borderBottom: `1px solid ${tokens.colorNeutralStroke2}`,
    whiteSpace: 'nowrap',
  },
  td: {
    padding: '8px',
    borderBottom: `1px solid ${tokens.colorNeutralStroke3}`,
    maxWidth: '260px',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
  },
  drawerBody: {
    display: 'flex',
    flexDirection: 'column',
    gap: tokens.spacingVerticalM,
  },
});

function createStarterReportPayload() {
  return {
    name: 'OPEX Drill-down Overview',
    description: 'Interactive summary with source table drill-down.',
    report_type: 'ANALYTICAL',
    default_filters: {},
    layout_config: { columns: 12 },
    is_public: false,
    sections: [
      {
        section_key: 'source-count',
        title: 'Source Tables',
        component_type: 'KPI' as const,
        source_type: 'RAW_SQL' as const,
        query_config: { sql: 'SELECT COUNT(*) AS total_tables FROM parsed_tables' },
        drill_config: {
          sql: "SELECT id::text AS id, file_id::text AS file_id, COALESCE(source_sheet, 'Unknown') AS source_sheet, jsonb_array_length(rows) AS row_count FROM parsed_tables ORDER BY created_at DESC",
        },
        display_order: 0,
      },
      {
        section_key: 'sheets-by-count',
        title: 'Tables by Sheet',
        component_type: 'CHART' as const,
        source_type: 'RAW_SQL' as const,
        query_config: {
          sql: "SELECT COALESCE(source_sheet, 'Unknown') AS label, COUNT(*) AS value FROM parsed_tables GROUP BY 1 ORDER BY value DESC",
        },
        drill_config: {
          sql: "SELECT id::text AS id, file_id::text AS file_id, COALESCE(source_sheet, 'Unknown') AS source_sheet, jsonb_array_length(rows) AS row_count FROM parsed_tables ORDER BY created_at DESC",
        },
        display_order: 1,
      },
      {
        section_key: 'latest-sources',
        title: 'Latest Sources',
        component_type: 'TABLE' as const,
        source_type: 'RAW_SQL' as const,
        query_config: {
          sql: "SELECT id::text AS id, file_id::text AS file_id, COALESCE(source_sheet, 'Unknown') AS source_sheet, jsonb_array_length(rows) AS row_count FROM parsed_tables ORDER BY created_at DESC",
        },
        drill_config: {
          sql: "SELECT id::text AS id, file_id::text AS file_id, COALESCE(source_sheet, 'Unknown') AS source_sheet, headers::text AS headers, rows::text AS rows_json FROM parsed_tables ORDER BY created_at DESC",
        },
        display_order: 2,
      },
    ],
  };
}

function normalizeRows(result?: DrilldownSectionResult): { columns: string[]; rows: unknown[][] } {
  if (!result) return { columns: [], rows: [] };
  if (result.columns && Array.isArray(result.rows) && Array.isArray(result.rows[0])) {
    return { columns: result.columns, rows: result.rows as unknown[][] };
  }
  const objectRows = (result.rows ?? []) as unknown as Record<string, unknown>[];
  const columns = objectRows.length > 0 ? Object.keys(objectRows[0]) : [];
  const rows = objectRows.map((row) => columns.map((column) => row[column]));
  return { columns, rows };
}

function valueText(value: unknown): string {
  if (value === null || value === undefined) return '';
  if (typeof value === 'object') return JSON.stringify(value);
  return String(value);
}

function ResultTable({
  columns,
  rows,
  onRowClick,
}: {
  columns: string[];
  rows: unknown[][];
  onRowClick?: (row: unknown[]) => void;
}) {
  const styles = useStyles();
  if (columns.length === 0 || rows.length === 0) {
    return <Caption1>No rows</Caption1>;
  }
  return (
    <div className={styles.tableWrap}>
      <table className={styles.table}>
        <thead>
          <tr>
            {columns.map((column) => (
              <th key={column} className={styles.th}>
                {column}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.slice(0, 12).map((row, rowIndex) => (
            <tr key={rowIndex} onClick={() => onRowClick?.(row)}>
              {row.map((cell, cellIndex) => (
                <td key={cellIndex} className={styles.td} title={valueText(cell)}>
                  {valueText(cell)}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function SectionContent({
  section,
  result,
  onDrill,
}: {
  section: DrilldownReportSection;
  result?: DrilldownSectionResult;
  onDrill: (selectedValue?: unknown) => void;
}) {
  const styles = useStyles();
  const { columns, rows } = normalizeRows(result);

  if (section.component_type === 'KPI') {
    const value = rows[0]?.[0] ?? '0';
    return (
      <>
        <div className={styles.kpiValue}>{valueText(value)}</div>
        <Button appearance="subtle" icon={<OpenRegular />} onClick={() => onDrill(value)}>
          Open detail
        </Button>
      </>
    );
  }

  if (section.component_type === 'CHART') {
    const values = rows.map((row) => Number(row[1] ?? 0));
    const max = Math.max(...values, 1);
    return (
      <>
        {rows.slice(0, 8).map((row, index) => (
          <button
            key={index}
            type="button"
            className={styles.barRow}
            onClick={() => onDrill(row[0])}
            style={{ border: 0, background: 'transparent', padding: 0, cursor: 'pointer' }}
          >
            <Caption1 title={valueText(row[0])}>{valueText(row[0])}</Caption1>
            <span className={styles.barTrack}>
              <span
                className={styles.barFill}
                style={{ width: `${Math.max((Number(row[1] ?? 0) / max) * 100, 4)}%` }}
              />
            </span>
            <Caption1>{valueText(row[1])}</Caption1>
          </button>
        ))}
      </>
    );
  }

  return <ResultTable columns={columns} rows={rows} onRowClick={(row) => onDrill(row[0])} />;
}

function DrilldownReportList() {
  const styles = useStyles();
  const navigate = useNavigate();
  const { data: reports, isLoading } = useDrilldownReports();
  const createReport = useCreateDrilldownReport();

  const handleCreate = async () => {
    const report = await createReport.mutateAsync(createStarterReportPayload());
    navigate(`/reports/analytics/${report.id}`);
  };

  if (isLoading) {
    return <Spinner label="Loading analytical reports..." />;
  }

  return (
    <div className={styles.page}>
      <div className={styles.header}>
        <div>
          <div className={styles.headerTitle}>
            <ChartMultipleRegular />
            <Title2>Analytical Reports</Title2>
          </div>
          <Body1 style={{ color: tokens.colorNeutralForeground3 }}>
            Summary reports with clickable drill-down into detail rows.
          </Body1>
        </div>
        <Button
          appearance="primary"
          icon={createReport.isPending ? <Spinner size="tiny" /> : <AddRegular />}
          disabled={createReport.isPending}
          onClick={handleCreate}
        >
          New Analytical Report
        </Button>
      </div>

      {!reports || reports.length === 0 ? (
        <div className={styles.empty}>
          <Title3 block>No analytical reports yet</Title3>
          <Body1 block>Create a starter report to assemble summary sections and drill-down details.</Body1>
        </div>
      ) : (
        <div className={styles.grid}>
          {reports.map((report) => (
            <Card
              key={report.id}
              className={styles.reportCard}
              onClick={() => navigate(`/reports/analytics/${report.id}`)}
            >
              <CardHeader
                header={<Title3>{report.name}</Title3>}
                description={report.description}
              />
              <div style={{ padding: tokens.spacingHorizontalM }}>
                <Badge appearance="filled" color={report.is_public ? 'success' : 'informative'}>
                  {report.is_public ? 'Public' : 'Private'}
                </Badge>
                <Caption1 block style={{ marginTop: tokens.spacingVerticalS }}>
                  {report.sections?.length ?? 0} sections
                </Caption1>
              </div>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}

function DrilldownReportViewer({ reportId }: { reportId: string }) {
  const styles = useStyles();
  const navigate = useNavigate();
  const [filters, setFilters] = useState<Record<string, unknown>>({});
  const [detail, setDetail] = useState<DrilldownResult | null>(null);
  const { data: report, isLoading } = useDrilldownReport(reportId);
  const { data, isFetching, refetch } = useDrilldownReportData(reportId, filters);
  const drill = useDrilldownAction(reportId);

  const sortedSections = useMemo(
    () => [...(report?.sections ?? [])].sort((a, b) => a.display_order - b.display_order),
    [report?.sections]
  );

  const handleDrill = async (section: DrilldownReportSection, selectedValue?: unknown) => {
    const nextFilters = selectedValue
      ? { ...filters, [section.section_key]: selectedValue }
      : filters;
    setFilters(nextFilters);
    const result = await drill.mutateAsync({
      section_key: section.section_key,
      filters: nextFilters,
      selected_value: selectedValue,
      page: 0,
      size: 50,
    });
    setDetail(result);
  };

  if (isLoading || !report) {
    return (
      <div className={styles.page}>
        <Spinner label="Loading analytical report..." />
      </div>
    );
  }

  return (
    <div className={styles.page}>
      <div className={styles.header}>
        <div>
          <div className={styles.headerTitle}>
            <Button appearance="subtle" icon={<ArrowLeftRegular />} onClick={() => navigate('/reports/analytics')}>
              Back
            </Button>
            <Title2>{report.name}</Title2>
          </div>
          {report.description && (
            <Body1 style={{ color: tokens.colorNeutralForeground3 }}>{report.description}</Body1>
          )}
        </div>
        <Button appearance="subtle" icon={<ArrowSyncRegular />} onClick={() => refetch()} disabled={isFetching}>
          Refresh
        </Button>
      </div>

      <div className={styles.filterBar}>
        <Caption1>Filters</Caption1>
        {Object.entries(filters).length === 0 ? (
          <Badge>No active filters</Badge>
        ) : (
          Object.entries(filters).map(([key, value]) => (
            <Badge key={key} appearance="filled" color="brand">
              {key}: {valueText(value)}
            </Badge>
          ))
        )}
        {Object.keys(filters).length > 0 && (
          <Button size="small" appearance="subtle" onClick={() => setFilters({})}>
            Clear
          </Button>
        )}
      </div>

      <div className={styles.analyticsGrid}>
        {sortedSections.map((section) => (
          <Card
            key={section.section_key}
            className={section.component_type === 'TABLE' ? styles.tableSectionCard : styles.sectionCard}
          >
            <CardHeader
              header={<Title3>{section.title}</Title3>}
              description={section.source_type}
            />
            <div className={styles.sectionBody}>
              {isFetching ? (
                <Spinner label="Loading section..." />
              ) : (
                <SectionContent
                  section={section}
                  result={data?.sections?.[section.section_key]}
                  onDrill={(selectedValue) => handleDrill(section, selectedValue)}
                />
              )}
            </div>
          </Card>
        ))}
      </div>

      {sortedSections.length === 0 && (
        <div className={styles.empty}>
          <Title3 block>No sections configured</Title3>
          <Body1 block>This analytical report is ready for composer configuration.</Body1>
        </div>
      )}

      <Drawer
        type="overlay"
        position="end"
        size="large"
        open={!!detail}
        onOpenChange={(_, change) => !change.open && setDetail(null)}
      >
        <DrawerHeader>
          <DrawerHeaderTitle
            action={
              <Button appearance="subtle" icon={<DismissRegular />} onClick={() => setDetail(null)} />
            }
          >
            Drill-down Detail
          </DrawerHeaderTitle>
        </DrawerHeader>
        <DrawerBody className={styles.drawerBody}>
          {detail && (
            <>
              <div className={styles.filterBar}>
                <Badge appearance="filled" color="brand">
                  {detail.section_key}
                </Badge>
                {detail.selected_value !== undefined && <Badge>{valueText(detail.selected_value)}</Badge>}
              </div>
              <ResultTable columns={detail.columns} rows={detail.rows} />
            </>
          )}
        </DrawerBody>
      </Drawer>
    </div>
  );
}

export default function DrilldownReportsPage() {
  const { reportId } = useParams<{ reportId: string }>();
  if (reportId) {
    return <DrilldownReportViewer reportId={reportId} />;
  }
  return <DrilldownReportList />;
}
