import { useState } from 'react';
import {
  Body1,
  Button,
  Spinner,
  Badge,
  Dialog,
  DialogSurface,
  DialogTitle,
  DialogBody,
  DialogActions,
  DialogContent,
  Input,
  Label,
  Dropdown,
  Option,
  Table,
  TableHeader,
  TableRow,
  TableHeaderCell,
  TableBody,
  TableCell,
  makeStyles,
  tokens,
} from '@fluentui/react-components';
import {
  Add24Regular,
  Delete24Regular,
  ArrowSync24Regular,
} from '@fluentui/react-icons';
import {
  useStorageRoutingRules,
  useUpsertRoutingRule,
  useDeleteRoutingRule,
  useRefreshRoutingRules,
} from '../../hooks/useStorageRouting';
import type { StorageBackend, UpsertRoutingRuleRequest } from '@reportplatform/types';

const SOURCE_TYPES = ['EXCEL', 'PPTX', 'PDF', 'CSV', 'SERVICE_NOW'] as const;
const BACKENDS: StorageBackend[] = ['POSTGRES', 'SPARK', 'BLOB'];

const BACKEND_LABELS: Record<StorageBackend, string> = {
  POSTGRES: 'PostgreSQL (JSONB)',
  SPARK: 'Spark / Delta Lake',
  BLOB: 'Azure Blob JSON',
};

const BACKEND_COLORS: Record<StorageBackend, 'brand' | 'warning' | 'informative'> = {
  POSTGRES: 'brand',
  SPARK: 'warning',
  BLOB: 'informative',
};

const useStyles = makeStyles({
  container: {
    display: 'flex',
    flexDirection: 'column',
    gap: '16px',
  },
  header: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    flexWrap: 'wrap',
    gap: '8px',
  },
  actions: {
    display: 'flex',
    gap: '8px',
  },
  form: {
    display: 'flex',
    flexDirection: 'column',
    gap: '12px',
    marginTop: '8px',
  },
  hint: {
    fontSize: tokens.fontSizeBase200,
    color: tokens.colorNeutralForeground3,
  },
  globalDefault: {
    fontStyle: 'italic',
    color: tokens.colorNeutralForeground3,
  },
  error: {
    color: tokens.colorPaletteRedForeground1,
  },
});

interface RuleFormState {
  orgId: string;
  sourceType: string;
  backend: StorageBackend;
}

const DEFAULT_FORM: RuleFormState = {
  orgId: '',
  sourceType: '',
  backend: 'POSTGRES',
};

/**
 * Admin panel for managing storage routing rules.
 *
 * Allows HOLDING_ADMIN to route specific organisations or source types
 * to POSTGRES, SPARK, or BLOB without a service restart.
 */
export function StorageRoutingPanel() {
  const styles = useStyles();
  const { data: rules, isLoading } = useStorageRoutingRules();
  const upsert = useUpsertRoutingRule();
  const deleteRule = useDeleteRoutingRule();
  const refresh = useRefreshRoutingRules();

  const [dialogOpen, setDialogOpen] = useState(false);
  const [form, setForm] = useState<RuleFormState>(DEFAULT_FORM);
  const [error, setError] = useState<string | null>(null);

  const handleSave = async () => {
    setError(null);
    try {
      const request: UpsertRoutingRuleRequest = {
        orgId: form.orgId.trim() || null,
        sourceType: form.sourceType || null,
        backend: form.backend,
        createdBy: 'admin',
      };
      await upsert.mutateAsync(request);
      setDialogOpen(false);
      setForm(DEFAULT_FORM);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Chyba při ukládání pravidla');
    }
  };

  const handleDelete = async (id: string) => {
    try {
      await deleteRule.mutateAsync(id);
    } catch (e: unknown) {
      // Global default is protected – ignore
    }
  };

  const isGlobalDefault = (rule: { orgId: string | null; sourceType: string | null }) =>
    rule.orgId === null && rule.sourceType === null;

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <Body1>
          Pravidla routování určují, do kterého backendu se ukládají tabulková data pro danou
          organizaci a typ souboru. Pravidla se vyhodnocují od nejspecifičtějšího (org+typ) po
          nejobecnější (globální výchozí).
        </Body1>
        <div className={styles.actions}>
          <Button
            icon={<ArrowSync24Regular />}
            appearance="outline"
            onClick={() => refresh.mutate()}
            disabled={refresh.isPending}
          >
            Obnovit cache
          </Button>
          <Button
            icon={<Add24Regular />}
            appearance="primary"
            onClick={() => {
              setForm(DEFAULT_FORM);
              setError(null);
              setDialogOpen(true);
            }}
          >
            Přidat pravidlo
          </Button>
        </div>
      </div>

      {isLoading && <Spinner label="Načítám pravidla…" />}

      {rules && (
        <Table aria-label="Pravidla routování">
          <TableHeader>
            <TableRow>
              <TableHeaderCell>Organizace</TableHeaderCell>
              <TableHeaderCell>Typ souboru</TableHeaderCell>
              <TableHeaderCell>Backend</TableHeaderCell>
              <TableHeaderCell>Platnost od</TableHeaderCell>
              <TableHeaderCell>Vytvořil</TableHeaderCell>
              <TableHeaderCell>Akce</TableHeaderCell>
            </TableRow>
          </TableHeader>
          <TableBody>
            {rules.map((rule) => (
              <TableRow key={rule.id}>
                <TableCell>
                  {rule.orgId ? (
                    <code>{rule.orgId}</code>
                  ) : (
                    <span className={styles.globalDefault}>— (všechny)</span>
                  )}
                </TableCell>
                <TableCell>
                  {rule.sourceType ?? <span className={styles.globalDefault}>— (všechny)</span>}
                </TableCell>
                <TableCell>
                  <Badge
                    appearance="filled"
                    color={BACKEND_COLORS[rule.backend as StorageBackend] ?? 'subtle'}
                  >
                    {BACKEND_LABELS[rule.backend as StorageBackend] ?? rule.backend}
                  </Badge>
                </TableCell>
                <TableCell>{new Date(rule.effectiveFrom).toLocaleString('cs-CZ')}</TableCell>
                <TableCell>{rule.createdBy ?? '—'}</TableCell>
                <TableCell>
                  {isGlobalDefault(rule) ? (
                    <span className={styles.hint}>Nelze smazat</span>
                  ) : (
                    <Button
                      icon={<Delete24Regular />}
                      appearance="subtle"
                      size="small"
                      onClick={() => handleDelete(rule.id)}
                    />
                  )}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}

      {/* Add rule dialog */}
      <Dialog open={dialogOpen} onOpenChange={(_, d) => setDialogOpen(d.open)}>
        <DialogSurface>
          <DialogTitle>Přidat pravidlo routování</DialogTitle>
          <DialogBody>
            <DialogContent>
              <div className={styles.form}>
                <div>
                  <Label htmlFor="orgId">
                    ID organizace{' '}
                    <span className={styles.hint}>(prázdné = platí pro všechny)</span>
                  </Label>
                  <Input
                    id="orgId"
                    placeholder="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
                    value={form.orgId}
                    onChange={(_, d) => setForm((f) => ({ ...f, orgId: d.value }))}
                  />
                </div>

                <div>
                  <Label htmlFor="sourceType">
                    Typ souboru{' '}
                    <span className={styles.hint}>(prázdné = platí pro všechny typy)</span>
                  </Label>
                  <Dropdown
                    id="sourceType"
                    placeholder="— (všechny typy)"
                    selectedOptions={form.sourceType ? [form.sourceType] : []}
                    onOptionSelect={(_, d) =>
                      setForm((f) => ({
                        ...f,
                        sourceType: d.optionValue === '' ? '' : (d.optionValue ?? ''),
                      }))
                    }
                  >
                    <Option value="">— (všechny typy)</Option>
                    {SOURCE_TYPES.map((t) => (
                      <Option key={t} value={t}>
                        {t}
                      </Option>
                    ))}
                  </Dropdown>
                </div>

                <div>
                  <Label htmlFor="backend">Backend *</Label>
                  <Dropdown
                    id="backend"
                    selectedOptions={[form.backend]}
                    onOptionSelect={(_, d) =>
                      setForm((f) => ({ ...f, backend: (d.optionValue as StorageBackend) ?? 'POSTGRES' }))
                    }
                  >
                    {BACKENDS.map((b) => (
                      <Option key={b} value={b}>
                        {BACKEND_LABELS[b]}
                      </Option>
                    ))}
                  </Dropdown>
                </div>

                {error && <div className={styles.error}>{error}</div>}
              </div>
            </DialogContent>
            <DialogActions>
              <Button appearance="secondary" onClick={() => setDialogOpen(false)}>
                Zrušit
              </Button>
              <Button
                appearance="primary"
                onClick={handleSave}
                disabled={upsert.isPending}
              >
                {upsert.isPending ? <Spinner size="tiny" /> : 'Uložit'}
              </Button>
            </DialogActions>
          </DialogBody>
        </DialogSurface>
      </Dialog>
    </div>
  );
}
