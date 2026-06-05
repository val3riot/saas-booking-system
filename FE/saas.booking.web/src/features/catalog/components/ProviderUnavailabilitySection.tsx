import type { FormEvent } from 'react';
import { Button } from '../../../components/Button';
import { EmptyState } from '../../../components/EmptyState';
import { FormField } from '../../../components/FormField';
import { formatDateTime } from '../../../lib/formatters';
import type { AvailabilityException, OfferedService } from '../../../types/api';
import type { ExceptionFormState } from '../types';

type ProviderUnavailabilitySectionProps = {
  exceptionForm: ExceptionFormState;
  exceptions: AvailabilityException[];
  isSavingException: boolean;
  onCreate: () => void;
  onFormChange: (form: ExceptionFormState) => void;
  onRemove: (exception: AvailabilityException) => void;
  onSelect: (exception: AvailabilityException) => void;
  onSubmit: (event: FormEvent) => void;
  onToggle: (exception: AvailabilityException) => void;
  onToggleService: (serviceId: number) => void;
  selectedException: AvailabilityException | null;
  selectedExceptionId: number | null;
  services: OfferedService[];
};

export function ProviderUnavailabilitySection({
  exceptionForm,
  exceptions,
  isSavingException,
  onCreate,
  onFormChange,
  onRemove,
  onSelect,
  onSubmit,
  onToggle,
  onToggleService,
  selectedException,
  selectedExceptionId,
  services
}: ProviderUnavailabilitySectionProps) {
  return (
    <section className="grid gap-4 rounded-lg border border-slate-200 bg-white p-4">
      <div className="flex flex-col items-start justify-between gap-3 md:flex-row md:items-center">
        <div>
          <p className="mb-1 text-xs font-bold uppercase text-brand-600">Agenda provider</p>
          <h2 className="text-lg font-bold text-slate-950">Indisponibilita</h2>
          <p className="text-sm text-slate-500">
            Ferie, chiusure e assenze rendono gli orari non disponibili nel calendario customer.
          </p>
        </div>
        <Button variant="ghost" type="button" onClick={onCreate}>
          Nuova indisponibilita
        </Button>
      </div>

      <div className="grid gap-5 lg:grid-cols-[minmax(280px,0.9fr)_minmax(0,1.4fr)]">
        <div>
          {exceptions.length === 0 ? (
            <EmptyState title="Nessuna indisponibilita" description="Aggiungi ferie, chiusure o assenze puntuali." />
          ) : (
            <div className="grid gap-2">
              {exceptions.map((exception) => (
                <button
                  className={[
                    'grid w-full gap-2 rounded-md border p-3 text-left transition-colors',
                    selectedExceptionId === exception.id
                      ? 'border-amber-500 bg-amber-50'
                      : 'border-slate-200 bg-white hover:border-slate-300 hover:bg-slate-50'
                  ].join(' ')}
                  key={exception.id}
                  type="button"
                  onClick={() => onSelect(exception)}
                >
                  <span className="flex flex-wrap items-center justify-between gap-3">
                    <strong className="text-slate-950">{exceptionServiceLabel(exception, services)}</strong>
                    <small className={exception.active ? 'text-amber-700' : 'text-slate-500'}>
                      {exception.active ? 'Attiva' : 'Disattivata'}
                    </small>
                  </span>
                  <span className="h-2 rounded-full bg-slate-100">
                    <span
                      className={`block h-2 rounded-full ${exception.active ? 'bg-amber-500' : 'bg-slate-300'}`}
                      style={{ width: exceptionWidth(exception) }}
                    />
                  </span>
                  <span className="text-sm text-slate-500">
                    {formatDateTime(exception.startsAt)} - {formatDateTime(exception.endsAt)}
                  </span>
                  {exception.reason && <span className="text-sm text-slate-500">Motivo: {exception.reason}</span>}
                </button>
              ))}
            </div>
          )}
        </div>

        <form className="grid gap-3" onSubmit={onSubmit}>
          <fieldset className="grid gap-2 rounded-md border border-slate-200 p-3">
            <legend className="px-1 text-sm font-semibold text-slate-950">Applicata a</legend>
            <label className="flex items-center gap-2 text-sm font-semibold text-slate-950">
              <input
                className="size-4 accent-brand-600"
                type="checkbox"
                checked={exceptionForm.appliesToAll}
                onChange={(event) =>
                  onFormChange({
                    ...exceptionForm,
                    appliesToAll: event.target.checked,
                    serviceIds: event.target.checked ? [] : exceptionForm.serviceIds
                  })
                }
              />
              <span>Tutti i servizi</span>
            </label>

            <div className="grid gap-2 md:grid-cols-2">
              {services.map((service) => (
                <label className="flex items-center gap-2 text-sm text-slate-700" key={service.id}>
                  <input
                    className="size-4 accent-brand-600"
                    type="checkbox"
                    checked={exceptionForm.serviceIds.includes(String(service.id))}
                    disabled={exceptionForm.appliesToAll}
                    onChange={() => onToggleService(service.id)}
                  />
                  <span>{service.name}</span>
                </label>
              ))}
            </div>
          </fieldset>

          <div className="grid gap-3 md:grid-cols-2">
            <FormField
              label="Inizio"
              name="exceptionStartsAt"
              type="datetime-local"
              value={exceptionForm.startsAt}
              onChange={(event) => onFormChange({ ...exceptionForm, startsAt: event.target.value })}
              required
            />
            <FormField
              label="Fine"
              name="exceptionEndsAt"
              type="datetime-local"
              value={exceptionForm.endsAt}
              onChange={(event) => onFormChange({ ...exceptionForm, endsAt: event.target.value })}
              required
            />
          </div>

          <FormField
            label="Motivo"
            name="exceptionReason"
            value={exceptionForm.reason}
            onChange={(event) => onFormChange({ ...exceptionForm, reason: event.target.value })}
            placeholder="Ferie, chiusura, imprevisto"
          />

          <div className="flex flex-wrap gap-2">
            <Button type="submit" disabled={isSavingException}>
              {selectedException ? 'Salva indisponibilita' : 'Crea indisponibilita'}
            </Button>
            {selectedException && (
              <>
                <Button
                  variant={selectedException.active ? 'secondary' : 'ghost'}
                  type="button"
                  onClick={() => onToggle(selectedException)}
                >
                  {selectedException.active ? 'Disattiva' : 'Riattiva'}
                </Button>
                <Button variant="ghost" type="button" onClick={() => onRemove(selectedException)}>
                  Rimuovi indisponibilita
                </Button>
              </>
            )}
          </div>
        </form>
      </div>
    </section>
  );
}

function exceptionServiceLabel(exception: AvailabilityException, services: OfferedService[]) {
  if (!exception.serviceId) {
    return 'Tutti i servizi';
  }

  return services.find((service) => service.id === exception.serviceId)?.name ?? `Servizio #${exception.serviceId}`;
}

function exceptionWidth(exception: AvailabilityException) {
  const minutes = Math.max(
    15,
    Math.round((new Date(exception.endsAt).getTime() - new Date(exception.startsAt).getTime()) / 60000)
  );
  return `${Math.min(100, Math.max(14, minutes / 6))}%`;
}
