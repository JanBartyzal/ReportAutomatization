/**
 * Text Template List – P8-W4/W6
 * Lists all accessible text templates (system + org) with links to editor and renderer.
 */
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  makeStyles,
  tokens,
  Button,
  Input,
  Badge,
  Spinner,
  Table,
  TableHeader,
  TableHeaderCell,
  TableBody,
  TableRow,
  TableCell,
  TableCellLayout,
} from '@fluentui/react-components';
import { AddRegular, OpenRegular, PlayRegular } from '@fluentui/react-icons';
import { useQuery } from '@tanstack/react-query';
import { listTextTemplates } from '../api/textTemplates';
import type { TextTemplate } from '@reportplatform/types';

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
  actions: { display: 'flex', gap: tokens.spacingHorizontalXS },
});

export default function TextTemplateListPage() {
  const styles = useStyles();
  const navigate = useNavigate();
  const [search, setSearch] = useState('');

  const { data: templates = [], isLoading } = useQuery({
    queryKey: ['text-templates'],
    queryFn: listTextTemplates,
  });

  const filtered = templates.filter(t =>
    t.name.toLowerCase().includes(search.toLowerCase()) ||
    (t.description ?? '').toLowerCase().includes(search.toLowerCase()),
  );

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <span className={styles.title}>Text Templates</span>
        <Button
          appearance="primary"
          icon={<AddRegular />}
          onClick={() => navigate('/reporting/text-templates/new')}
        >
          New Template
        </Button>
      </div>

      <div className={styles.filters}>
        <Input
          placeholder="Search templates…"
          value={search}
          onChange={(_, d) => setSearch(d.value)}
          style={{ width: 280 }}
        />
      </div>

      {isLoading && <Spinner label="Loading templates…" />}

      {!isLoading && (
        <Table aria-label="Text templates">
          <TableHeader>
            <TableRow>
              <TableHeaderCell>Name</TableHeaderCell>
              <TableHeaderCell>Type</TableHeaderCell>
              <TableHeaderCell>Output Formats</TableHeaderCell>
              <TableHeaderCell>Bindings</TableHeaderCell>
              <TableHeaderCell>Actions</TableHeaderCell>
            </TableRow>
          </TableHeader>
          <TableBody>
            {filtered.map((t: TextTemplate) => (
              <TableRow key={t.id}>
                <TableCell>
                  <TableCellLayout>
                    {t.name}
                    {t.isSystem && (
                      <Badge appearance="tint" color="informative" style={{ marginLeft: 6 }}>
                        System
                      </Badge>
                    )}
                  </TableCellLayout>
                </TableCell>
                <TableCell>
                  <Badge appearance="outline">{t.templateType}</Badge>
                </TableCell>
                <TableCell>
                  <TableCellLayout>{t.outputFormats.join(', ')}</TableCellLayout>
                </TableCell>
                <TableCell>
                  <TableCellLayout>
                    {t.dataBindings?.bindings?.length ?? 0} binding(s)
                  </TableCellLayout>
                </TableCell>
                <TableCell>
                  <div className={styles.actions}>
                    <Button
                      size="small"
                      icon={<OpenRegular />}
                      onClick={() => navigate(`/reporting/text-templates/${t.id}/edit`)}
                    >
                      Edit
                    </Button>
                    <Button
                      size="small"
                      icon={<PlayRegular />}
                      appearance="primary"
                      onClick={() => navigate(`/reporting/text-templates/${t.id}/render`)}
                    >
                      Render
                    </Button>
                  </div>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}
    </div>
  );
}
