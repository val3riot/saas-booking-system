import type { ButtonHTMLAttributes } from 'react';

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: 'primary' | 'secondary' | 'ghost';
  fullWidth?: boolean;
};

const variants = {
  primary: 'border-transparent bg-brand-600 text-white hover:bg-brand-700',
  secondary: 'border-transparent bg-slate-700 text-white hover:bg-slate-800',
  ghost: 'border-slate-200 bg-white text-slate-900 hover:bg-slate-50'
};

export function Button({ className = '', fullWidth = false, variant = 'primary', ...props }: ButtonProps) {
  return (
    <button
      className={[
        'min-h-10 rounded-md border px-3 py-2 font-bold transition-colors',
        variants[variant],
        fullWidth ? 'w-full' : '',
        className
      ].join(' ')}
      {...props}
    />
  );
}
