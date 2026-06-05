import type { FormEvent } from 'react';
import { Button } from '../../../components/Button';
import { EmptyState } from '../../../components/EmptyState';
import { FormField } from '../../../components/FormField';
import { StatusMessage } from '../../../components/StatusMessage';
import type { Availability, OfferedService } from '../../../types/api';
import type { AvailabilityFormState, ServiceFormState } from '../types';

const weekDays = [
  { value: '1', label: 'Lunedi' },
  { value: '2', label: 'Martedi' },
  { value: '3', label: 'Mercoledi' },
  { value: '4', label: 'Giovedi' },
  { value: '5', label: 'Venerdi' },
  { value: '6', label: 'Sabato' },
  { value: '7', label: 'Domenica' }
];

type ServiceEditorSectionProps = {
  availabilities: Availability[];
  availabilityForm: AvailabilityFormState;
  isSaving: boolean;
  isSavingAvailability: boolean;
  onAvailabilityFormChange: (form: AvailabilityFormState) => void;
  onAvailabilitySubmit: (event: FormEvent) => void;
  onCreateAvailability: () => void;
  onFormChange: (form: ServiceFormState) => void;
  onRemoveAvailability: (availability: Availability) => void;
  onRemoveService: (service: OfferedService) => void;
  onSelectAvailability: (availability: Availability) => void;
  onServiceSubmit: (event: FormEvent) => void;
  onToggleAvailability: (availability: Availability) => void;
  onToggleService: (service: OfferedService) => void;
  selectedAvailability: Availability | null;
  selectedAvailabilityId: number | null;
  selectedService: OfferedService | null;
  serviceForm: ServiceFormState;
  serviceRuleCount: { active: number; total: number };
};

export function ServiceEditorSection({
  availabilities,
  availabilityForm,
  isSaving,
  isSavingAvailability,
  onAvailabilityFormChange,
  onAvailabilitySubmit,
  onCreateAvailability,
  onFormChange,
  onRemoveAvailability,
  onRemoveService,
  onSelectAvailability,
  onServiceSubmit,
  onToggleAvailability,
  onToggleService,
  selectedAvailability,
  selectedAvailabilityId,
  selectedService,
  serviceForm,
  serviceRuleCount
}: ServiceEditorSectionProps) {
  return (
    <section className="min-h-[420px] rounded-lg border border-slate-200 bg-white p-4">
      <form className="grid gap-4" onSubmit={onServiceSubmit}>
        <div>
          <p className="mb-1 text-xs font-bold uppercase text-brand-600">
            {selectedService ? 'Modifica servizio' : 'Nuovo servizio'}
          </p>
          <h2 className="text-lg font-bold text-slate-950">
            {selectedService ? selectedService.name : 'Dettagli servizio'}
          </h2>
          {selectedService && (
            <p className="text-sm text-slate-500">
              Stato: {selectedService.active ? 'servizio attivo' : 'servizio dismesso'} · {serviceRuleCount.active}{' '}
              regole attive
            </p>
          )}
        </div>

        <FormField
          label="Nome"
          name="name"
          value={serviceForm.name}
          onChange={(event) => onFormChange({ ...serviceForm, name: event.target.value })}
          placeholder="Prima visita"
          required
        />
        <FormField
          label="Descrizione"
          name="description"
          value={serviceForm.description}
          onChange={(event) => onFormChange({ ...serviceForm, description: event.target.value })}
          multiline
          rows={4}
        />
        <div className="grid gap-3 md:grid-cols-2">
          <FormField
            label="Durata minuti"
            name="durationMinutes"
            type="number"
            min={1}
            value={serviceForm.durationMinutes}
            onChange={(event) => onFormChange({ ...serviceForm, durationMinutes: event.target.value })}
            required
          />
          <FormField
            label="Prezzo EUR"
            name="price"
            type="number"
            min={0}
            step="0.01"
            value={serviceForm.price}
            onChange={(event) => onFormChange({ ...serviceForm, price: event.target.value })}
            required
          />
        </div>

        <div className="flex flex-wrap gap-2">
          <Button type="submit" disabled={isSaving}>
            {selectedService ? 'Salva modifiche' : 'Crea servizio'}
          </Button>
          {selectedService && (
            <>
              <Button
                variant={selectedService.active ? 'secondary' : 'ghost'}
                type="button"
                onClick={() => onToggleService(selectedService)}
              >
                {selectedService.active ? 'Dismetti' : 'Riattiva'}
              </Button>
              <Button variant="ghost" type="button" onClick={() => onRemoveService(selectedService)}>
                Rimuovi dal catalogo
              </Button>
            </>
          )}
        </div>
      </form>

      {selectedService && (
        <div className="mt-6 grid gap-4 border-t border-slate-200 pt-5">
          {serviceRuleCount.active === 0 && (
            <StatusMessage tone="danger">
              Questo servizio non sara visibile ai customer finche non avra almeno una disponibilita attiva.
            </StatusMessage>
          )}

          <div className="flex flex-col items-start justify-between gap-3 md:flex-row md:items-center">
            <div>
              <p className="mb-1 text-xs font-bold uppercase text-brand-600">Disponibilita</p>
              <h3 className="text-base font-bold text-slate-950">Regole settimanali</h3>
            </div>
            <Button variant="ghost" type="button" onClick={onCreateAvailability}>
              Nuova regola
            </Button>
          </div>

          {availabilities.length === 0 ? (
            <EmptyState
              title="Nessuna disponibilita"
              description="Aggiungi giorni e orari per generare appuntamenti prenotabili."
            />
          ) : (
            <div className="grid gap-2">
              {availabilities.map((availability) => (
                <button
                  className={[
                    'grid w-full gap-1 rounded-md border p-3 text-left transition-colors',
                    selectedAvailabilityId === availability.id
                      ? 'border-brand-600 bg-teal-50'
                      : 'border-slate-200 bg-white hover:border-slate-300 hover:bg-slate-50'
                  ].join(' ')}
                  key={availability.id}
                  type="button"
                  onClick={() => onSelectAvailability(availability)}
                >
                  <span className="flex flex-wrap items-center justify-between gap-3">
                    <strong className="text-slate-950">{dayLabel(availability.dayOfWeek)}</strong>
                    <small className={availability.active ? 'text-emerald-700' : 'text-slate-500'}>
                      {availability.active ? 'Attiva' : 'Disattivata'}
                    </small>
                  </span>
                  <span className="text-sm text-slate-500">
                    {formatTime(availability.startTime)} - {formatTime(availability.endTime)}
                  </span>
                </button>
              ))}
            </div>
          )}

          <form className="grid gap-3" onSubmit={onAvailabilitySubmit}>
            <label className="grid gap-1.5 text-sm font-semibold text-slate-950">
              <span>Giorno</span>
              <select
                className="min-h-10 w-full rounded-md border border-slate-200 bg-white px-3 py-2 text-slate-950 outline-none transition focus:border-brand-600 focus:ring-4 focus:ring-brand-600/15"
                value={availabilityForm.dayOfWeek}
                onChange={(event) => onAvailabilityFormChange({ ...availabilityForm, dayOfWeek: event.target.value })}
              >
                {weekDays.map((day) => (
                  <option key={day.value} value={day.value}>
                    {day.label}
                  </option>
                ))}
              </select>
            </label>

            <div className="grid gap-3 md:grid-cols-2">
              <FormField
                label="Inizio"
                name="startTime"
                type="time"
                value={availabilityForm.startTime}
                onChange={(event) => onAvailabilityFormChange({ ...availabilityForm, startTime: event.target.value })}
                required
              />
              <FormField
                label="Fine"
                name="endTime"
                type="time"
                value={availabilityForm.endTime}
                onChange={(event) => onAvailabilityFormChange({ ...availabilityForm, endTime: event.target.value })}
                required
              />
            </div>

            <div className="flex flex-wrap gap-2">
              <Button type="submit" disabled={isSavingAvailability}>
                {selectedAvailability ? 'Salva regola' : 'Crea regola'}
              </Button>
              {selectedAvailability && (
                <>
                  <Button
                    variant={selectedAvailability.active ? 'secondary' : 'ghost'}
                    type="button"
                    onClick={() => onToggleAvailability(selectedAvailability)}
                  >
                    {selectedAvailability.active ? 'Disattiva' : 'Riattiva'}
                  </Button>
                  <Button variant="ghost" type="button" onClick={() => onRemoveAvailability(selectedAvailability)}>
                    Rimuovi regola
                  </Button>
                </>
              )}
            </div>
          </form>
        </div>
      )}
    </section>
  );
}

function dayLabel(dayOfWeek: number) {
  return weekDays.find((day) => Number(day.value) === dayOfWeek)?.label ?? `Giorno ${dayOfWeek}`;
}

function formatTime(value: string) {
  return value.slice(0, 5);
}
