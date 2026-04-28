/**
 * Projects List – P8-W7/W9
 * Shows projects synced from ServiceNow with RAG status indicators, KPIs, and filtering.
 */
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  makeStyles,
  tokens,
  Button,
  Input,
  Select,
  Option,
  Badge,
  Spinner,
  ProgressBar,
  Table,
  TableHeader,
  TableHeaderCell,
  TableBody,
  TableRow,
  TableCell,
  TableCellLayout,
  type BadgeProps,
} from '@fluentui/react-components';
import { OpenRegular, ArrowClockwiseRegular } from '@fluentui/react-icons';
import { useQuery, useMutation } from '@tanstack/react-query';
import { listProjects, triggerProjectSync } from '../api/projects';
import { listConnections } from '../api/integrations';
import type { ProjectListFilters, RagStatus, SnowProject } from '@reportplatform/types';

const RAG_COLOR: Record<RagStatus, BadgeProps['color']> = {
  RED: 'danger',
  AMBER: 'warning',
  GREEN: 'success',
};

const useStyles = makeStyles({
  container: { padding: tokens.spacingHorizontalL },
  header: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: tokens.spacingVerticalM,
  },
  title: { fontSize: tokens.fontSizeBase600, fontWeight: tokens.fontWeightSemibold },
  filters: {
    display: 'flex',
    gap: tokens.spacingHorizontalM,
    marginBottom: tokens.spacingVerticalM,
    flexWrap: 'wrap',
    alignItems: 'flex-end',
  },
  filterLabel: {
    display: 'flex',
    flexDirection: 'column',
    gap: tokens.spacingVerticalXS,
    fontSize: tokens.fontSizeBase200,
    color: tokens.colorNeutralForeground3,
  },
  pagination: {
    marginTop: tokens.spacingVerticalM,
    display: 'flex',
    gap: tokens.spacingHorizontalM,
    alignItems: 'center',
    color: tokens.colorNeutralForeground3,
  },
});

export default function ProjectsPage() {
  const styles = useStyles();
  const navigate = useNavigate();

  const [filters, setFilters] = useState<ProjectListFilters>({ page: 0, size: 20 });

  const { data: connections = [] } = useQuery({
    queryKey: ['sn-connections'],
    queryFn: listConnections,
  });

  const { data: projectPage, isLoading, refetch } = useQuery({
    queryKey: ['snow-projects', filters],
    queryFn: () => listProjects(filters),
  });

  const syncMut = useMutation({
    mutationFn: (connectionId: string) => triggerProjectSync(connectionId),
    onSuccess: () => refetch(),
  });

  const projects = projectPage?.content ?? [];
  const totalPages = projectPage?.totalPages ?? 0;
  const totalElements = projectPage?.totalElements ?? 0;

  function setFilter<K extends keyof ProjectListFilters>(key: K, value: ProjectListFilters[K]) {
    setFilters(prev => ({ ...prev, [key]: value, page: 0 }));
  }

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <span className={styles.title}>ServiceNow Projects</span>
        <div style={{ display: 'flex', gap: tokens.spacingHorizontalS }}>
          {connections.length > 0 && (
            <Button
              icon={<ArrowClockwiseRegular />}
              disabled={syncMut.isPending || !filters.connectionId}
              onClick={() => filters.connectionId && syncMut.mutate(filters.connectionId)}
            >
              {syncMut.isPending ? 'Syncing…' : 'Sync Now'}
            </Button>
          )}
        </div>
      </div>

      <div className={styles.filters}>
        <label className={styles.filterLabel}>
          Connection
          <Select
            value={filters.connectionId ?? ''}
            onChange={(_, d) => setFilter('connectionId', d.value || undefined)}
            style={{ width: 200 }}
          >
            <Option value="">All connections</Option>
            {connections.map((c) => (
              <Option key={c.id} value={c.id}>{c.instance_url}</Option>
            ))}
          </Select>
        </label>

        <label className={styles.filterLabel}>
          RAG Status
          <Select
            value={filters.ragStatus ?? ''}
            onChange={(_, d) => setFilter('ragStatus', (d.value as RagStatus) || undefined)}
            style={{ width: 140 }}
          >
            <Option value="">All</Option>
            <Option value="RED">RED</Option>
            <Option value="AMBER">AMBER</Option>
            <Option value="GREEN">GREEN</Option>
          </Select>
        </label>

        <label className={styles.filterLabel}>
          Status
          <Input
            placeholder="active, closed…"
            value={filters.status ?? ''}
            onChange={(_, d) => setFilter('status', d.value || undefined)}
            style={{ width: 160 }}
          />
        </label>

        <label className={styles.filterLabel}>
          Manager email
          <Input
            placeholder="filter by manager"
            value={filters.managerEmail ?? ''}
            onChange={(_, d) => setFilter('managerEmail', d.value || undefined)}
            style={{ width: 200 }}
          />
        </label>
      </div>

      {isLoading && <Spinner label="Loading projects…" />}

      {!isLoading && (
        <>
          <Table aria-label="Projects">
            <TableHeader>
              <TableRow>
                <TableHeaderCell>Number</TableHeaderCell>
                <TableHeaderCell>Description</TableHeaderCell>
                <TableHeaderCell>Status</TableHeaderCell>
                <TableHeaderCell>Manager</TableHeaderCell>
                <TableHeaderCell>Budget Utilization</TableHeaderCell>
                <TableHeaderCell>Schedule Variance</TableHeaderCell>
                <TableHeaderCell>Milestone Rate</TableHeaderCell>
                <TableHeaderCell>RAG</TableHeaderCell>
                <TableHeaderCell>Actions</TableHeaderCell>
              </TableRow>
            </TableHeader>
            <TableBody>
              {projects.map((p: SnowProject) => (
                <TableRow key={p.id}>
                  <TableCell>
                    <TableCellLayout>{p.number ?? p.sysId}</TableCellLayout>
                  </TableCell>
                  <TableCell>
                    <TableCellLayout truncate style={{ maxWidth: 250 }}>
                      {p.shortDescription ?? '—'}
                    </TableCellLayout>
                  </TableCell>
                  <TableCell>
                    <Badge appearance="outline">{p.status ?? '—'}</Badge>
                  </TableCell>
                  <TableCell>
                    <TableCellLayout>{p.managerName ?? '—'}</TableCellLayout>
                  </TableCell>
                  <TableCell style={{ minWidth: 140 }}>
                    {p.budgetUtilizationPct != null ? (
                      <div style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                        <span style={{ fontSize: tokens.fontSizeBase100 }}>
                          {p.budgetUtilizationPct.toFixed(1)}%
                        </span>
                        <ProgressBar
                          value={p.budgetUtilizationPct / 100}
                          color={p.budgetUtilizationPct > 95 ? 'error' : p.budgetUtilizationPct > 80 ? 'warning' : 'success'}
                        />
                      </div>
                    ) : '—'}
                  </TableCell>
                  <TableCell>
                    {p.scheduleVarianceDays != null ? (
                      <span style={{
                        color: p.scheduleVarianceDays > 14
                          ? tokens.colorPaletteRedForeground1
                          : p.scheduleVarianceDays > 0
                            ? tokens.colorPaletteMarigoldForeground2
                            : tokens.colorPaletteGreenForeground1,
                        fontWeight: tokens.fontWeightSemibold,
                      }}>
                        {p.scheduleVarianceDays > 0 ? '+' : ''}{p.scheduleVarianceDays}d
                      </span>
                    ) : '—'}
                  </TableCell>
                  <TableCell>
                    {p.milestoneCompletionRate != null
                      ? `${p.milestoneCompletionRate.toFixed(0)}%`
                      : '—'}
                  </TableCell>
                  <TableCell>
                    {p.ragStatus ? (
                      <Badge color={RAG_COLOR[p.ragStatus]}>{p.ragStatus}</Badge>
                    ) : '—'}
                  </TableCell>
                  <TableCell>
                    <Button
                      size="small"
                      icon={<OpenRegular />}
                      onClick={() => navigate(`/projects/${p.id}`)}
                    >
                      Detail
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>

          <div className={styles.pagination}>
            <Button
              size="small"
              disabled={(filters.page ?? 0) === 0}
              onClick={() => setFilters(p => ({ ...p, page: (p.page ?? 1) - 1 }))}
            >
              Previous
            </Button>
            <span>
              Page {(filters.page ?? 0) + 1} / {totalPages} — {totalElements} project(s)
            </span>
            <Button
              size="small"
              disabled={(filters.page ?? 0) >= totalPages - 1}
              onClick={() => setFilters(p => ({ ...p, page: (p.page ?? 0) + 1 }))}
            >
              Next
            </Button>
          </div>
        </>
      )}
    </div>
  );
}
