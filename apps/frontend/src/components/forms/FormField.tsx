/**
 * FormField — wrapper around Fluent <Field> per design system
 * Label always above input, required marker *, hint text, error message
 * Field spacing: spacingM (16px) vertical
 */
import { Field, makeStyles, tokens } from '@fluentui/react-components';
import { ErrorCircle16Regular } from '@fluentui/react-icons';
import type { ReactNode } from 'react';

const useStyles = makeStyles({
  root: {
    marginBottom: '16px',
  },
  hint: {
    fontSize: '12px',
    lineHeight: '1.5',
    color: tokens.colorNeutralForeground3,
    marginTop: '4px',
  },
  error: {
    fontSize: '12px',
    lineHeight: '1.5',
    color: tokens.colorStatusDangerForeground1,
    marginTop: '4px',
    display: 'flex',
    alignItems: 'center',
    gap: '4px',
  },
});

export interface FormFieldProps {
  label: string;
  required?: boolean;
  hint?: string;
  error?: string;
  children: ReactNode;
  htmlFor?: string;
}

export function FormField({
  label,
  required = false,
  hint,
  error,
  children,
}: FormFieldProps) {
  const styles = useStyles();

  return (
    <div className={styles.root}>
      <Field
        label={required ? `${label} *` : label}
        validationState={error ? 'error' : 'none'}
        validationMessage={
          error ? (
            <span className={styles.error}>
              <ErrorCircle16Regular />
              {error}
            </span>
          ) : undefined
        }
      >
        {children}
      </Field>
      {hint && !error && <div className={styles.hint}>{hint}</div>}
    </div>
  );
}
