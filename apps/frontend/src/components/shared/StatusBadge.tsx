/**
 * Unified StatusBadge — JSON-driven status colors
 * Replaces per-page badge implementations.
 * Colors sourced from theme-config.json via statusColors.ts
 */
import { makeStyles, tokens } from '@fluentui/react-components';
import { getStatusColors, getStatusLabel } from '../../theme/statusColors';

const useStyles = makeStyles({
  root: {
    display: 'inline-flex',
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: tokens.borderRadiusMedium,
    fontWeight: tokens.fontWeightSemibold,
    whiteSpace: 'nowrap',
  },
  sm: {
    height: '20px',
    fontSize: '10px',
    paddingLeft: tokens.spacingHorizontalSNudge,
    paddingRight: tokens.spacingHorizontalSNudge,
  },
  md: {
    height: '24px',
    fontSize: '12px',
    paddingLeft: tokens.spacingHorizontalS,
    paddingRight: tokens.spacingHorizontalS,
  },
});

export interface StatusBadgeProps {
  status: string;
  size?: 'sm' | 'md';
}

export function StatusBadge({ status, size = 'md' }: StatusBadgeProps) {
  const styles = useStyles();
  const colors = getStatusColors(status);
  const label = getStatusLabel(status);

  return (
    <span
      className={`${styles.root} ${styles[size]}`}
      style={{ backgroundColor: colors.bg, color: colors.text }}
    >
      {label}
    </span>
  );
}
