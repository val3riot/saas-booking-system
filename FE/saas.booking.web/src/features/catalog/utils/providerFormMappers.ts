import type {
  AvailabilityExceptionPayload,
  AvailabilityPayload,
  OfferedService,
  OfferedServicePayload,
  ProviderProfile,
  ProviderProfilePayload
} from '../../../types/api';
import type { AvailabilityFormState, ExceptionFormState, ProviderProfileFormState, ServiceFormState } from '../types';

export type ExceptionPayloadItem = {
  label: string;
  payload: AvailabilityExceptionPayload;
};

export const emptyProviderProfileForm: ProviderProfileFormState = {
  businessName: '',
  description: '',
  category: '',
  city: '',
  address: ''
};

export function toServicePayload(form: ServiceFormState): OfferedServicePayload {
  return {
    name: form.name.trim(),
    description: form.description.trim() || undefined,
    durationMinutes: Number(form.durationMinutes),
    priceCents: Math.round(Number(form.price) * 100)
  };
}

export function toAvailabilityPayload(form: AvailabilityFormState): AvailabilityPayload {
  return {
    dayOfWeek: Number(form.dayOfWeek),
    startTime: form.startTime,
    endTime: form.endTime
  };
}

export function toExceptionPayloadItems(form: ExceptionFormState, services: OfferedService[]): ExceptionPayloadItem[] {
  const base = {
    startsAt: new Date(form.startsAt).toISOString(),
    endsAt: new Date(form.endsAt).toISOString(),
    reason: form.reason.trim() || undefined
  };

  if (form.appliesToAll) {
    return [{ label: 'Tutti i servizi', payload: { ...base, serviceId: null } }];
  }

  return form.serviceIds.map((serviceId) => ({
    label: services.find((service) => service.id === Number(serviceId))?.name ?? `Servizio #${serviceId}`,
    payload: { ...base, serviceId: Number(serviceId) }
  }));
}

export function createdExceptionMessage(count: number) {
  if (count === 1) {
    return 'Indisponibilita creata.';
  }

  return `${count} indisponibilita create.`;
}

export function toProviderProfileForm(profile: ProviderProfile): ProviderProfileFormState {
  return {
    businessName: profile.businessName,
    description: profile.description ?? '',
    category: profile.category,
    city: profile.city,
    address: profile.address ?? ''
  };
}

export function toProviderProfilePayload(form: ProviderProfileFormState): ProviderProfilePayload {
  return {
    businessName: form.businessName.trim(),
    description: form.description.trim() || undefined,
    category: form.category.trim(),
    city: form.city.trim(),
    address: form.address.trim() || undefined
  };
}
