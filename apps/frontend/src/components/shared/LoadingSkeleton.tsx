/**
 * LoadingSkeleton — unified loading state per design system
 * aria-busy="true", pulse animation (2s infinite)
 * Variants: card, table-row, full-page
 */
import { makeStyles, tokens } from '@fluentui/react-components';

const pulse = {
  '@keyframes pulse': {
    '0%, 100%': { opacity: 1 },
    '50%': { opacity: 0.4 },
  },
} as const;

const useStyles = makeStyles({
  bar: {
    backgroundColor: tokens.colorNeutralBackground3,
    borderRadius: '4px',
    animationName: pulse['@keyframes pulse'] as unknown as string,
    animationDuration: '2s',
    animationIterationFunction: 'ease-in-out',
    animationIterationCount: 'infinite',
  },
  card: {
    backgroundColor: tokens.colorNeutralBackground1,
    borderRadius: '8px',
    border: `1px solid ${tokens.colorNeutralStroke1}`,
    padding: '24px',
    display: 'flex',
    flexDirection: 'column',
    gap: '12px',
  },
  tableRow: {
    display: 'flex',
    gap: '16px',
    padding: '10px 0',
    borderBottom: `1px solid ${tokens.colorNeutralStroke1}`,
  },
  fullPage: {
    display: 'flex',
    flexDirection: 'column',
    gap: '24px',
    padding: '40px 0',
  },
});

function SkeletonBar({ width = '100%', height = '14px' }: { width?: string; height?: string }) {
  const styles = useStyles();
  return <div className={styles.bar} style={{ width, height }} />;
}

export type SkeletonVariant = 'card' | 'table-row' | 'full-page';

export interface LoadingSkeletonProps {
  variant?: SkeletonVariant;
  count?: number;
}

function CardSkeleton() {
  const styles = useStyles();
  return (
    <div className={styles.card}>
      <SkeletonBar width="40%" height="16px" />
      <SkeletonBar width="60%" height="32px" />
      <SkeletonBar width="30%" height="12px" />
    </div>
  );
}

function TableRowSkeleton() {
  const styles = useStyles();
  return (
    <div className={styles.tableRow}>
      <SkeletonBar width="20%" height="14px" />
      <SkeletonBar width="30%" height="14px" />
      <SkeletonBar width="25%" height="14px" />
      <SkeletonBar width="15%" height="14px" />
    </div>
  );
}

function FullPageSkeleton() {
  const styles = useStyles();
  return (
    <div className={styles.fullPage}>
      <SkeletonBar width="30%" height="24px" />
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '24px' }}>
        <CardSkeleton />
        <CardSkeleton />
        <CardSkeleton />
      </div>
      {Array.from({ length: 5 }).map((_, i) => (
        <TableRowSkeleton key={i} />
      ))}
    </div>
  );
}

export function LoadingSkeleton({ variant = 'card', count = 1 }: LoadingSkeletonProps) {
  if (variant === 'full-page') {
    return (
      <div aria-busy="true" aria-label="Loading content">
        <FullPageSkeleton />
      </div>
    );
  }

  const Component = variant === 'table-row' ? TableRowSkeleton : CardSkeleton;

  return (
    <div aria-busy="true" aria-label="Loading content">
      {Array.from({ length: count }).map((_, i) => (
        <Component key={i} />
      ))}
    </div>
  );
}
