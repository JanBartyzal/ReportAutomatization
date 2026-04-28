/**
 * Named Query Catalog – P8-W5/W6
 * Browse, create, edit and test named queries (data-source agnostic).
 */
import { useState } from 'react';
import {
  makeStyles,
  tokens,
  Button,
  Input,
  Textarea,
  Badge,
  Dialog,
  DialogTrigger,
  DialogSurface,
  DialogBody,
  DialogTitle,
  DialogContent,
  DialogActions,
  Label,
  Select,
  Option,
  MessageBar,
  Spinner,
  Table,
  TableHeader,
  TableHeaderCell,
  TableBody,
  TableRow,
  TableCell,
  TableCellLayout,
} from '@fluentui/react-components';
import { AddRegular, DeleteRegular, PlayRegular } from '@fluentui/react-icons';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  listNamedQueries,
  createNamedQuery,
  deleteNamedQuery,
  executeNamedQuery,
} from '../api/namedQueries';
import type { NamedQuery, CreateNamedQueryRequest, NamedQueryResult } from '@reportplatform/types';

const useStyles = makeStyles({
  container: { padding: tokens.spacingHorizontalL },
  header: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: tokens.spacingVerticalM,
  },
  title: { fontSize: tokens.fontSizeBase600, fontWeight: tokens.fontWeightSemibold },
  filters: { display: 'flex', gap: tokens.spacingHorizontalM, marginBottom: tokens.spacingVerticalM },
  systemBadge: { marginLeft: tokens.spacingHorizontalXS },
  resultBox: {
    marginTop: tokens.spacingVerticalM,
    backgroundColor: tokens.colorNeutralBackground2,
    borderRadius: tokens.borderRadiusMedium,
    padding: tokens.spacingHorizontalM,
    fontFamily: 'monospace',
    fontSize: tokens.fontSizeBase200,
    maxHeight: '300px',
    overflowY: 'auto',
  },
  fieldGroup: {
    display: 'flex',
    flexDirection: 'column',
    gap: tokens.spacingVerticalXS,
    marginBottom: tokens.spacingVerticalM,
  },
});

const DATA_SOURCE_TYPES = [
  'PARSED_TABLE', 'SNOW_INCIDENTS', 'SNOW_REQUESTS',
  'SNOW_PROJECTS', 'FORM_RESPONSES', 'CUSTOM',
];

export default function NamedQueryPage() {
  const styles = useStyles();
  const qc = useQueryClient();

  const [search, setSearch] = useState('');
  const [dsFilter, setDsFilter] = useState('');
  const [createOpen, setCreateOpen] = useState(false);
  const [runOpen, setRunOpen] = useState(false);
  const [selectedQuery, setSelectedQuery] = useState<NamedQuery | null>(null);
  const [runResult, setRunResult] = useState<NamedQueryResult | null>(null);
  const [runError, setRunError] = useState<string | null>(null);

  const [form, setForm] = useState<CreateNamedQueryRequest>({
    name: '',
    dataSourceType: 'PARSED_TABLE',
    sqlQuery: '',
    description: '',
  });

  const { data: queries = [], isLoading } = useQuery({
    queryKey: ['named-queries', dsFilter],
    queryFn: () => listNamedQueries(dsFilter || undefined),
  });

  const createMut = useMutation({
    mutationFn: createNamedQuery,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['named-queries'] });
      setCreateOpen(false);
      setForm({ name: '', dataSourceType: 'PARSED_TABLE', sqlQuery: '', description: '' });
    },
  });

  const deleteMut = useMutation({
    mutationFn: deleteNamedQuery,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['named-queries'] }),
  });

  const runMut = useMutation({
    mutationFn: (id: string) => executeNamedQuery(id, { limit: 50 }),
    onSuccess: (result) => { setRunResult(result); setRunError(null); },
    onError: (e: Error) => { setRunError(e.message); setRunResult(null); },
  });

  const filtered = queries.filter(q =>
    q.name.toLowerCase().includes(search.toLowerCase()) ||
    (q.description ?? '').toLowerCase().includes(search.toLowerCase()),
  );

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <span className={styles.title}>Named Query Catalog</span>
        <Button appearance="primary" icon={<AddRegular />} onClick={() => setCreateOpen(true)}>
          New Query
        </Button>
      </div>

      <div className={styles.filters}>
        <Input
          placeholder="Search by name…"
          value={search}
          onChange={(_, d) => setSearch(d.value)}
          contentBefore={<span />}
          style={{ width: 260 }}
        />
        <Select value={dsFilter} onChange={(_, d) => setDsFilter(d.value)}>
          <Option value="">All data sources</Option>
          {DATA_SOURCE_TYPES.map(t => <Option key={t} value={t}>{t}</Option>)}
        </Select>
      </div>

      {isLoading && <Spinner label="Loading queries…" />}

      {!isLoading && (
        <Table aria-label="Named queries">
          <TableHeader>
            <TableRow>
              <TableHeaderCell>Name</TableHeaderCell>
              <TableHeaderCell>Data Source</TableHeaderCell>
              <TableHeaderCell>Description</TableHeaderCell>
              <TableHeaderCell>Type</TableHeaderCell>
              <TableHeaderCell>Actions</TableHeaderCell>
            </TableRow>
          </TableHeader>
          <TableBody>
            {filtered.map(q => (
              <TableRow key={q.id}>
                <TableCell>
                  <TableCellLayout>{q.name}</TableCellLayout>
                </TableCell>
                <TableCell>
                  <Badge appearance="outline">{q.dataSourceType}</Badge>
                </TableCell>
                <TableCell>
                  <TableCellLayout truncate>{q.description ?? '—'}</TableCellLayout>
                </TableCell>
                <TableCell>
                  {q.isSystem && (
                    <Badge appearance="tint" color="informative" className={styles.systemBadge}>
                      System
                    </Badge>
                  )}
                </TableCell>
                <TableCell>
                  <Button
                    size="small"
                    icon={<PlayRegular />}
                    onClick={() => { setSelectedQuery(q); setRunResult(null); setRunError(null); setRunOpen(true); }}
                  >
                    Run
                  </Button>
                  {!q.isSystem && (
                    <Button
                      size="small"
                      icon={<DeleteRegular />}
                      appearance="subtle"
                      onClick={() => deleteMut.mutate(q.id)}
                      style={{ marginLeft: 4 }}
                    >
                      Delete
                    </Button>
                  )}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}

      {/* Create dialog */}
      <Dialog open={createOpen} onOpenChange={(_, d) => setCreateOpen(d.open)}>
        <DialogSurface>
          <DialogBody>
            <DialogTitle>New Named Query</DialogTitle>
            <DialogContent>
              <div className={styles.fieldGroup}>
                <Label htmlFor="nq-name">Name *</Label>
                <Input id="nq-name" value={form.name}
                  onChange={(_, d) => setForm(p => ({ ...p, name: d.value }))} />
              </div>
              <div className={styles.fieldGroup}>
                <Label htmlFor="nq-desc">Description</Label>
                <Input id="nq-desc" value={form.description ?? ''}
                  onChange={(_, d) => setForm(p => ({ ...p, description: d.value }))} />
              </div>
              <div className={styles.fieldGroup}>
                <Label htmlFor="nq-ds">Data Source Type</Label>
                <Select id="nq-ds" value={form.dataSourceType}
                  onChange={(_, d) => setForm(p => ({ ...p, dataSourceType: d.value }))}>
                  {DATA_SOURCE_TYPES.map(t => <Option key={t} value={t}>{t}</Option>)}
                </Select>
              </div>
              <div className={styles.fieldGroup}>
                <Label htmlFor="nq-sql">SQL Query *</Label>
                <Textarea id="nq-sql" value={form.sqlQuery} rows={6}
                  onChange={(_, d) => setForm(p => ({ ...p, sqlQuery: d.value }))}
                  style={{ fontFamily: 'monospace' }} />
              </div>
              {createMut.isError && (
                <MessageBar intent="error">
                  {(createMut.error as Error).message}
                </MessageBar>
              )}
            </DialogContent>
            <DialogActions>
              <DialogTrigger disableButtonEnhancement>
                <Button appearance="secondary">Cancel</Button>
              </DialogTrigger>
              <Button
                appearance="primary"
                disabled={!form.name || !form.sqlQuery || createMut.isPending}
                onClick={() => createMut.mutate(form)}
              >
                {createMut.isPending ? <Spinner size="tiny" /> : 'Create'}
              </Button>
            </DialogActions>
          </DialogBody>
        </DialogSurface>
      </Dialog>

      {/* Run dialog */}
      <Dialog open={runOpen} onOpenChange={(_, d) => setRunOpen(d.open)}>
        <DialogSurface style={{ maxWidth: 700 }}>
          <DialogBody>
            <DialogTitle>Run: {selectedQuery?.name}</DialogTitle>
            <DialogContent>
              <p style={{ fontSize: tokens.fontSizeBase200, color: tokens.colorNeutralForeground3 }}>
                Executes with no parameters, limit 50 rows.
              </p>
              {runMut.isPending && <Spinner label="Executing…" />}
              {runError && <MessageBar intent="error">{runError}</MessageBar>}
              {runResult && (
                <>
                  <p style={{ color: tokens.colorNeutralForeground2, fontSize: tokens.fontSizeBase200 }}>
                    {runResult.totalCount} row(s) — executed {new Date(runResult.executedAt).toLocaleTimeString()}
                  </p>
                  <pre className={styles.resultBox}>
                    {JSON.stringify(runResult.rows.slice(0, 10), null, 2)}
                  </pre>
                </>
              )}
            </DialogContent>
            <DialogActions>
              <DialogTrigger disableButtonEnhancement>
                <Button appearance="secondary">Close</Button>
              </DialogTrigger>
              <Button
                appearance="primary"
                icon={<PlayRegular />}
                disabled={!selectedQuery || runMut.isPending}
                onClick={() => selectedQuery && runMut.mutate(selectedQuery.id)}
              >
                Execute
              </Button>
            </DialogActions>
          </DialogBody>
        </DialogSurface>
      </Dialog>
    </div>
  );
}
