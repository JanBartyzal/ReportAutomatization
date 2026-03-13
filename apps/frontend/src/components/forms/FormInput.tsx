/**
 * FormInput — text/number input with consistent states per design system
 * Default border: Stroke1, Hover: Brand80, Focus: Brand90 2px
 * Error: Red border, Disabled: 50% opacity
 */
import { Input, type InputProps } from '@fluentui/react-components';
import { FormField } from './FormField';

export interface FormInputProps extends Omit<InputProps, 'size'> {
  label: string;
  required?: boolean;
  hint?: string;
  error?: string;
}

export function FormInput({
  label,
  required,
  hint,
  error,
  ...inputProps
}: FormInputProps) {
  return (
    <FormField label={label} required={required} hint={hint} error={error}>
      <Input {...inputProps} />
    </FormField>
  );
}
