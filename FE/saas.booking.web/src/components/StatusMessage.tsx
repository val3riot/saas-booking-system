import type { ReactNode } from 'react';

type StatusMessageProps = {
  tone?: 'info' | 'success' | 'warning' | 'danger';
  children: ReactNode;
};

const toneClasses = {
  info: 'border-slate-200 text-slate-700',
  success: 'border-emerald-700/25 text-emerald-700',
  warning: 'border-amber-700/25 text-amber-700',
  danger: 'border-red-700/25 text-red-700'
};

export function StatusMessage({ tone = 'info', children }: StatusMessageProps) {
  return (
    <div className={`rounded-lg border bg-white px-4 py-3 ${toneClasses[tone]}`} role="status">
      {children}
    </div>
  );
}
