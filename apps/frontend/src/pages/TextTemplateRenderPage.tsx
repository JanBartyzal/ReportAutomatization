/**
 * Text Template Render – P8-W4/W6
 * Select output format, provide runtime input params, and download rendered output.
 */
import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  makeStyles,
  tokens,
  Button,
  Label,
  Select,
  Option,
  Input,
  MessageBar,
  Spinner,
  Badge,
} from '@fluentui/react-components';
import {
  ArrowLeft24Regular,
  DocumentArrowDown24Regular,
  PlayRegular,
} from '@fluentui/react-icons';
import { useQuery, useMutation } from '@tanstack/react-query';
import { getTextTemplate, renderTextTemplate } from '../api/textTemplates';
import type { OutputFormat, RenderRequest, RenderResponse } from '@reportplatform/types';

const useStyles = makeStyles({
  container: { padding: tokens.spacingHorizontalL, maxWidth: '700px' },
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
  resultCard: {
    marginTop: tokens.spacingVerticalL,
    padding: tokens.spacingHorizontalL,
    backgroundColor: tokens.colorNeutralBackground2,
    borderRadius: tokens.borderRadiusMedium,
    display: 'flex',
    flexDirection: 'column',
    gap: tokens.spacingVerticalS,
    alignItems: 'flex-start',
  },
  hint: {
    fontSize: tokens.fontSizeBase200,
    color: tokens.colorNeutralForeground3,
    marginBottom: tokens.spacingVerticalS,
  },
  paramRow: {
    display: 'flex',
    gap: tokens.spacingHorizontalS,
    alignItems: 'center',
    marginBottom: tokens.spacingVerticalXS,
  },
});

export default function TextTemplateRenderPage() {
  const styles = useStyles();
  const navigate = useNavigate();
  const { templateId } = useParams<{ templateId: string }>();

  const [outputFormat, setOutputFormat] = useState<OutputFormat>('PPTX');
  const [inputParams, setInputParams] = useState<Record<string, string>>({});
  const [renderResult, setRenderResult] = useState<RenderResponse | null>(null);
  const [renderError, setRenderError] = useState<string | null>(null);

  const { data: template, isLoading } = useQuery({
    queryKey: ['text-template', templateId],
    queryFn: () => getTextTemplate(templateId!),
    enabled: !!templateId,
  });

  const renderMut = useMutation({
    mutationFn: (req: RenderRequest) => renderTextTemplate(templateId!, req),
    onSuccess: (result) => { setRenderResult(result); setRenderError(null); },
    onError: (e: Error) => { setRenderError(e.message); setRenderResult(null); },
  });

  if (isLoading) return <Spinner label="Loading template…" />;
  if (!template) return <MessageBar intent="error">Template not found.</MessageBar>;

  const formats = template.outputFormats;

  function handleRender() {
    const params: Record<string, unknown> = {};
    Object.entries(inputParams).forEach(([k, v]) => { if (v) params[k] = v; });
    renderMut.mutate({ outputFormat, inputParams: params });
  }

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <Button
          appearance="subtle"
          icon={<ArrowLeft24Regular />}
          onClick={() => navigate('/reporting/text-templates')}
        />
        <span className={styles.title}>Render: {template.name}</span>
        {template.isSystem && <Badge appearance="tint" color="informative">System</Badge>}
      </div>

      <p className={styles.hint}>{template.description}</p>

      <div className={styles.fieldGroup}>
        <Label htmlFor="fmt">Output Format</Label>
        <Select id="fmt" value={outputFormat}
          onChange={(_, d) => setOutputFormat(d.value as OutputFormat)}>
          {formats.map(f => <Option key={f} value={f}>{f}</Option>)}
        </Select>
      </div>

      {/* Render input placeholders extracted from bindings params */}
      {template.dataBindings?.bindings?.length > 0 && (
        <div>
          <Label style={{ marginBottom: tokens.spacingVerticalXS }}>Runtime Parameters</Label>
          <p className={styles.hint}>
            Parameters referenced as {'{{'} input.X {'}} '} in binding params.
          </p>
          {Array.from(new Set(
            template.dataBindings.bindings.flatMap(b =>
              Object.values(b.params ?? {})
                .map(v => String(v).match(/\{\{input\.([^}]+)\}\}/)?.[1])
                .filter(Boolean) as string[],
            ),
          )).map(paramName => (
            <div key={paramName} className={styles.paramRow}>
              <Label style={{ minWidth: 120 }}>{paramName}</Label>
              <Input
                placeholder={`Value for ${paramName}`}
                value={inputParams[paramName] ?? ''}
                onChange={(_, d) => setInputParams(p => ({ ...p, [paramName]: d.value }))}
                style={{ flex: 1 }}
              />
            </div>
          ))}
        </div>
      )}

      {renderError && (
        <MessageBar intent="error" style={{ marginTop: tokens.spacingVerticalM }}>
          {renderError}
        </MessageBar>
      )}

      <Button
        appearance="primary"
        icon={renderMut.isPending ? <Spinner size="tiny" /> : <PlayRegular />}
        disabled={renderMut.isPending}
        onClick={handleRender}
        style={{ marginTop: tokens.spacingVerticalM }}
      >
        {renderMut.isPending ? 'Rendering…' : 'Generate'}
      </Button>

      {renderResult && (
        <div className={styles.resultCard}>
          <p style={{ color: tokens.colorNeutralForeground2, fontWeight: tokens.fontWeightSemibold }}>
            Report generated successfully
          </p>
          <p className={styles.hint}>
            Format: {renderResult.outputFormat} — {new Date(renderResult.renderedAt).toLocaleString()}
          </p>
          <Button
            as="a"
            href={renderResult.downloadUrl}
            target="_blank"
            rel="noreferrer"
            appearance="primary"
            icon={<DocumentArrowDown24Regular />}
          >
            Download
          </Button>
        </div>
      )}
    </div>
  );
}
