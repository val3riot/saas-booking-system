import type { FormEvent } from 'react';
import { FormField } from '../../../components/FormField';
import { Button } from '../../../components/Button';
import type { ProviderProfile } from '../../../types/api';
import type { ProviderProfileFormState } from '../types';

type ProviderProfileSectionProps = {
  form: ProviderProfileFormState;
  isSaving: boolean;
  onChange: (form: ProviderProfileFormState) => void;
  onSubmit: (event: FormEvent) => void;
  profile: ProviderProfile | null;
};

export function ProviderProfileSection({ form, isSaving, onChange, onSubmit, profile }: ProviderProfileSectionProps) {
  return (
    <section className="grid gap-4 rounded-lg border border-slate-200 bg-white p-4">
      <div>
        <p className="mb-1 text-xs font-bold uppercase text-brand-600">Profilo provider</p>
        <h2 className="text-lg font-bold text-slate-950">{profile?.businessName ?? 'Dettagli attivita'}</h2>
        {profile && (
          <p className="text-sm text-slate-500">
            Stato: {profile.active ? 'profilo attivo' : 'profilo non attivo'} · {profile.city}
          </p>
        )}
      </div>

      <form className="grid gap-3" onSubmit={onSubmit}>
        <div className="grid gap-3 md:grid-cols-2">
          <FormField
            label="Nome attivita"
            name="businessName"
            value={form.businessName}
            onChange={(event) => onChange({ ...form, businessName: event.target.value })}
            required
          />
          <FormField
            label="Categoria"
            name="profileCategory"
            value={form.category}
            onChange={(event) => onChange({ ...form, category: event.target.value })}
            required
          />
        </div>
        <div className="grid gap-3 md:grid-cols-2">
          <FormField
            label="Citta"
            name="profileCity"
            value={form.city}
            onChange={(event) => onChange({ ...form, city: event.target.value })}
            required
          />
          <FormField
            label="Indirizzo"
            name="profileAddress"
            value={form.address}
            onChange={(event) => onChange({ ...form, address: event.target.value })}
          />
        </div>
        <FormField
          label="Descrizione"
          name="profileDescription"
          value={form.description}
          onChange={(event) => onChange({ ...form, description: event.target.value })}
          multiline
          rows={3}
        />
        <div>
          <Button type="submit" disabled={isSaving}>
            {isSaving ? 'Salvataggio...' : 'Salva profilo'}
          </Button>
        </div>
      </form>
    </section>
  );
}
