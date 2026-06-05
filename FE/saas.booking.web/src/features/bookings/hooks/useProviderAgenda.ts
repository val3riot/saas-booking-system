import { FormEvent, useEffect, useState } from 'react';
import { apiErrorMessage } from '../../../lib/apiErrors';
import type { Booking } from '../../../types/api';
import { todayDateInputValue } from '../../../utils/dateUtils';
import {
  cancelProviderBooking,
  confirmProviderBooking,
  listProviderAgenda,
  rejectProviderBooking
} from '../api/bookingsApi';

const today = todayDateInputValue();

export function useProviderAgenda() {
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [from, setFrom] = useState(today);
  const [to, setTo] = useState(today);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [action, setAction] = useState<{ booking: Booking; type: 'reject' | 'cancel' } | null>(null);
  const [selectedBooking, setSelectedBooking] = useState<Booking | null>(null);

  useEffect(() => {
    void loadBookings();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [from, to]);

  async function loadBookings() {
    setIsLoading(true);
    setError(null);

    try {
      setBookings(await listProviderAgenda({ from, to }));
    } catch (requestError) {
      setError(apiErrorMessage(requestError));
    } finally {
      setIsLoading(false);
    }
  }

  async function confirm(booking: Booking) {
    setMessage(null);
    setError(null);

    try {
      await confirmProviderBooking(booking.id);
      setMessage('Prenotazione confermata.');
      await loadBookings();
    } catch (requestError) {
      setError(apiErrorMessage(requestError));
    }
  }

  async function reject(booking: Booking, reason: string) {
    setMessage(null);
    setError(null);

    try {
      await rejectProviderBooking(booking.id, reason || undefined);
      setMessage('Prenotazione rifiutata.');
      setAction(null);
      await loadBookings();
    } catch (requestError) {
      setError(apiErrorMessage(requestError));
    }
  }

  async function cancel(booking: Booking, reason: string) {
    setMessage(null);
    setError(null);

    try {
      await cancelProviderBooking(booking.id, reason || undefined);
      setMessage('Prenotazione annullata.');
      setAction(null);
      await loadBookings();
    } catch (requestError) {
      setError(apiErrorMessage(requestError));
    }
  }

  function submitFilters(event: FormEvent) {
    event.preventDefault();
    void loadBookings();
  }

  return {
    action,
    bookings,
    cancel,
    confirm,
    error,
    from,
    isLoading,
    loadBookings,
    message,
    reject,
    selectedBooking,
    setAction,
    setFrom,
    setSelectedBooking,
    setTo,
    submitFilters,
    to,
    today
  };
}
