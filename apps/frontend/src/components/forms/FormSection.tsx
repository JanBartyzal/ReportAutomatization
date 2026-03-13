/**
 * FormSection — visual grouping with title and description per design system
 */
import { makeStyles, tokens } from '@fluentui/react-components';
import type { ReactNode } from 'react';

const useStyles = makeStyles({
  root: {
    marginBottom: '32px',
  },
  title: {
    fontSize: '20px',
    fontWeight: '600',
    lineHeight: '1.4',
    color: tokens.colorNeutralForeground1,
    marginTop: 0,
    marginBottom: '4px',
  },
  description: {
    fontSize: '14px',
    lineHeight: '1.5',
    color: tokens.colorNeutralForeground3,
    marginTop: 0,
    marginBottom: '16px',
  },
  separator: {
    borderTop: `1px solid ${tokens.colorNeutralStroke1}`,
    paddingTop: '24px',
  },
});

export interface FormSectionProps {
  title: string;
  description?: string;
  children: ReactNode;
  separator?: boolean;
}

export function FormSection({
  title,
  description,
  children,
  separator = false,
}: FormSectionProps) {
  const styles = useStyles();

  return (
    <div className={`${styles.root} ${separator ? styles.separator : ''}`}>
      <h2 className={styles.title}>{title}</h2>
      {description && <p className={styles.description}>{description}</p>}
      {children}
    </div>
  );
}
