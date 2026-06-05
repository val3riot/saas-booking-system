import { apiRequest } from '../../../services/http/apiClient';
import type {
  Availability,
  AvailabilityException,
  AvailabilityExceptionPayload,
  AvailabilityPayload,
  OfferedService,
  OfferedServicePayload,
  ProviderProfile,
  ProviderProfilePayload,
  UpdateAvailabilityExceptionPayload,
  UpdateAvailabilityPayload,
  UpdateOfferedServicePayload
} from '../../../types/api';

const PROVIDER_SERVICES_PATH = '/api/providers/me/services';

export function listMyServices() {
  return apiRequest<OfferedService[]>(PROVIDER_SERVICES_PATH);
}

export function createMyService(payload: OfferedServicePayload) {
  return apiRequest<OfferedService>(PROVIDER_SERVICES_PATH, {
    method: 'POST',
    body: payload
  });
}

export function updateMyService(serviceId: number, payload: UpdateOfferedServicePayload) {
  return apiRequest<OfferedService>(`${PROVIDER_SERVICES_PATH}/${serviceId}`, {
    method: 'PUT',
    body: payload
  });
}

export function activateMyService(serviceId: number) {
  return apiRequest<void>(`${PROVIDER_SERVICES_PATH}/${serviceId}/activate`, {
    method: 'POST'
  });
}

export function deactivateMyService(serviceId: number) {
  return apiRequest<void>(`${PROVIDER_SERVICES_PATH}/${serviceId}/deactivate`, {
    method: 'POST'
  });
}

export function deleteMyService(serviceId: number) {
  return apiRequest<void>(`${PROVIDER_SERVICES_PATH}/${serviceId}`, {
    method: 'DELETE'
  });
}

function availabilitiesPath(serviceId: number) {
  return `${PROVIDER_SERVICES_PATH}/${serviceId}/availabilities`;
}

export function listMyAvailabilities(serviceId: number) {
  return apiRequest<Availability[]>(availabilitiesPath(serviceId));
}

export function createMyAvailability(serviceId: number, payload: AvailabilityPayload) {
  return apiRequest<Availability>(availabilitiesPath(serviceId), {
    method: 'POST',
    body: payload
  });
}

export function updateMyAvailability(serviceId: number, availabilityId: number, payload: UpdateAvailabilityPayload) {
  return apiRequest<Availability>(`${availabilitiesPath(serviceId)}/${availabilityId}`, {
    method: 'PUT',
    body: payload
  });
}

export function activateMyAvailability(serviceId: number, availabilityId: number) {
  return apiRequest<void>(`${availabilitiesPath(serviceId)}/${availabilityId}/activate`, {
    method: 'POST'
  });
}

export function deactivateMyAvailability(serviceId: number, availabilityId: number) {
  return apiRequest<void>(`${availabilitiesPath(serviceId)}/${availabilityId}/deactivate`, {
    method: 'POST'
  });
}

export function deleteMyAvailability(serviceId: number, availabilityId: number) {
  return apiRequest<void>(`${availabilitiesPath(serviceId)}/${availabilityId}`, {
    method: 'DELETE'
  });
}

const AVAILABILITY_EXCEPTIONS_PATH = '/api/providers/me/availability-exceptions';

export function listMyAvailabilityExceptions() {
  return apiRequest<AvailabilityException[]>(AVAILABILITY_EXCEPTIONS_PATH);
}

export function createMyAvailabilityException(payload: AvailabilityExceptionPayload) {
  return apiRequest<AvailabilityException>(AVAILABILITY_EXCEPTIONS_PATH, {
    method: 'POST',
    body: payload
  });
}

export function updateMyAvailabilityException(exceptionId: number, payload: UpdateAvailabilityExceptionPayload) {
  return apiRequest<AvailabilityException>(`${AVAILABILITY_EXCEPTIONS_PATH}/${exceptionId}`, {
    method: 'PUT',
    body: payload
  });
}

export function activateMyAvailabilityException(exceptionId: number) {
  return apiRequest<void>(`${AVAILABILITY_EXCEPTIONS_PATH}/${exceptionId}/activate`, {
    method: 'POST'
  });
}

export function deactivateMyAvailabilityException(exceptionId: number) {
  return apiRequest<void>(`${AVAILABILITY_EXCEPTIONS_PATH}/${exceptionId}/deactivate`, {
    method: 'POST'
  });
}

export function deleteMyAvailabilityException(exceptionId: number) {
  return apiRequest<void>(`${AVAILABILITY_EXCEPTIONS_PATH}/${exceptionId}`, {
    method: 'DELETE'
  });
}

const PROVIDER_PROFILE_PATH = '/api/providers/me';

export function getMyProviderProfile() {
  return apiRequest<ProviderProfile>(PROVIDER_PROFILE_PATH);
}

export function createMyProviderProfile(payload: ProviderProfilePayload) {
  return apiRequest<ProviderProfile>(PROVIDER_PROFILE_PATH, {
    method: 'POST',
    body: payload
  });
}

export function updateMyProviderProfile(payload: ProviderProfilePayload) {
  return apiRequest<ProviderProfile>(PROVIDER_PROFILE_PATH, {
    method: 'PUT',
    body: payload
  });
}
