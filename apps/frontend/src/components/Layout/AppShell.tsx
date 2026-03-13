/**
 * AppShell — main layout wrapper per design system
 * Grid: 12 columns, gap 24px, max-width 1400px centered
 * Uses useBreakpoint() hook for responsive behavior
 */
import { makeStyles, tokens } from '@fluentui/react-components';
import type { ReactNode } from 'react';

const useStyles = makeStyles({
  root: {
    flex: 1,
    padding: tokens.spacingHorizontalL,
    maxWidth: '1400px',
    width: '100%',
    margin: '0 auto',
    boxSizing: 'border-box',
  },
  grid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(12, 1fr)',
    gap: '24px',
  },
});

export interface AppShellProps {
  children: ReactNode;
  grid?: boolean;
}

export function AppShell({ children, grid = false }: AppShellProps) {
  const styles = useStyles();

  if (grid) {
    return (
      <div className={styles.root}>
        <div className={styles.grid}>{children}</div>
      </div>
    );
  }

  return <div className={styles.root}>{children}</div>;
}
