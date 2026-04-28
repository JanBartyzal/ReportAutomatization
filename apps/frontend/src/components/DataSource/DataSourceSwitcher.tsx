import React from 'react';
import {
  Badge,
  Button,
  Spinner,
  Tooltip,
  makeStyles,
  tokens,
  mergeClasses,
} from '@fluentui/react-components';
import {
  DatabaseRegular,
  CloudRegular,
  DocumentDatabaseRegular,
  AppsListDetailRegular,
  CheckmarkCircle16Regular,
  DismissCircle16Regular,
  QuestionCircle16Regular,
} from '@fluentui/react-icons';
import type { DataSourceMode, DataSourceStats } from '@reportplatform/types';

export interface DataSourceSwitcherProps {
  /** Currently selected data source mode */
  value: DataSourceMode;
  /** Called when the user switches the source */
  onChange: (mode: DataSourceMode) => void;
  /** Stats returned from /api/query/sinks/data-source-stats */
  stats?: DataSourceStats;
  /** True while stats are loading */
  loadingStats?: boolean;
}

const useStyles = makeStyles({
  root: {
    display: 'flex',
    alignItems: 'center',
    gap: tokens.spacingHorizontalS,
    flexWrap: 'wrap',
  },
  button: {
    display: 'flex',
    alignItems: 'center',
    gap: tokens.spacingHorizontalXS,
    minWidth: '120px',
    paddingLeft: tokens.spacingHorizontalS,
    paddingRight: tokens.spacingHorizontalS,
  },
  buttonActive: {
    border: `2px solid ${tokens.colorBrandForeground1}`,
    backgroundColor: tokens.colorBrandBackground2,
  },
  label: {
    fontWeight: tokens.fontWeightSemibold,
  },
  count: {
    fontSize: tokens.fontSizeBase200,
    color: tokens.colorNeutralForeground3,
  },
  availabilityIcon: {
    fontSize: '14px',
    marginLeft: '2px',
  },
});

interface SourceOption {
  mode: DataSourceMode;
  label: string;
  icon: React.ReactElement;
  count: (stats: DataSourceStats) => number | null;
  available: (stats: DataSourceStats) => boolean | null;
  tooltip: string;
}

const SOURCE_OPTIONS: SourceOption[] = [
  {
    mode: 'ALL',
    label: 'Vše',
    icon: <AppsListDetailRegular />,
    count: () => null,
    available: () => true,
    tooltip: 'Zobrazit data ze všech zdrojů',
  },
  {
    mode: 'POSTGRES',
    label: 'PostgreSQL',
    icon: <DatabaseRegular />,
    count: (s) => s.postgresRowCount,
    available: () => true,
    tooltip: 'Data uložená jako JSONB v PostgreSQL (výchozí)',
  },
  {
    mode: 'SPARK',
    label: 'Spark / Delta',
    icon: <CloudRegular />,
    count: (s) => (s.sparkRecordCount >= 0 ? s.sparkRecordCount : null),
    available: (s) => s.sparkAvailable,
    tooltip: 'Data zpracovaná externím Spark/ADF pipeline a uložená v Delta Lake (ADLS Gen2)',
  },
  {
    mode: 'BLOB',
    label: 'Blob JSON',
    icon: <DocumentDatabaseRegular />,
    count: (s) => s.blobCount,
    available: (s) => s.blobAvailable,
    tooltip: 'Data serializovaná jako JSON a uložená přímo v Azure Blob Storage',
  },
];

function AvailabilityIcon({ available }: { available: boolean | null }) {
  if (available === null) return <QuestionCircle16Regular />;
  return available ? (
    <CheckmarkCircle16Regular style={{ color: 'green' }} />
  ) : (
    <DismissCircle16Regular style={{ color: 'red' }} />
  );
}

function formatCount(count: number | null): string {
  if (count === null) return '?';
  if (count < 0) return '∞';
  if (count >= 1_000_000) return `${(count / 1_000_000).toFixed(1)}M`;
  if (count >= 1_000) return `${(count / 1_000).toFixed(1)}k`;
  return count.toString();
}

/**
 * Three-way (+ "all") data source toggle for the Sink Browser.
 *
 * Shows per-backend record counts and availability indicators so operators
 * can see at a glance whether Spark or Blob data is accessible.
 */
export function DataSourceSwitcher({
  value,
  onChange,
  stats,
  loadingStats,
}: DataSourceSwitcherProps) {
  const styles = useStyles();

  return (
    <div className={styles.root} role="group" aria-label="Zdroj dat">
      {loadingStats && <Spinner size="tiny" label="Načítám statistiky…" labelPosition="after" />}
      {SOURCE_OPTIONS.map((opt) => {
        const isActive = value === opt.mode;
        const count = stats ? opt.count(stats) : null;
        const available = stats ? opt.available(stats) : null;

        return (
          <Tooltip key={opt.mode} content={opt.tooltip} relationship="description">
            <Button
              appearance={isActive ? 'primary' : 'outline'}
              size="medium"
              className={mergeClasses(styles.button, isActive ? styles.buttonActive : undefined)}
              onClick={() => onChange(opt.mode)}
              icon={opt.icon}
            >
              <span className={styles.label}>{opt.label}</span>
              {stats && count !== null && (
                <Badge
                  size="small"
                  appearance="filled"
                  color={isActive ? 'informative' : 'subtle'}
                >
                  {formatCount(count)}
                </Badge>
              )}
              {stats && opt.mode !== 'ALL' && (
                <span className={styles.availabilityIcon}>
                  <AvailabilityIcon available={available} />
                </span>
              )}
            </Button>
          </Tooltip>
        );
      })}
    </div>
  );
}
