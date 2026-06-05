type EmptyStateProps = {
  title: string;
  description?: string;
};

export function EmptyState({ title, description }: EmptyStateProps) {
  return (
    <div className="grid gap-1.5 rounded-lg border border-slate-200 bg-white p-5">
      <h2 className="text-base font-bold text-slate-950">{title}</h2>
      {description && <p className="text-slate-500">{description}</p>}
    </div>
  );
}
