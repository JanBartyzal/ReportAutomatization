import { useState, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  makeStyles,
  tokens,
  Button,
  Badge,
  Checkbox,
  Input,
  Dropdown,
  Option,
  Tooltip,
  Dialog,
  DialogTrigger,
  DialogSurface,
  DialogBody,
  DialogTitle,
  DialogContent,
  DialogActions,
} from '@fluentui/react-components';
import {
  ArrowLeft24Regular,
  Edit24Regular,
  Checkmark24Regular,
  Dismiss24Regular,
  Delete24Regular,
  History24Regular,
} from '@fluentui/react-icons';
import { useSinkDetail, useCreateCorrection, useDeleteCorrection, useUpsertSelection } from '../hooks/useSinks';
import LoadingSpinner from '../components/LoadingSpinner';
import { PageHeader } from '../components/shared/PageHeader';
import type { CorrectionType, ErrorCategory, SinkCorrection } from '@reportplatform/types';

const useStyles = makeStyles({
  container: {
    padding: tokens.spacingHorizontalL,
  },
  backButton: {
    marginBottom: tokens.spacingVerticalM,
  },
  meta: {
    display: 'flex',
    gap: tokens.spacingHorizontalXL,
    marginBottom: tokens.spacingVerticalL,
    flexWrap: 'wrap',
    color: tokens.colorNeutralForeground3,
  },
  tableContainer: {
    overflowX: 'auto',
    marginBottom: tokens.spacingVerticalL,
    border: `1px solid ${tokens.colorNeutralStroke1}`,
    borderRadius: tokens.borderRadiusMedium,
  },
  table: {
    width: '100%',
    borderCollapse: 'collapse',
    fontSize: tokens.fontSizeBase200,
  },
  th: {
    padding: `${tokens.spacingVerticalS} ${tokens.spacingHorizontalM}`,
    backgroundColor: tokens.colorNeutralBackground3,
    borderBottom: `2px solid ${tokens.colorNeutralStroke1}`,
    textAlign: 'left',
    fontWeight: tokens.fontWeightSemibold,
    whiteSpace: 'nowrap',
  },
  td: {
    padding: `${tokens.spacingVerticalXS} ${tokens.spacingHorizontalM}`,
    borderBottom: `1px solid ${tokens.colorNeutralStroke2}`,
    cursor: 'pointer',
    ':hover': {
      backgroundColor: tokens.colorNeutralBackground1Hover,
    },
  },
  tdCorrected: {
    padding: `${tokens.spacingVerticalXS} ${tokens.spacingHorizontalM}`,
    borderBottom: `1px solid ${tokens.colorNeutralStroke2}`,
    cursor: 'pointer',
    backgroundColor: tokens.colorPaletteYellowBackground1,
    position: 'relative' as const,
    ':hover': {
      backgroundColor: tokens.colorPaletteYellowBackground2,
    },
  },
  editCell: {
    display: 'flex',
    alignItems: 'center',
    gap: tokens.spacingHorizontalXS,
  },
  selectionPanel: {
    display: 'flex',
    gap: tokens.spacingHorizontalM,
    alignItems: 'center',
    padding: tokens.spacingHorizontalM,
    backgroundColor: tokens.colorNeutralBackground3,
    borderRadius: tokens.borderRadiusMedium,
    marginBottom: tokens.spacingVerticalL,
  },
  correctionHistory: {
    marginTop: tokens.spacingVerticalL,
  },
  correctionItem: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: tokens.spacingVerticalS,
    borderBottom: `1px solid ${tokens.colorNeutralStroke2}`,
  },
});

const ERROR_CATEGORIES: ErrorCategory[] = [
  'MERGED_CELLS', 'WRONG_HEADER', 'MISSING_ROW', 'VALUE_FORMAT', 'SPLIT_TABLE',
];

interface EditingCell {
  rowIndex: number;
  colIndex: number;
  value: string;
  originalValue: string;
}

export default function SinkDetailPage() {
  const styles = useStyles();
  const { sinkId } = useParams<{ sinkId: string }>();
  const navigate = useNavigate();

  const { data: sink, isLoading } = useSinkDetail(sinkId ?? '');
  const createCorrection = useCreateCorrection(sinkId ?? '');
  const deleteCorrectionMut = useDeleteCorrection(sinkId ?? '');
  const upsertSelection = useUpsertSelection(sinkId ?? '');

  const [editing, setEditing] = useState<EditingCell | null>(null);
  const [errorCategory, setErrorCategory] = useState<ErrorCategory | undefined>();
  const [showHistory, setShowHistory] = useState(false);
  const [selectionNote, setSelectionNote] = useState('');

  const headers: string[] = sink?.correctedHeaders ?? sink?.headers ?? [];
  const rows: string[][] = sink?.correctedRows ?? sink?.rows ?? [];

  const isCellCorrected = useCallback(
    (rowIdx: number, colIdx: number) => {
      return sink?.corrections?.some(
        (c) => c.rowIndex === rowIdx && c.colIndex === colIdx && c.correctionType === 'CELL_VALUE'
      ) ?? false;
    },
    [sink?.corrections]
  );

  const getCellCorrection = useCallback(
    (rowIdx: number, colIdx: number): SinkCorrection | undefined => {
      return sink?.corrections?.find(
        (c) => c.rowIndex === rowIdx && c.colIndex === colIdx
      );
    },
    [sink?.corrections]
  );

  const handleCellClick = (rowIndex: number, colIndex: number, value: string) => {
    setEditing({ rowIndex, colIndex, value, originalValue: value });
    setErrorCategory(undefined);
  };

  const handleSaveEdit = () => {
    if (!editing || editing.value === editing.originalValue) {
      setEditing(null);
      return;
    }
    createCorrection.mutate({
      rowIndex: editing.rowIndex,
      colIndex: editing.colIndex,
      originalValue: editing.originalValue,
      correctedValue: editing.value,
      correctionType: 'CELL_VALUE' as CorrectionType,
      errorCategory,
    });
    setEditing(null);
  };

  const handleCancelEdit = () => setEditing(null);

  const handleToggleSelection = (selected: boolean) => {
    upsertSelection.mutate({
      selected,
      priority: 0,
      note: selectionNote || undefined,
    });
  };

  if (isLoading || !sink) return <LoadingSpinner />;

  const isSelected = sink.selections?.some((s) => s.selected) ?? false;

  return (
    <div className={styles.container}>
      <Button
        className={styles.backButton}
        icon={<ArrowLeft24Regular />}
        appearance="subtle"
        onClick={() => navigate('/sinks')}
      >
        Back to Sink Browser
      </Button>

      <PageHeader
        title={`Sink: ${sink.sourceSheet || sink.id.substring(0, 8)}`}
        actions={
          <Button
            icon={<History24Regular />}
            appearance="subtle"
            onClick={() => setShowHistory(!showHistory)}
          >
            {showHistory ? 'Hide' : 'Show'} History ({sink.correctionCount})
          </Button>
        }
      />

      {/* Metadata */}
      <div className={styles.meta}>
        <span>File: {sink.fileId.substring(0, 12)}...</span>
        <span>Sheet: {sink.sourceSheet || '—'}</span>
        <span>{rows.length} rows × {headers.length} columns</span>
        {sink.correctionCount > 0 && (
          <Badge appearance="filled" color="warning">{sink.correctionCount} corrections</Badge>
        )}
      </div>

      {/* Selection Panel */}
      <div className={styles.selectionPanel}>
        <Checkbox
          checked={isSelected}
          onChange={(_, d) => handleToggleSelection(!!d.checked)}
          label="Include this sink in report output"
        />
        <Input
          placeholder="Note (optional)..."
          size="small"
          value={selectionNote}
          onChange={(_, d) => setSelectionNote(d.value)}
          style={{ flex: 1, maxWidth: 300 }}
        />
        {isSelected && <Badge appearance="filled" color="success">Selected</Badge>}
      </div>

      {/* Data Table with inline editing */}
      <div className={styles.tableContainer}>
        <table className={styles.table}>
          <thead>
            <tr>
              <th className={styles.th}>#</th>
              {headers.map((h, ci) => (
                <th key={ci} className={styles.th}>{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {rows.map((row, ri) => (
              <tr key={ri}>
                <td className={styles.td} style={{ color: tokens.colorNeutralForeground3 }}>{ri + 1}</td>
                {row.map((cell, ci) => {
                  const corrected = isCellCorrected(ri, ci);
                  const correction = corrected ? getCellCorrection(ri, ci) : undefined;
                  const isEditing = editing?.rowIndex === ri && editing?.colIndex === ci;

                  return (
                    <td
                      key={ci}
                      className={corrected ? styles.tdCorrected : styles.td}
                      onClick={() => !isEditing && handleCellClick(ri, ci, cell)}
                    >
                      {isEditing ? (
                        <div className={styles.editCell}>
                          <Input
                            size="small"
                            value={editing.value}
                            onChange={(_, d) => setEditing({ ...editing, value: d.value })}
                            autoFocus
                            onKeyDown={(e) => {
                              if (e.key === 'Enter') handleSaveEdit();
                              if (e.key === 'Escape') handleCancelEdit();
                            }}
                          />
                          <Dropdown
                            size="small"
                            placeholder="Error type"
                            onOptionSelect={(_, d) => setErrorCategory(d.optionValue as ErrorCategory)}
                            style={{ minWidth: 120 }}
                          >
                            {ERROR_CATEGORIES.map((cat) => (
                              <Option key={cat} value={cat}>{cat}</Option>
                            ))}
                          </Dropdown>
                          <Button size="small" icon={<Checkmark24Regular />} appearance="primary" onClick={handleSaveEdit} />
                          <Button size="small" icon={<Dismiss24Regular />} appearance="subtle" onClick={handleCancelEdit} />
                        </div>
                      ) : (
                        <Tooltip
                          content={
                            correction
                              ? `Original: ${correction.originalValue} | By: ${correction.correctedBy}`
                              : 'Click to edit'
                          }
                          relationship="description"
                        >
                          <span>
                            {cell}
                            {corrected && <Edit24Regular style={{ marginLeft: 4, fontSize: 12, opacity: 0.6 }} />}
                          </span>
                        </Tooltip>
                      )}
                    </td>
                  );
                })}
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Correction History */}
      {showHistory && sink.corrections.length > 0 && (
        <div className={styles.correctionHistory}>
          <h3>Correction History</h3>
          {sink.corrections.map((c) => (
            <div key={c.id} className={styles.correctionItem}>
              <div>
                <Badge appearance="outline" size="small">{c.correctionType}</Badge>
                {' '}Row {c.rowIndex ?? '—'}, Col {c.colIndex ?? '—'}:
                <span style={{ textDecoration: 'line-through', marginLeft: 4 }}>{c.originalValue}</span>
                {' → '}
                <strong>{c.correctedValue}</strong>
                <span style={{ marginLeft: 8, color: tokens.colorNeutralForeground3 }}>
                  by {c.correctedBy} at {new Date(c.correctedAt).toLocaleString()}
                </span>
              </div>
              <Dialog>
                <DialogTrigger>
                  <Button size="small" icon={<Delete24Regular />} appearance="subtle" />
                </DialogTrigger>
                <DialogSurface>
                  <DialogBody>
                    <DialogTitle>Revert Correction</DialogTitle>
                    <DialogContent>
                      Are you sure you want to revert this correction? The cell will return to its original value.
                    </DialogContent>
                    <DialogActions>
                      <DialogTrigger disableButtonEnhancement>
                        <Button appearance="secondary">Cancel</Button>
                      </DialogTrigger>
                      <Button
                        appearance="primary"
                        onClick={() => deleteCorrectionMut.mutate(c.id)}
                      >
                        Revert
                      </Button>
                    </DialogActions>
                  </DialogBody>
                </DialogSurface>
              </Dialog>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
