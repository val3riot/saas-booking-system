import { EmptyState } from '../../../components/EmptyState';
import { formatCurrency } from '../../../lib/formatters';
import type { OfferedService } from '../../../types/api';

type ServiceListProps = {
  availabilityCounts: Record<number, { active: number; total: number }>;
  isLoading: boolean;
  onSelect: (service: OfferedService) => void;
  selectedServiceId: number | null;
  services: OfferedService[];
};

export function ServiceList({
  availabilityCounts,
  isLoading,
  onSelect,
  selectedServiceId,
  services
}: ServiceListProps) {
  return (
    <section className="min-h-[420px] rounded-lg border border-slate-200 bg-white p-4">
      {services.length === 0 && !isLoading ? (
        <EmptyState title="Nessun servizio" description="Crea il primo servizio prenotabile dal pannello a destra." />
      ) : (
        <div className="grid gap-2">
          {services.map((service) => {
            const ruleCount = availabilityCounts[service.id] ?? { active: 0, total: 0 };

            return (
              <button
                className={[
                  'grid w-full gap-1 rounded-md border p-3 text-left transition-colors',
                  selectedServiceId === service.id
                    ? 'border-brand-600 bg-teal-50'
                    : 'border-slate-200 bg-white hover:border-slate-300 hover:bg-slate-50'
                ].join(' ')}
                key={service.id}
                type="button"
                onClick={() => onSelect(service)}
              >
                <span className="flex items-start justify-between gap-3">
                  <strong className="text-slate-950">{service.name}</strong>
                  <small className={service.active ? 'text-emerald-700' : 'text-slate-500'}>
                    {service.active ? 'Attivo' : 'Dismesso'}
                  </small>
                </span>
                <span className="text-sm text-slate-500">
                  {service.durationMinutes} min · {formatCurrency(service.priceCents)}
                </span>
                <span className={ruleCount.active > 0 ? 'text-sm text-emerald-700' : 'text-sm text-amber-700'}>
                  {ruleCount.active} regole attive / {ruleCount.total} totali
                </span>
              </button>
            );
          })}
        </div>
      )}
    </section>
  );
}
