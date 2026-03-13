/**
 * EmptyState — no-data placeholder per design system
 * Fluent icon + Title 3 message + Body 1 description + optional CTA button
 */
import { makeStyles, tokens, Button } from '@fluentui/react-components';
import { Info24Regular } from '@fluentui/react-icons';
import type { ReactNode } from 'react';

const useStyles = makeStyles({
  root: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    padding: '48px 24px',
    textAlign: 'center',
    gap: '8px',
  },
  icon: {
    fontSize: '48px',
    color: tokens.colorNeutralForeground3,
    marginBottom: '8px',
  },
  title: {
    fontSize: '16px',
    fontWeight: '600',
    lineHeight: '1.5',
    color: tokens.colorNeutralForeground1,
    margin: 0,
  },
  description: {
    fontSize: '14px',
    lineHeight: '1.5',
    color: tokens.colorNeutralForeground3,
    maxWidth: '400px',
    margin: 0,
  },
  cta: {
    marginTop: '16px',
  },
});

export interface EmptyStateProps {
  icon?: ReactNode;
  title: string;
  description?: string;
  ctaLabel?: string;
  onCtaClick?: () => void;
}

export function EmptyState({
  icon,
  title,
  description,
  ctaLabel,
  onCtaClick,
}: EmptyStateProps) {
  const styles = useStyles();

  return (
    <div className={styles.root}>
      <div className={styles.icon}>{icon ?? <Info24Regular />}</div>
      <h3 className={styles.title}>{title}</h3>
      {description && <p className={styles.description}>{description}</p>}
      {ctaLabel && onCtaClick && (
        <div className={styles.cta}>
          <Button appearance="primary" onClick={onCtaClick}>
            {ctaLabel}
          </Button>
        </div>
      )}
    </div>
  );
}
