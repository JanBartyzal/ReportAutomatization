/**
 * Text Template Editor – P8-W4/W6
 * Create or edit a text template with content and data bindings.
 */
import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  makeStyles,
  tokens,
  Button,
  Input,
  Textarea,
  Label,
  Select,
  Option,
  Checkbox,
  MessageBar,
  Spinner,
  Badge,
  Divider,
} from '@fluentui/react-components';
import { ArrowLeft24Regular, Save24Regular, Add24Regular, Delete24Regular } from '@fluentui/react-icons';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  getTextTemplate,
  createTextTemplate,
  updateTextTemplate,
} from '../api/textTemplates';
import { listNamedQueries } from '../api/namedQueries';
import type {
  CreateTextTemplateRequest,
  UpdateTextTemplateRequest,
  BindingEntry,
  OutputFormat,
} from '@reportplatform/types';

const useStyles = makeStyles({
  container: { padding: tokens.spacingHorizontalL, maxWidth: '900px' },
  header: {
    display: 'flex',
    alignItems: 'center',
    gap: tokens.spacingHorizontalM,
    marginBottom: tokens.spacingVerticalM,
  },
  title: { fontSize: tokens.fontSizeBase600, fontWeight: tokens.fontWeightSemibold },
  fieldGroup: {
    display: 'flex',
    flexDirection: 'column',
    gap: tokens.spacingVerticalXS,
    marginBottom: tokens.spacingVerticalM,
  },
  row: { display: 'flex', gap: tokens.spacingHorizontalM, alignItems: 'flex-start' },
  bindingCard: {
    marginBottom: tokens.spacingVerticalS,
    padding: tokens.spacingHorizontalM,
    backgroundColor: tokens.colorNeutralBackground2,
    borderRadius: tokens.borderRadiusMedium,
  },
  bindingRow: {
    display: 'flex',
    gap: tokens.spacingHorizontalS,
    alignItems: 'center',
    flexWrap: 'wrap',
  },
  sectionTitle: {
    fontSize: tokens.fontSizeBase400,
    fontWeight: tokens.fontWeightSemibold,
    marginTop: tokens.spacingVerticalL,
    marginBottom: tokens.spacingVerticalS,
  },
});

const OUTPUT_FORMATS: OutputFormat[] = ['PPTX', 'EXCEL', 'HTML_EMAIL'];

export default function TextTemplateEditorPage() {
  const styles = useStyles();
  const navigate = useNavigate();
  const { templateId } = useParams<{ templateId: string }>();
  const isNew = !templateId || templateId === 'new';
  const qc = useQueryClient();

  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [content, setContent] = useState('');
  const [templateType, setTemplateType] = useState<'MARKDOWN' | 'HTML'>('MARKDOWN');
  const [outputFormats, setOutputFormats] = useState<OutputFormat[]>(['PPTX']);
  const [bindings, setBindings] = useState<BindingEntry[]>([]);
  const [saveError, setSaveError] = useState<string | null>(null);

  const { data: template, isLoading: templateLoading } = useQuery({
    queryKey: ['text-template', templateId],
    queryFn: () => getTextTemplate(templateId!),
    enabled: !isNew,
  });

  const { data: namedQueries = [] } = useQuery({
    queryKey: ['named-queries'],
    queryFn: () => listNamedQueries(),
  });

  useEffect(() => {
    if (template) {
      setName(template.name);
      setDescription(template.description ?? '');
      setContent(template.content);
      setTemplateType(template.templateType);
      setOutputFormats(template.outputFormats);
      setBindings(template.dataBindings?.bindings ?? []);
    }
  }, [template]);

  const createMut = useMutation({
    mutationFn: createTextTemplate,
    onSuccess: (t) => {
      qc.invalidateQueries({ queryKey: ['text-templates'] });
      navigate(`/reporting/text-templates/${t.id}/edit`);
    },
    onError: (e: Error) => setSaveError(e.message),
  });

  const updateMut = useMutation({
    mutationFn: ({ id, req }: { id: string; req: UpdateTextTemplateRequest }) =>
      updateTextTemplate(id, req),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['text-template', templateId] });
      qc.invalidateQueries({ queryKey: ['text-templates'] });
    },
    onError: (e: Error) => setSaveError(e.message),
  });

  function handleSave() {
    setSaveError(null);
    const dataBindings = { bindings };
    if (isNew) {
      const req: CreateTextTemplateRequest = {
        name, description, content, templateType, outputFormats, dataBindings,
      };
      createMut.mutate(req);
    } else {
      updateMut.mutate({
        id: templateId!,
        req: { name, description, content, outputFormats, dataBindings },
      });
    }
  }

  function addBinding() {
    setBindings(prev => [...prev, {
      placeholder: '{{NEW_PLACEHOLDER}}',
      type: 'TABLE',
      queryId: namedQueries[0]?.id ?? '',
      label: '',
      params: {},
    }]);
  }

  function removeBinding(idx: number) {
    setBindings(prev => prev.filter((_, i) => i !== idx));
  }

  function updateBinding(idx: number, patch: Partial<BindingEntry>) {
    setBindings(prev => prev.map((b, i) => i === idx ? { ...b, ...patch } : b));
  }

  function toggleFormat(fmt: OutputFormat) {
    setOutputFormats(prev =>
      prev.includes(fmt) ? prev.filter(f => f !== fmt) : [...prev, fmt],
    );
  }

  if (!isNew && templateLoading) return <Spinner label="Loading template…" />;

  const isSaving = createMut.isPending || updateMut.isPending;

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <Button
          appearance="subtle"
          icon={<ArrowLeft24Regular />}
          onClick={() => navigate('/reporting/text-templates')}
        />
        <span className={styles.title}>
          {isNew ? 'New Text Template' : `Edit: ${template?.name ?? ''}`}
        </span>
        {!isNew && template?.isSystem && (
          <Badge appearance="tint" color="informative">System</Badge>
        )}
      </div>

      {saveError && <MessageBar intent="error" style={{ marginBottom: tokens.spacingVerticalM }}>{saveError}</MessageBar>}

      <div className={styles.fieldGroup}>
        <Label htmlFor="tt-name">Name *</Label>
        <Input id="tt-name" value={name} onChange={(_, d) => setName(d.value)} />
      </div>
      <div className={styles.fieldGroup}>
        <Label htmlFor="tt-desc">Description</Label>
        <Input id="tt-desc" value={description} onChange={(_, d) => setDescription(d.value)} />
      </div>
      <div className={styles.row}>
        <div className={styles.fieldGroup} style={{ flex: 1 }}>
          <Label htmlFor="tt-type">Template Type</Label>
          <Select id="tt-type" value={templateType}
            onChange={(_, d) => setTemplateType(d.value as 'MARKDOWN' | 'HTML')}>
            <Option value="MARKDOWN">Markdown</Option>
            <Option value="HTML">HTML</Option>
          </Select>
        </div>
        <div className={styles.fieldGroup} style={{ flex: 1 }}>
          <Label>Output Formats</Label>
          <div style={{ display: 'flex', gap: tokens.spacingHorizontalS }}>
            {OUTPUT_FORMATS.map(fmt => (
              <Checkbox
                key={fmt}
                label={fmt}
                checked={outputFormats.includes(fmt)}
                onChange={() => toggleFormat(fmt)}
              />
            ))}
          </div>
        </div>
      </div>

      <div className={styles.fieldGroup}>
        <Label htmlFor="tt-content">Template Content *</Label>
        <Textarea
          id="tt-content"
          value={content}
          onChange={(_, d) => setContent(d.value)}
          rows={12}
          style={{ fontFamily: 'monospace', fontSize: tokens.fontSizeBase200 }}
        />
      </div>

      <Divider />
      <div className={styles.sectionTitle}>Data Bindings</div>
      <p style={{ color: tokens.colorNeutralForeground3, fontSize: tokens.fontSizeBase200, marginBottom: tokens.spacingVerticalS }}>
        Use {'{{'} PLACEHOLDER {'}}'}  in content. Each binding resolves via a Named Query.
      </p>

      {bindings.map((b, idx) => (
        <div key={idx} className={styles.bindingCard}>
          <div className={styles.bindingRow}>
            <div style={{ flex: 1 }}>
              <Label>Placeholder</Label>
              <Input
                value={b.placeholder}
                onChange={(_, d) => updateBinding(idx, { placeholder: d.value })}
                style={{ fontFamily: 'monospace', fontSize: tokens.fontSizeBase200 }}
              />
            </div>
            <div style={{ flex: 1 }}>
              <Label>Type</Label>
              <Select value={b.type}
                onChange={(_, d) => updateBinding(idx, { type: d.value as BindingEntry['type'] })}>
                <Option value="TABLE">TABLE</Option>
                <Option value="SCALAR">SCALAR</Option>
                <Option value="CHART">CHART</Option>
              </Select>
            </div>
            <div style={{ flex: 2 }}>
              <Label>Named Query</Label>
              <Select value={b.queryId}
                onChange={(_, d) => updateBinding(idx, { queryId: d.value })}>
                <Option value="">— select —</Option>
                {namedQueries.map(q => (
                  <Option key={q.id} value={q.id}>{q.name}</Option>
                ))}
              </Select>
            </div>
            <div style={{ flex: 1 }}>
              <Label>Label</Label>
              <Input
                value={b.label}
                onChange={(_, d) => updateBinding(idx, { label: d.value })}
              />
            </div>
            <Button
              size="small"
              icon={<Delete24Regular />}
              appearance="subtle"
              onClick={() => removeBinding(idx)}
              style={{ marginTop: 20 }}
            />
          </div>
        </div>
      ))}

      <Button icon={<Add24Regular />} onClick={addBinding} style={{ marginTop: tokens.spacingVerticalXS }}>
        Add Binding
      </Button>

      <Divider style={{ marginTop: tokens.spacingVerticalL }} />

      <div style={{ display: 'flex', justifyContent: 'flex-end', gap: tokens.spacingHorizontalM, marginTop: tokens.spacingVerticalM }}>
        <Button onClick={() => navigate('/reporting/text-templates')}>Cancel</Button>
        <Button
          appearance="primary"
          icon={isSaving ? <Spinner size="tiny" /> : <Save24Regular />}
          disabled={!name || !content || isSaving}
          onClick={handleSave}
        >
          {isSaving ? 'Saving…' : 'Save Template'}
        </Button>
      </div>
    </div>
  );
}
