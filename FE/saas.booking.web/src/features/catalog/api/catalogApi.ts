import { apiRequest, queryString } from '../../../services/http/apiClient';
import type { BookingSlot, CatalogProvider, CatalogService, PageResponse } from '../../../types/api';

export type ProviderSearchParams = {
  query?: string;
  category?: string;
  city?: string;
  availableOn?: string;
  page?: number;
  size?: number;
  sort?: 'BUSINESS_NAME' | 'CITY' | 'CATEGORY';
  direction?: 'ASC' | 'DESC';
};

export function searchProviders(params: ProviderSearchParams) {
  return apiRequest<PageResponse<CatalogProvider>>(`/api/catalog/providers/search${queryString(params)}`);
}

export function listProviderServices(providerId: number) {
  return apiRequest<CatalogService[]>(`/api/catalog/providers/${providerId}/services`);
}

export function listBookingSlots(params: { providerId: number; serviceId: number; from: string; to: string }) {
  return apiRequest<BookingSlot[]>(`/api/booking-slots${queryString(params)}`);
}
