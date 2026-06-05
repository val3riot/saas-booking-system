import type { InputHTMLAttributes, TextareaHTMLAttributes } from 'react';

type BaseProps = {
  label: string;
  error?: string;
};

type InputProps = BaseProps &
  InputHTMLAttributes<HTMLInputElement> & {
    multiline?: false;
  };

type TextareaProps = BaseProps &
  TextareaHTMLAttributes<HTMLTextAreaElement> & {
    multiline: true;
  };

type FormFieldProps = InputProps | TextareaProps;

export function FormField(props: FormFieldProps) {
  const { label, error, multiline, ...fieldProps } = props;
  const fieldId = props.id ?? props.name ?? label;
  const controlClass =
    'min-h-10 w-full rounded-md border border-slate-200 bg-white px-3 py-2 text-slate-950 outline-none transition focus:border-brand-600 focus:ring-4 focus:ring-brand-600/15';

  return (
    <label className="grid gap-1.5 text-sm font-semibold text-slate-950" htmlFor={fieldId}>
      <span>{label}</span>
      {multiline ? (
        <textarea
          className={`${controlClass} resize-y`}
          id={fieldId}
          {...(fieldProps as TextareaHTMLAttributes<HTMLTextAreaElement>)}
        />
      ) : (
        <input className={controlClass} id={fieldId} {...(fieldProps as InputHTMLAttributes<HTMLInputElement>)} />
      )}
      {error && <small className="text-red-700">{error}</small>}
    </label>
  );
}
