import { apiRequest } from '../../../services/http/apiClient';
import type { Booking } from '../../../types/api';

export type CreateBookingPayload = {
  providerId: number;
  serviceId: number;
  startsAt: string;
};

export function listBookings() {
  return apiRequest<Booking[]>('/api/bookings');
}

export function createBooking(payload: CreateBookingPayload) {
  return apiRequest<Booking>('/api/bookings', {
    method: 'POST',
    body: payload
  });
}

export function cancelBooking(bookingId: number, reason?: string) {
  return apiRequest<void>(`/api/bookings/${bookingId}/cancel`, {
    method: 'POST',
    body: reason ? { reason } : undefined
  });
}

export function listProviderBookings() {
  return apiRequest<Booking[]>('/api/providers/me/bookings');
}

export function listProviderAgenda(params: { from: string; to: string }) {
  const searchParams = new URLSearchParams(params);
  return apiRequest<Booking[]>(`/api/providers/me/agenda?${searchParams.toString()}`);
}

export function confirmProviderBooking(bookingId: number) {
  return apiRequest<Booking>(`/api/providers/me/bookings/${bookingId}/confirm`, {
    method: 'POST'
  });
}

export function rejectProviderBooking(bookingId: number, reason?: string) {
  return apiRequest<Booking>(`/api/providers/me/bookings/${bookingId}/reject`, {
    method: 'POST',
    body: reason ? { reason } : undefined
  });
}

export function cancelProviderBooking(bookingId: number, reason?: string) {
  return apiRequest<void>(`/api/providers/me/bookings/${bookingId}/cancel`, {
    method: 'POST',
    body: reason ? { reason } : undefined
  });
}
