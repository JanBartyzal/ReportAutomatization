/**
 * Project Detail – P8-W7/W9
 * Full project view: KPI cards, task tree, and budget lines.
 */
import { useNavigate, useParams } from 'react-router-dom';
import {
  makeStyles,
  tokens,
  Button,
  Badge,
  Spinner,
  MessageBar,
  ProgressBar,
  Tab,
  TabList,
  Table,
  TableHeader,
  TableHeaderCell,
  TableBody,
  TableRow,
  TableCell,
  TableCellLayout,
  type BadgeProps,
} from '@fluentui/react-components';
import { ArrowLeft24Regular, PlayRegular } from '@fluentui/react-icons';
import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { getProject } from '../api/projects';
import type { RagStatus, SnowProjectTask, SnowProjectBudget } from '@reportplatform/types';

const RAG_COLOR: Record<RagStatus, BadgeProps['color']> = {
  RED: 'danger',
  AMBER: 'warning',
  GREEN: 'success',
};

const useStyles = makeStyles({
  container: { padding: tokens.spacingHorizontalL, maxWidth: '1100px' },
  header: {
    display: 'flex',
    alignItems: 'center',
    gap: tokens.spacingHorizontalM,
    marginBottom: tokens.spacingVerticalM,
  },
  title: { fontSize: tokens.fontSizeBase600, fontWeight: tokens.fontWeightSemibold },
  kpiGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fill, minmax(180px, 1fr))',
    gap: tokens.spacingHorizontalM,
    marginBottom: tokens.spacingVerticalL,
  },
  kpiCard: {
    backgroundColor: tokens.colorNeutralBackground2,
    borderRadius: tokens.borderRadiusMedium,
    padding: `${tokens.spacingVerticalM} ${tokens.spacingHorizontalM}`,
    display: 'flex',
    flexDirection: 'column',
    gap: tokens.spacingVerticalXS,
  },
  kpiLabel: {
    fontSize: tokens.fontSizeBase100,
    color: tokens.colorNeutralForeground3,
    textTransform: 'uppercase',
    letterSpacing: '0.05em',
  },
  kpiValue: {
    fontSize: tokens.fontSizeBase500,
    fontWeight: tokens.fontWeightSemibold,
    color: tokens.colorNeutralForeground1,
  },
  dimText: { fontSize: tokens.fontSizeBase200, color: tokens.colorNeutralForeground3 },
});

function KpiCard({ label, value }: { label: string; value: React.ReactNode }) {
  const styles = useStyles();
  return (
    <div className={styles.kpiCard}>
      <span className={styles.kpiLabel}>{label}</span>
      <span className={styles.kpiValue}>{value}</span>
    </div>
  );
}

export default function ProjectDetailPage() {
  const styles = useStyles();
  const navigate = useNavigate();
  const { projectId } = useParams<{ projectId: string }>();
  const [activeTab, setActiveTab] = useState('tasks');

  const { data: project, isLoading, isError } = useQuery({
    queryKey: ['snow-project', projectId],
    queryFn: () => getProject(projectId!),
    enabled: !!projectId,
  });

  if (isLoading) return <Spinner label="Loading project…" />;
  if (isError || !project) {
    return <MessageBar intent="error">Project not found.</MessageBar>;
  }

  const tasks = project.tasks ?? [];
  const budgets = project.budgets ?? [];

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <Button
          appearance="subtle"
          icon={<ArrowLeft24Regular />}
          onClick={() => navigate('/projects')}
        />
        <span className={styles.title}>
          {project.number ?? project.sysId}: {project.shortDescription}
        </span>
        {project.ragStatus && (
          <Badge color={RAG_COLOR[project.ragStatus]} size="large">{project.ragStatus}</Badge>
        )}
        <Button
          appearance="primary"
          icon={<PlayRegular />}
          onClick={() => navigate('/reporting/text-templates')}
          style={{ marginLeft: 'auto' }}
        >
          Generate Report
        </Button>
      </div>

      {/* KPI Grid */}
      <div className={styles.kpiGrid}>
        <KpiCard label="Status" value={<Badge appearance="outline">{project.status ?? '—'}</Badge>} />
        <KpiCard label="Phase" value={project.phase ?? '—'} />
        <KpiCard label="Manager" value={project.managerName ?? '—'} />
        <KpiCard label="% Complete" value={
          project.percentComplete != null ? `${project.percentComplete.toFixed(0)}%` : '—'
        } />
        <KpiCard label="Budget Utilization" value={
          project.budgetUtilizationPct != null
            ? (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 4, minWidth: 120 }}>
                <span>{project.budgetUtilizationPct.toFixed(1)}%</span>
                <ProgressBar
                  value={project.budgetUtilizationPct / 100}
                  color={
                    project.budgetUtilizationPct > 95 ? 'error'
                    : project.budgetUtilizationPct > 80 ? 'warning'
                    : 'success'
                  }
                />
              </div>
            ) : '—'
        } />
        <KpiCard label="Schedule Variance" value={
          project.scheduleVarianceDays != null
            ? `${project.scheduleVarianceDays > 0 ? '+' : ''}${project.scheduleVarianceDays} days`
            : '—'
        } />
        <KpiCard label="Milestone Rate" value={
          project.milestoneCompletionRate != null
            ? `${project.milestoneCompletionRate.toFixed(0)}%`
            : '—'
        } />
        <KpiCard label="Total Budget" value={
          project.totalBudget != null
            ? new Intl.NumberFormat('cs-CZ', { maximumFractionDigits: 0 }).format(project.totalBudget)
            : '—'
        } />
        <KpiCard label="Actual Cost" value={
          project.actualCost != null
            ? new Intl.NumberFormat('cs-CZ', { maximumFractionDigits: 0 }).format(project.actualCost)
            : '—'
        } />
        <KpiCard label="Planned End" value={project.plannedEndDate ?? '—'} />
        <KpiCard label="Projected End" value={project.projectedEndDate ?? '—'} />
      </div>

      {/* Tabs */}
      <TabList
        selectedValue={activeTab}
        onTabSelect={(_, d) => setActiveTab(String(d.value))}
      >
        <Tab value="tasks">Tasks ({tasks.length})</Tab>
        <Tab value="budgets">Budget Lines ({budgets.length})</Tab>
      </TabList>

      {activeTab === 'tasks' && (
        <Table aria-label="Project tasks" style={{ marginTop: tokens.spacingVerticalM }}>
          <TableHeader>
            <TableRow>
              <TableHeaderCell>Number</TableHeaderCell>
              <TableHeaderCell>Description</TableHeaderCell>
              <TableHeaderCell>State</TableHeaderCell>
              <TableHeaderCell>Milestone</TableHeaderCell>
              <TableHeaderCell>Assigned To</TableHeaderCell>
              <TableHeaderCell>Due Date</TableHeaderCell>
            </TableRow>
          </TableHeader>
          <TableBody>
            {tasks.map((t: SnowProjectTask) => (
              <TableRow key={t.id}>
                <TableCell><TableCellLayout>{t.number ?? t.sysId}</TableCellLayout></TableCell>
                <TableCell>
                  <TableCellLayout truncate style={{ maxWidth: 300 }}>
                    {t.shortDescription ?? '—'}
                  </TableCellLayout>
                </TableCell>
                <TableCell><Badge appearance="outline">{t.state ?? '—'}</Badge></TableCell>
                <TableCell>
                  {t.milestone && <Badge color="informative">Milestone</Badge>}
                </TableCell>
                <TableCell><TableCellLayout>{t.assignedToName ?? '—'}</TableCellLayout></TableCell>
                <TableCell>
                  <TableCellLayout>
                    <span style={{
                      color: t.dueDate && !t.completedAt && new Date(t.dueDate) < new Date()
                        ? tokens.colorPaletteRedForeground1
                        : undefined,
                    }}>
                      {t.dueDate ?? '—'}
                    </span>
                  </TableCellLayout>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}

      {activeTab === 'budgets' && (
        <Table aria-label="Budget lines" style={{ marginTop: tokens.spacingVerticalM }}>
          <TableHeader>
            <TableRow>
              <TableHeaderCell>Category</TableHeaderCell>
              <TableHeaderCell>Fiscal Year</TableHeaderCell>
              <TableHeaderCell>Planned Amount</TableHeaderCell>
              <TableHeaderCell>Actual Amount</TableHeaderCell>
              <TableHeaderCell>Utilization</TableHeaderCell>
            </TableRow>
          </TableHeader>
          <TableBody>
            {budgets.map((b: SnowProjectBudget) => {
              const util = b.plannedAmount && b.actualAmount
                ? (b.actualAmount / b.plannedAmount) * 100
                : null;
              return (
                <TableRow key={b.id}>
                  <TableCell><TableCellLayout>{b.category ?? '—'}</TableCellLayout></TableCell>
                  <TableCell><TableCellLayout>{b.fiscalYear ?? '—'}</TableCellLayout></TableCell>
                  <TableCell>
                    <TableCellLayout>
                      {b.plannedAmount != null
                        ? new Intl.NumberFormat('cs-CZ', { maximumFractionDigits: 0 }).format(b.plannedAmount)
                        : '—'}
                    </TableCellLayout>
                  </TableCell>
                  <TableCell>
                    <TableCellLayout>
                      {b.actualAmount != null
                        ? new Intl.NumberFormat('cs-CZ', { maximumFractionDigits: 0 }).format(b.actualAmount)
                        : '—'}
                    </TableCellLayout>
                  </TableCell>
                  <TableCell>
                    {util != null ? (
                      <div style={{ display: 'flex', flexDirection: 'column', gap: 2, minWidth: 100 }}>
                        <span style={{ fontSize: tokens.fontSizeBase100 }}>{util.toFixed(1)}%</span>
                        <ProgressBar
                          value={util / 100}
                          color={util > 95 ? 'error' : util > 80 ? 'warning' : 'success'}
                        />
                      </div>
                    ) : '—'}
                  </TableCell>
                </TableRow>
              );
            })}
          </TableBody>
        </Table>
      )}
    </div>
  );
}
