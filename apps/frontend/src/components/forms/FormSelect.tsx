/**
 * FormSelect — dropdown with consistent styling per design system
 */
import {
  Select,
  type SelectProps,
} from '@fluentui/react-components';
import { FormField } from './FormField';

export interface FormSelectOption {
  value: string;
  label: string;
}

export interface FormSelectProps extends Omit<SelectProps, 'size'> {
  label: string;
  required?: boolean;
  hint?: string;
  error?: string;
  options: FormSelectOption[];
  placeholder?: string;
}

export function FormSelect({
  label,
  required,
  hint,
  error,
  options,
  placeholder,
  ...selectProps
}: FormSelectProps) {
  return (
    <FormField label={label} required={required} hint={hint} error={error}>
      <Select {...selectProps}>
        {placeholder && (
          <option value="" disabled>
            {placeholder}
          </option>
        )}
        {options.map((opt) => (
          <option key={opt.value} value={opt.value}>
            {opt.label}
          </option>
        ))}
      </Select>
    </FormField>
  );
}
