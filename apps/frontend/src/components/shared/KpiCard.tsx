/**
 * KpiCard — standardized KPI display per design system
 * Padding: spacingL (24px), Shadow: Level 1, Hover: Level 2
 */
import React from 'react';
import { makeStyles, tokens, mergeClasses } from '@fluentui/react-components';
import {
  ArrowTrending24Regular,
  ArrowTrendingDown24Regular,
  LineHorizontal124Regular,
} from '@fluentui/react-icons';
import { elevation } from '../../theme/tokens';

const useStyles = makeStyles({
  root: {
    backgroundColor: tokens.colorNeutralBackground1,
    borderRadius: '8px',
    border: `1px solid ${tokens.colorNeutralStroke1}`,
    padding: '24px',
    boxShadow: elevation.level1,
    transitionProperty: 'box-shadow',
    transitionDuration: '200ms',
    transitionTimingFunction: 'ease-in-out',
    minWidth: '240px',
    ':hover': {
      boxShadow: elevation.level2,
    },
  },
  title: {
    fontSize: '16px',
    fontWeight: '600',
    lineHeight: '1.5',
    color: tokens.colorNeutralForeground1,
    marginBottom: '8px',
  },
  valueRow: {
    display: 'flex',
    alignItems: 'baseline',
    gap: '12px',
  },
  value: {
    fontSize: '32px',
    fontWeight: '700',
    lineHeight: '1.2',
    color: tokens.colorNeutralForeground1,
    fontVariantNumeric: 'tabular-nums',
  },
  trend: {
    display: 'inline-flex',
    alignItems: 'center',
    gap: '4px',
    fontSize: '12px',
    fontWeight: '600',
  },
  trendUp: {
    color: tokens.colorStatusSuccessForeground1,
  },
  trendDown: {
    color: tokens.colorStatusDangerForeground1,
  },
  trendFlat: {
    color: tokens.colorNeutralForeground3,
  },
  subtitle: {
    fontSize: '12px',
    lineHeight: '1.5',
    color: tokens.colorNeutralForeground3,
    marginTop: '4px',
  },
  sparkline: {
    marginTop: '12px',
  },
});

export interface KpiCardProps {
  title: string;
  value: string | number;
  trend?: 'up' | 'down' | 'flat';
  trendLabel?: string;
  subtitle?: string;
  sparklineData?: React.ReactNode;
}

export function KpiCard({ title, value, trend, trendLabel, subtitle, sparklineData }: KpiCardProps) {
  const styles = useStyles();

  const trendIcon = trend === 'up'
    ? <ArrowTrending24Regular />
    : trend === 'down'
      ? <ArrowTrendingDown24Regular />
      : <LineHorizontal124Regular />;

  const trendClass = trend === 'up'
    ? styles.trendUp
    : trend === 'down'
      ? styles.trendDown
      : styles.trendFlat;

  return (
    <div className={styles.root}>
      <div className={styles.title}>{title}</div>
      <div className={styles.valueRow}>
        <span className={styles.value}>{value}</span>
        {trend && (
          <span className={mergeClasses(styles.trend, trendClass)}>
            {trendIcon}
            {trendLabel}
          </span>
        )}
      </div>
      {subtitle && <div className={styles.subtitle}>{subtitle}</div>}
      {sparklineData && <div className={styles.sparkline}>{sparklineData}</div>}
    </div>
  );
}
