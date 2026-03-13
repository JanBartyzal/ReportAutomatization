/**
 * PageHeader — consistent page title area per design system
 * Breadcrumb + Title (Title 1) + optional action buttons
 * Spacing: spacingXXL top, spacingXL bottom
 */
import { makeStyles, tokens, Body1 } from '@fluentui/react-components';
import type { ReactNode } from 'react';

const useStyles = makeStyles({
  root: {
    paddingTop: '40px',
    paddingBottom: '32px',
  },
  breadcrumb: {
    fontSize: '12px',
    color: tokens.colorNeutralForeground3,
    marginBottom: '8px',
    display: 'flex',
    alignItems: 'center',
    gap: '4px',
  },
  titleRow: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: '16px',
  },
  title: {
    fontSize: '24px',
    fontWeight: '600',
    lineHeight: '1.3',
    color: tokens.colorNeutralForeground1,
    margin: 0,
  },
  actions: {
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
    flexShrink: 0,
  },
  subtitle: {
    color: tokens.colorNeutralForeground2,
    marginTop: '4px',
  },
});

export interface PageHeaderProps {
  title: string;
  subtitle?: string;
  breadcrumb?: ReactNode;
  actions?: ReactNode;
}

export function PageHeader({ title, subtitle, breadcrumb, actions }: PageHeaderProps) {
  const styles = useStyles();

  return (
    <div className={styles.root}>
      {breadcrumb && <div className={styles.breadcrumb}>{breadcrumb}</div>}
      <div className={styles.titleRow}>
        <h1 className={styles.title}>{title}</h1>
        {actions && <div className={styles.actions}>{actions}</div>}
      </div>
      {subtitle && <Body1 className={styles.subtitle}>{subtitle}</Body1>}
    </div>
  );
}
