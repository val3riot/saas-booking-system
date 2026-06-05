import { FormEvent, useEffect, useMemo, useState } from 'react';
import { createBooking } from '../../bookings/api/bookingsApi';
import { apiErrorMessage } from '../../../lib/apiErrors';
import type { BookingSlot, CatalogProvider, CatalogService, PageResponse } from '../../../types/api';
import { todayDateInputValue } from '../../../utils/dateUtils';
import { listBookingSlots, listProviderServices, searchProviders } from '../api/catalogApi';
import type { CatalogSearchState } from '../types';

const today = todayDateInputValue();

const initialSearch: CatalogSearchState = {
  query: '',
  category: '',
  city: '',
  availableOn: '',
  sort: 'BUSINESS_NAME',
  direction: 'ASC',
  size: '10'
};

export function useCustomerCatalog() {
  const [filters, setFilters] = useState<CatalogSearchState>(initialSearch);
  const [result, setResult] = useState<PageResponse<CatalogProvider> | null>(null);
  const [selectedProvider, setSelectedProvider] = useState<CatalogProvider | null>(null);
  const [services, setServices] = useState<CatalogService[]>([]);
  const [selectedServiceId, setSelectedServiceId] = useState<number | null>(null);
  const [bookingDate, setBookingDate] = useState(today);
  const [slots, setSlots] = useState<BookingSlot[]>([]);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [slotToConfirm, setSlotToConfirm] = useState<BookingSlot | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [isLoadingSlots, setIsLoadingSlots] = useState(false);
  const [isBookingSlot, setIsBookingSlot] = useState(false);

  const selectedService = useMemo(
    () => services.find((service) => service.id === selectedServiceId) ?? null,
    [selectedServiceId, services]
  );

  useEffect(() => {
    void loadProviders();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (!selectedProvider || !selectedServiceId) {
      setSlots([]);
      return;
    }

    void loadSlots(selectedProvider.id, selectedServiceId, bookingDate);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [bookingDate, selectedProvider?.id, selectedServiceId]);

  async function loadProviders(page = 0, selectFirstResult = false) {
    setIsLoading(true);
    setError(null);

    try {
      const providers = await searchProviders({
        query: filters.query,
        category: filters.category,
        city: filters.city,
        availableOn: filters.availableOn,
        page,
        size: Number(filters.size),
        sort: filters.sort,
        direction: filters.direction
      });
      setResult(providers);
      if ((selectFirstResult || !selectedProvider) && providers.content.length > 0) {
        await selectProvider(providers.content[0]);
      }
    } catch (requestError) {
      setError(apiErrorMessage(requestError));
    } finally {
      setIsLoading(false);
    }
  }

  async function handleSearch(event: FormEvent) {
    event.preventDefault();
    setSelectedProvider(null);
    setServices([]);
    setSelectedServiceId(null);
    setBookingDate(filters.availableOn || today);
    setSlots([]);
    await loadProviders(0, true);
  }

  async function selectProvider(provider: CatalogProvider) {
    setSelectedProvider(provider);
    setSelectedServiceId(null);
    setSlots([]);
    setError(null);

    try {
      const providerServices = await listProviderServices(provider.id);
      setServices(providerServices);
      if (providerServices.length > 0) {
        setSelectedServiceId(providerServices[0].id);
      }
    } catch (requestError) {
      setError(apiErrorMessage(requestError));
    }
  }

  function handleServiceChange(value: string) {
    const serviceId = Number(value);
    setSelectedServiceId(Number.isNaN(serviceId) ? null : serviceId);
    setSlots([]);
    setMessage(null);
  }

  async function loadSlots(providerId = selectedProvider?.id, serviceId = selectedServiceId, date = bookingDate) {
    if (!providerId || !serviceId) {
      setSlots([]);
      return;
    }

    setError(null);
    setIsLoadingSlots(true);

    try {
      const providerSlots = await listBookingSlots({
        providerId,
        serviceId,
        from: date,
        to: date
      });
      setSlots(providerSlots);
    } catch (requestError) {
      setError(apiErrorMessage(requestError));
    } finally {
      setIsLoadingSlots(false);
    }
  }

  async function bookSlot(slot: BookingSlot) {
    setMessage(null);
    setError(null);
    setIsBookingSlot(true);

    try {
      await createBooking({
        providerId: slot.providerId,
        serviceId: slot.serviceId,
        startsAt: slot.startsAt
      });
      setSlots((current) =>
        current.map((item) => (item.startsAt === slot.startsAt ? { ...item, status: 'BOOKED' } : item))
      );
      setSlotToConfirm(null);
      setMessage('Richiesta inviata: la prenotazione e in attesa di conferma.');
      await loadSlots(slot.providerId, slot.serviceId, bookingDate);
    } catch (requestError) {
      setError(apiErrorMessage(requestError));
    } finally {
      setIsBookingSlot(false);
    }
  }

  return {
    bookingDate,
    bookSlot,
    error,
    filters,
    handleSearch,
    handleServiceChange,
    isBookingSlot,
    isLoading,
    isLoadingSlots,
    loadProviders,
    loadSlots,
    message,
    result,
    selectProvider,
    selectedProvider,
    selectedService,
    selectedServiceId,
    services,
    setBookingDate,
    setFilters,
    setSlotToConfirm,
    slotToConfirm,
    slots,
    today
  };
}
