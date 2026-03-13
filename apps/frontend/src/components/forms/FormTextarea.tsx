/**
 * FormTextarea — multi-line input with same state rules per design system
 */
import { Textarea, type TextareaProps } from '@fluentui/react-components';
import { FormField } from './FormField';

export interface FormTextareaProps extends Omit<TextareaProps, 'size'> {
  label: string;
  required?: boolean;
  hint?: string;
  error?: string;
}

export function FormTextarea({
  label,
  required,
  hint,
  error,
  ...textareaProps
}: FormTextareaProps) {
  return (
    <FormField label={label} required={required} hint={hint} error={error}>
      <Textarea {...textareaProps} />
    </FormField>
  );
}
