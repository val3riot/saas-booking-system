import { useEffect, useMemo, useState } from 'react';
import { apiErrorMessage } from '../../../lib/apiErrors';
import type { Booking } from '../../../types/api';
import { cancelBooking, listBookings } from '../api/bookingsApi';

export type CustomerBookingSort = 'startsAtAsc' | 'startsAtDesc' | 'createdAtDesc' | 'createdAtAsc';

export function useCustomerBookings() {
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [sort, setSort] = useState<CustomerBookingSort>('startsAtAsc');
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [action, setAction] = useState<{ booking: Booking; type: 'cancel' } | null>(null);
  const [selectedBooking, setSelectedBooking] = useState<Booking | null>(null);

  const sortedBookings = useMemo(() => {
    return [...bookings].sort((left, right) => {
      if (sort === 'startsAtAsc') {
        return new Date(left.startsAt).getTime() - new Date(right.startsAt).getTime();
      }

      if (sort === 'startsAtDesc') {
        return new Date(right.startsAt).getTime() - new Date(left.startsAt).getTime();
      }

      if (sort === 'createdAtAsc') {
        return new Date(left.createdAt).getTime() - new Date(right.createdAt).getTime();
      }

      return new Date(right.createdAt).getTime() - new Date(left.createdAt).getTime();
    });
  }, [bookings, sort]);

  useEffect(() => {
    void loadBookings();
  }, []);

  async function loadBookings() {
    setIsLoading(true);
    setError(null);

    try {
      setBookings(await listBookings());
    } catch (requestError) {
      setError(apiErrorMessage(requestError));
    } finally {
      setIsLoading(false);
    }
  }

  async function cancel(booking: Booking, reason: string) {
    setMessage(null);
    setError(null);

    try {
      await cancelBooking(booking.id, reason || undefined);
      setMessage('Prenotazione annullata.');
      setAction(null);
      await loadBookings();
    } catch (requestError) {
      setError(apiErrorMessage(requestError));
    }
  }

  return {
    action,
    bookings: sortedBookings,
    cancel,
    error,
    isLoading,
    loadBookings,
    message,
    selectedBooking,
    setAction,
    setSelectedBooking,
    setSort,
    sort
  };
}
