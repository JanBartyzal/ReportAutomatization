/**
 * FormDatePicker — date picker with project tokens per design system
 */
import { Input, type InputProps } from '@fluentui/react-components';
import { FormField } from './FormField';

export interface FormDatePickerProps extends Omit<InputProps, 'type' | 'size'> {
  label: string;
  required?: boolean;
  hint?: string;
  error?: string;
}

export function FormDatePicker({
  label,
  required,
  hint,
  error,
  ...inputProps
}: FormDatePickerProps) {
  return (
    <FormField label={label} required={required} hint={hint} error={error}>
      <Input type="date" {...inputProps} />
    </FormField>
  );
}
