import { Button } from '../../../components/Button';
import { EmptyState } from '../../../components/EmptyState';
import { FormField } from '../../../components/FormField';
import { Modal } from '../../../components/Modal';
import { StatusMessage } from '../../../components/StatusMessage';
import { useAuth } from '../../../features/auth/hooks/useAuth';
import { bookingSlotLabel, formatClock, formatCurrency, formatDateTime } from '../../../lib/formatters';
import type { BookingSlot, CatalogProvider, CatalogService } from '../../../types/api';
import { useCustomerCatalog } from '../hooks/useCustomerCatalog';
import { ProviderServicesPage } from './ProviderServicesPage';

export function CatalogPage() {
  const { session } = useAuth();

  if (session?.role === 'PROVIDER') {
    return <ProviderServicesPage />;
  }

  return <CustomerCatalogPage />;
}

function CustomerCatalogPage() {
  const catalog = useCustomerCatalog();

  return (
    <div className="mx-auto grid max-w-6xl gap-5">
      <section className="flex flex-col items-start justify-between gap-4 md:flex-row md:items-center">
        <div>
          <p className="mb-1 text-xs font-bold uppercase text-brand-600">Catalogo</p>
          <h1 className="text-2xl font-bold leading-tight text-slate-950">Provider e servizi</h1>
        </div>
        <span className="rounded-full border border-slate-200 bg-white px-3 py-1.5 text-sm text-slate-700">
          {catalog.result?.totalElements ?? 0} risultati
        </span>
      </section>

      <form
        className="grid items-end gap-3 rounded-lg border border-slate-200 bg-white p-4 lg:grid-cols-[1.3fr_1fr_1fr_0.9fr_auto]"
        onSubmit={catalog.handleSearch}
      >
        <FormField
          label="Ricerca"
          name="query"
          value={catalog.filters.query}
          onChange={(event) => catalog.setFilters({ ...catalog.filters, query: event.target.value })}
          placeholder="Nome o descrizione"
        />
        <FormField
          label="Categoria"
          name="category"
          value={catalog.filters.category}
          onChange={(event) => catalog.setFilters({ ...catalog.filters, category: event.target.value })}
        />
        <FormField
          label="Citta'"
          name="city"
          value={catalog.filters.city}
          onChange={(event) => catalog.setFilters({ ...catalog.filters, city: event.target.value })}
        />
        <FormField
          label="Data"
          name="availableOn"
          type="date"
          value={catalog.filters.availableOn}
          onChange={(event) => catalog.setFilters({ ...catalog.filters, availableOn: event.target.value })}
        />
        <Button type="submit" disabled={catalog.isLoading}>
          Cerca
        </Button>
      </form>

      {catalog.error && <StatusMessage tone="danger">{catalog.error}</StatusMessage>}
      {catalog.message && <StatusMessage tone="success">{catalog.message}</StatusMessage>}

      <div className="grid gap-5 lg:grid-cols-[minmax(280px,0.9fr)_minmax(0,1.4fr)]">
        <section className="min-h-[420px] rounded-lg border border-slate-200 bg-white p-4">
          {!catalog.result || catalog.result.content.length === 0 ? (
            <EmptyState title="Nessun provider" description="Modifica i filtri o riprova con una ricerca piu' ampia." />
          ) : (
            <>
              <div className="grid gap-2">
                {catalog.result.content.map((provider) => (
                  <button
                    className={[
                      'grid w-full gap-1 rounded-md border p-3 text-left transition-colors',
                      catalog.selectedProvider?.id === provider.id
                        ? 'border-brand-600 bg-teal-50'
                        : 'border-slate-200 bg-white hover:border-slate-300 hover:bg-slate-50'
                    ].join(' ')}
                    key={provider.id}
                    type="button"
                    onClick={() => void catalog.selectProvider(provider)}
                  >
                    <strong className="text-slate-950">{provider.businessName}</strong>
                    <span className="text-sm text-slate-500">
                      {provider.category} · {provider.city}
                    </span>
                  </button>
                ))}
              </div>
              <div className="mt-4 flex items-center justify-between gap-3 text-slate-500">
                <Button
                  variant="ghost"
                  type="button"
                  disabled={catalog.result.first}
                  onClick={() => void catalog.loadProviders(catalog.result!.page - 1)}
                >
                  Indietro
                </Button>
                <span className="text-sm">
                  Pagina {catalog.result.page + 1} di {Math.max(catalog.result.totalPages, 1)}
                </span>
                <Button
                  variant="ghost"
                  type="button"
                  disabled={catalog.result.last}
                  onClick={() => void catalog.loadProviders(catalog.result!.page + 1)}
                >
                  Avanti
                </Button>
              </div>
            </>
          )}
        </section>

        <section className="min-h-[420px] rounded-lg border border-slate-200 bg-white p-4">
          {!catalog.selectedProvider ? (
            <EmptyState
              title="Seleziona un provider"
              description="I servizi e gli orari disponibili appariranno qui."
            />
          ) : (
            <div className="grid gap-4">
              <div>
                <p className="mb-1 text-xs font-bold uppercase text-brand-600">{catalog.selectedProvider.category}</p>
                <h2 className="text-lg font-bold text-slate-950">{catalog.selectedProvider.businessName}</h2>
                <p className="text-slate-500">
                  {catalog.selectedProvider.city}
                  {catalog.selectedProvider.address ? ` · ${catalog.selectedProvider.address}` : ''}
                </p>
                {catalog.selectedProvider.description && <p>{catalog.selectedProvider.description}</p>}
              </div>

              {catalog.services.length === 0 ? (
                <EmptyState title="Nessun servizio" description="Questo provider non ha servizi prenotabili." />
              ) : (
                <>
                  <div className="grid gap-3 md:grid-cols-[minmax(0,1fr)_180px]">
                    <label className="grid gap-1.5 text-sm font-semibold text-slate-950">
                      <span>Servizio</span>
                      <select
                        className="min-h-10 w-full rounded-md border border-slate-200 bg-white px-3 py-2 text-slate-950 outline-none transition focus:border-brand-600 focus:ring-4 focus:ring-brand-600/15"
                        value={catalog.selectedServiceId ?? ''}
                        onChange={(event) => catalog.handleServiceChange(event.target.value)}
                      >
                        {catalog.services.map((service) => (
                          <option key={service.id} value={service.id}>
                            {service.name} · {service.durationMinutes} min · {formatCurrency(service.priceCents)}
                          </option>
                        ))}
                      </select>
                    </label>

                    <FormField
                      label="Giorno"
                      name="bookingDate"
                      type="date"
                      value={catalog.bookingDate}
                      onChange={(event) => catalog.setBookingDate(event.target.value || catalog.today)}
                    />
                  </div>

                  {catalog.selectedService && catalog.selectedService.description && (
                    <p className="text-slate-500">{catalog.selectedService.description}</p>
                  )}

                  <Button
                    variant="secondary"
                    type="button"
                    onClick={() => void catalog.loadSlots()}
                    disabled={!catalog.selectedServiceId || catalog.isLoadingSlots}
                  >
                    {catalog.isLoadingSlots ? 'Caricamento orari...' : 'Aggiorna orari'}
                  </Button>

                  <SlotList
                    slots={catalog.slots}
                    isLoading={catalog.isLoadingSlots}
                    disabled={catalog.isBookingSlot}
                    onSelect={(slot) => catalog.setSlotToConfirm(slot)}
                  />
                </>
              )}
            </div>
          )}
        </section>
      </div>

      {catalog.slotToConfirm && catalog.selectedProvider && catalog.selectedService && (
        <BookingConfirmModal
          provider={catalog.selectedProvider}
          service={catalog.selectedService}
          slot={catalog.slotToConfirm}
          isSaving={catalog.isBookingSlot}
          onCancel={() => catalog.setSlotToConfirm(null)}
          onConfirm={() => void catalog.bookSlot(catalog.slotToConfirm!)}
        />
      )}
    </div>
  );
}

function SlotList({
  slots,
  isLoading,
  disabled,
  onSelect
}: {
  slots: BookingSlot[];
  isLoading: boolean;
  disabled: boolean;
  onSelect: (slot: BookingSlot) => void;
}) {
  if (isLoading) {
    return <EmptyState title="Orari in caricamento" description="Sto aggiornando gli orari disponibili." />;
  }

  if (slots.length === 0) {
    return <EmptyState title="Nessun orario disponibile" description="Prova un altro servizio o un altro giorno." />;
  }

  return (
    <div className="grid grid-cols-[repeat(auto-fill,minmax(110px,1fr))] gap-2">
      {slots.map((slot) => (
        <button
          key={slot.startsAt}
          className={[
            'grid gap-0.5 rounded-md border p-3 text-left transition-colors',
            slot.status === 'AVAILABLE'
              ? 'border-brand-600/45 bg-white hover:bg-teal-50'
              : slot.status === 'BLOCKED'
                ? 'border-amber-200 bg-amber-50'
                : 'border-slate-200 bg-slate-100'
          ].join(' ')}
          type="button"
          disabled={disabled || slot.status !== 'AVAILABLE'}
          onClick={() => onSelect(slot)}
        >
          <span className="font-semibold text-slate-950">{formatClock(slot.startsAt)}</span>
          <small className={slot.status === 'BLOCKED' ? 'text-amber-700' : 'text-slate-500'}>
            {bookingSlotLabel(slot.status)}
          </small>
        </button>
      ))}
    </div>
  );
}

function BookingConfirmModal({
  provider,
  service,
  slot,
  isSaving,
  onCancel,
  onConfirm
}: {
  provider: CatalogProvider;
  service: CatalogService;
  slot: BookingSlot;
  isSaving: boolean;
  onCancel: () => void;
  onConfirm: () => void;
}) {
  return (
    <Modal
      title={service.name}
      eyebrow="Conferma prenotazione"
      description={provider.businessName}
      onClose={onCancel}
      footer={
        <div className="flex flex-wrap justify-end gap-2">
          <Button variant="ghost" type="button" onClick={onCancel} disabled={isSaving}>
            Indietro
          </Button>
          <Button type="button" onClick={onConfirm} disabled={isSaving}>
            {isSaving ? 'Invio...' : 'Invia richiesta'}
          </Button>
        </div>
      }
    >
      <dl className="grid gap-2 rounded-md border border-slate-200 bg-slate-50 p-3 text-sm">
        <div className="flex justify-between gap-3">
          <dt className="text-slate-500">Quando</dt>
          <dd className="text-right font-semibold text-slate-950">
            {formatDateTime(slot.startsAt)} - {formatClock(slot.endsAt)}
          </dd>
        </div>
        <div className="flex justify-between gap-3">
          <dt className="text-slate-500">Durata</dt>
          <dd className="font-semibold text-slate-950">{service.durationMinutes} min</dd>
        </div>
        <div className="flex justify-between gap-3">
          <dt className="text-slate-500">Prezzo</dt>
          <dd className="font-semibold text-slate-950">{formatCurrency(service.priceCents)}</dd>
        </div>
      </dl>

      <StatusMessage tone="warning">
        Dopo l'invio la richiesta sara in attesa e l'orario restera occupato finche il provider la conferma o la
        rifiuta.
      </StatusMessage>
    </Modal>
  );
}
