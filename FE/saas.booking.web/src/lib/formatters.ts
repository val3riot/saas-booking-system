import type { Booking, BookingSlot } from '../types/api';

export function formatCurrency(priceCents: number) {
  return new Intl.NumberFormat('it-IT', { style: 'currency', currency: 'EUR' }).format(priceCents / 100);
}

export function formatDateTime(value: string) {
  return new Intl.DateTimeFormat('it-IT', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value));
}

export function formatClock(value: string) {
  return new Intl.DateTimeFormat('it-IT', { hour: '2-digit', minute: '2-digit' }).format(new Date(value));
}

export function bookingStatusLabel(status: Booking['status']) {
  if (status === 'PENDING') return 'In attesa';
  if (status === 'CONFIRMED') return 'Confermata';
  if (status === 'REJECTED') return 'Rifiutata';
  if (status === 'CANCELLED') return 'Annullata';
  return 'Completata';
}

export function bookingStatusClass(status: Booking['status']) {
  if (status === 'PENDING') return 'text-amber-700';
  if (status === 'CONFIRMED') return 'text-emerald-700';
  if (status === 'CANCELLED' || status === 'REJECTED') return 'text-red-700';
  return 'text-slate-700';
}

export function bookingSlotLabel(status: BookingSlot['status']) {
  if (status === 'AVAILABLE') return 'Disponibile';
  if (status === 'BLOCKED') return 'Non disponibile';
  return 'Occupato';
}
