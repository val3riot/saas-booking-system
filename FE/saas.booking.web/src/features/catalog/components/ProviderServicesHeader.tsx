import { Button } from '../../../components/Button';

type ProviderServicesHeaderProps = {
  activeExceptionCount: number;
  activeServiceCount: number;
  onCreateService: () => void;
  totalServiceCount: number;
};

export function ProviderServicesHeader({
  activeExceptionCount,
  activeServiceCount,
  onCreateService,
  totalServiceCount
}: ProviderServicesHeaderProps) {
  return (
    <section className="flex flex-col items-start justify-between gap-4 md:flex-row md:items-center">
      <div>
        <p className="mb-1 text-xs font-bold uppercase text-brand-600">Provider</p>
        <h1 className="text-2xl font-bold leading-tight text-slate-950">I miei servizi</h1>
      </div>
      <div className="flex flex-wrap items-center gap-2">
        <span className="rounded-full border border-slate-200 bg-white px-3 py-1.5 text-sm text-slate-700">
          {activeServiceCount} attivi / {totalServiceCount} totali
        </span>
        <span className="rounded-full border border-amber-200 bg-amber-50 px-3 py-1.5 text-sm text-amber-700">
          {activeExceptionCount} indisponibilita attive
        </span>
        <Button type="button" onClick={onCreateService}>
          Nuovo servizio
        </Button>
      </div>
    </section>
  );
}
