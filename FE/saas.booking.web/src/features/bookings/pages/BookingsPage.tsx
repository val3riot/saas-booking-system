import { FormEvent, useState } from 'react';
import { Button } from '../../../components/Button';
import { EmptyState } from '../../../components/EmptyState';
import { Modal } from '../../../components/Modal';
import { StatusMessage } from '../../../components/StatusMessage';
import { useAuth } from '../../../features/auth/hooks/useAuth';
import { bookingStatusClass, bookingStatusLabel, formatClock, formatDateTime } from '../../../lib/formatters';
import type { Booking } from '../../../types/api';
import { useCustomerBookings } from '../hooks/useCustomerBookings';
import { useProviderAgenda } from '../hooks/useProviderAgenda';

export function BookingsPage() {
  const { session } = useAuth();

  if (session?.role === 'PROVIDER') {
    return <ProviderBookingsPage />;
  }

  return <CustomerBookingsPage />;
}

function CustomerBookingsPage() {
  const bookingsState = useCustomerBookings();

  return (
    <>
      <BookingsLayout
        title="Prenotazioni"
        eyebrow="Customer"
        emptyDescription="Quando prenoti un orario dal catalogo, lo troverai qui."
        bookings={bookingsState.bookings}
        error={bookingsState.error}
        message={bookingsState.message}
        isLoading={bookingsState.isLoading}
        onRefresh={bookingsState.loadBookings}
        headerControls={
          <label className="grid gap-1.5 text-sm font-semibold text-slate-950 sm:min-w-56">
            <span>Ordina per</span>
            <select
              className="min-h-10 rounded-md border border-slate-200 bg-white px-3 py-2 outline-none transition focus:border-brand-600 focus:ring-4 focus:ring-brand-600/15"
              value={bookingsState.sort}
              onChange={(event) => bookingsState.setSort(event.target.value as typeof bookingsState.sort)}
            >
              <option value="startsAtAsc">Data più vicina</option>
              <option value="startsAtDesc">Data più lontana</option>
              <option value="createdAtDesc">Creazione più recente</option>
              <option value="createdAtAsc">Creazione meno recente</option>
            </select>
          </label>
        }
        metaText={(booking) => booking.providerBusinessName}
        onOpenDetails={bookingsState.setSelectedBooking}
        renderActions={(booking) => (
          <Button
            variant="ghost"
            type="button"
            disabled={!['PENDING', 'CONFIRMED'].includes(booking.status)}
            onClick={() => bookingsState.setAction({ booking, type: 'cancel' })}
          >
            Annulla
          </Button>
        )}
      />
      {bookingsState.action && (
        <ReasonModal
          title="Annulla prenotazione"
          description="Indica un motivo per tenere traccia dell'annullamento."
          confirmLabel="Annulla prenotazione"
          isOptional
          onCancel={() => bookingsState.setAction(null)}
          onConfirm={(reason) => void bookingsState.cancel(bookingsState.action!.booking, reason)}
        />
      )}
      {bookingsState.selectedBooking && (
        <BookingDetailModal
          booking={bookingsState.selectedBooking}
          metaText={bookingsState.selectedBooking.providerBusinessName}
          onClose={() => bookingsState.setSelectedBooking(null)}
        />
      )}
    </>
  );
}

function ProviderBookingsPage() {
  const agenda = useProviderAgenda();

  return (
    <>
      <form
        className="mx-auto mb-5 grid max-w-6xl items-end gap-3 rounded-lg border border-slate-200 bg-white p-4 md:grid-cols-[1fr_1fr_auto]"
        onSubmit={agenda.submitFilters}
      >
        <label className="grid gap-1.5 text-sm font-semibold text-slate-950">
          <span>Dal</span>
          <input
            className="min-h-10 rounded-md border border-slate-200 px-3 py-2 outline-none transition focus:border-brand-600 focus:ring-4 focus:ring-brand-600/15"
            type="date"
            value={agenda.from}
            onChange={(event) => agenda.setFrom(event.target.value || agenda.today)}
          />
        </label>
        <label className="grid gap-1.5 text-sm font-semibold text-slate-950">
          <span>Al</span>
          <input
            className="min-h-10 rounded-md border border-slate-200 px-3 py-2 outline-none transition focus:border-brand-600 focus:ring-4 focus:ring-brand-600/15"
            type="date"
            value={agenda.to}
            onChange={(event) => agenda.setTo(event.target.value || agenda.today)}
          />
        </label>
        <Button type="submit" disabled={agenda.isLoading}>
          Filtra agenda
        </Button>
      </form>
      <BookingsLayout
        title="Agenda prenotazioni"
        eyebrow="Provider"
        emptyDescription="Nessuna prenotazione nel periodo selezionato."
        bookings={agenda.bookings}
        error={agenda.error}
        message={agenda.message}
        isLoading={agenda.isLoading}
        onRefresh={agenda.loadBookings}
        metaText={(booking) => `Customer #${booking.customerId}`}
        onOpenDetails={agenda.setSelectedBooking}
        renderActions={(booking) => (
          <div className="flex flex-wrap gap-2">
            <Button type="button" disabled={booking.status !== 'PENDING'} onClick={() => void agenda.confirm(booking)}>
              Conferma
            </Button>
            <Button
              variant="secondary"
              type="button"
              disabled={booking.status !== 'PENDING'}
              onClick={() => agenda.setAction({ booking, type: 'reject' })}
            >
              Rifiuta
            </Button>
            <Button
              variant="ghost"
              type="button"
              disabled={!['PENDING', 'CONFIRMED'].includes(booking.status)}
              onClick={() => agenda.setAction({ booking, type: 'cancel' })}
            >
              Annulla
            </Button>
          </div>
        )}
      />
      {agenda.action && (
        <ReasonModal
          title={agenda.action.type === 'reject' ? 'Rifiuta prenotazione' : 'Annulla prenotazione'}
          description={
            agenda.action.type === 'reject'
              ? 'Indica il motivo del rifiuto per renderlo visibile nello storico.'
              : "Indica un motivo per tenere traccia dell'annullamento."
          }
          confirmLabel={agenda.action.type === 'reject' ? 'Rifiuta' : 'Annulla prenotazione'}
          isOptional
          onCancel={() => agenda.setAction(null)}
          onConfirm={(reason) => {
            if (agenda.action!.type === 'reject') {
              void agenda.reject(agenda.action!.booking, reason);
              return;
            }
            void agenda.cancel(agenda.action!.booking, reason);
          }}
        />
      )}
      {agenda.selectedBooking && (
        <BookingDetailModal
          booking={agenda.selectedBooking}
          metaText={`Customer #${agenda.selectedBooking.customerId}`}
          onClose={() => agenda.setSelectedBooking(null)}
        />
      )}
    </>
  );
}

type BookingsLayoutProps = {
  title: string;
  eyebrow: string;
  emptyDescription: string;
  bookings: Booking[];
  error: string | null;
  message: string | null;
  isLoading: boolean;
  onRefresh: () => Promise<void>;
  headerControls?: React.ReactNode;
  metaText: (booking: Booking) => string;
  onOpenDetails: (booking: Booking) => void;
  renderActions: (booking: Booking) => React.ReactNode;
};

function BookingsLayout({
  title,
  eyebrow,
  emptyDescription,
  bookings,
  error,
  message,
  isLoading,
  onRefresh,
  headerControls,
  metaText,
  onOpenDetails,
  renderActions
}: BookingsLayoutProps) {
  return (
    <div className="mx-auto grid max-w-6xl gap-5">
      <section className="flex flex-col items-start justify-between gap-4 md:flex-row md:items-center">
        <div>
          <p className="mb-1 text-xs font-bold uppercase text-brand-600">{eyebrow}</p>
          <h1 className="text-2xl font-bold leading-tight text-slate-950">{title}</h1>
        </div>
        <div className="flex w-full flex-col items-stretch gap-3 sm:w-auto sm:flex-row sm:items-end">
          {headerControls}
          <Button variant="ghost" type="button" onClick={() => void onRefresh()} disabled={isLoading}>
            Aggiorna
          </Button>
        </div>
      </section>

      {error && <StatusMessage tone="danger">{error}</StatusMessage>}
      {message && <StatusMessage tone="success">{message}</StatusMessage>}

      {bookings.length === 0 ? (
        <EmptyState title="Nessuna prenotazione" description={emptyDescription} />
      ) : (
        <div className="grid gap-3">
          {bookings.map((booking) => (
            <article
              className="flex flex-col justify-between gap-4 rounded-lg border border-slate-200 bg-white p-4 md:flex-row md:items-center"
              key={booking.id}
            >
              <div>
                <div className="flex flex-wrap items-center gap-2">
                  <h2 className="text-base font-bold text-slate-950">{booking.serviceName}</h2>
                  <span
                    className={`rounded-full border border-slate-200 bg-white px-3 py-1.5 text-xs font-semibold ${bookingStatusClass(booking.status)}`}
                  >
                    {bookingStatusLabel(booking.status)}
                  </span>
                </div>
                <p className="text-slate-500">{metaText(booking)}</p>
                <p>
                  {formatDateTime(booking.startsAt)} - {formatClock(booking.endsAt)}
                </p>
                {booking.cancellationReason && (
                  <p className="text-sm text-slate-500">Motivo: {booking.cancellationReason}</p>
                )}
              </div>

              <div className="flex flex-wrap gap-2">
                <Button variant="ghost" type="button" onClick={() => onOpenDetails(booking)}>
                  Dettagli
                </Button>
                {renderActions(booking)}
              </div>
            </article>
          ))}
        </div>
      )}
    </div>
  );
}

function ReasonModal({
  title,
  description,
  confirmLabel,
  isOptional,
  onCancel,
  onConfirm
}: {
  title: string;
  description: string;
  confirmLabel: string;
  isOptional?: boolean;
  onCancel: () => void;
  onConfirm: (reason: string) => void;
}) {
  const [reason, setReason] = useState('');

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    onConfirm(reason.trim());
  }

  return (
    <Modal
      title={title}
      eyebrow="Motivo"
      description={description}
      onClose={onCancel}
      footer={
        <div className="flex flex-wrap justify-end gap-2">
          <Button variant="ghost" type="button" onClick={onCancel}>
            Indietro
          </Button>
          <Button type="submit" form="reason-form">
            {confirmLabel}
          </Button>
        </div>
      }
    >
      <form id="reason-form" className="grid gap-4" onSubmit={handleSubmit}>
        <label className="grid gap-1.5 text-sm font-semibold text-slate-950">
          <span>Motivo {isOptional ? 'opzionale' : ''}</span>
          <textarea
            className="min-h-28 rounded-md border border-slate-200 px-3 py-2 outline-none transition focus:border-brand-600 focus:ring-4 focus:ring-brand-600/15"
            value={reason}
            onChange={(event) => setReason(event.target.value)}
            maxLength={255}
          />
        </label>
      </form>
    </Modal>
  );
}

function BookingDetailModal({
  booking,
  metaText,
  onClose
}: {
  booking: Booking;
  metaText: string;
  onClose: () => void;
}) {
  return (
    <Modal title={booking.serviceName} eyebrow="Dettaglio prenotazione" description={metaText} onClose={onClose}>
      <dl className="grid gap-2 rounded-md border border-slate-200 bg-slate-50 p-3 text-sm">
        <DetailRow label="Stato" value={bookingStatusLabel(booking.status)} />
        <DetailRow label="Inizio" value={formatDateTime(booking.startsAt)} />
        <DetailRow label="Fine" value={formatDateTime(booking.endsAt)} />
        <DetailRow label="Creata il" value={formatDateTime(booking.createdAt)} />
        <DetailRow label="Aggiornata il" value={formatDateTime(booking.updatedAt)} />
        {booking.cancelledAt && <DetailRow label="Annullata il" value={formatDateTime(booking.cancelledAt)} />}
        {booking.cancellationReason && <DetailRow label="Motivo" value={booking.cancellationReason} />}
      </dl>
    </Modal>
  );
}

function DetailRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex justify-between gap-3">
      <dt className="text-slate-500">{label}</dt>
      <dd className="text-right font-semibold text-slate-950">{value}</dd>
    </div>
  );
}
