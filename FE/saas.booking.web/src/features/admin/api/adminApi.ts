import { apiRequest } from '../../../services/http/apiClient';
import type {
  CreateProviderPayload,
  CreateUserPayload,
  ProviderProfile,
  UpdateProviderPayload,
  UpdateUserPayload,
  User
} from '../../../types/api';

const USERS_PATH = '/api/users';
const PROVIDERS_PATH = '/api/providers';

export function listAdminUsers() {
  return apiRequest<User[]>(USERS_PATH);
}

export function createAdminUser(payload: CreateUserPayload) {
  return apiRequest<User>(USERS_PATH, {
    method: 'POST',
    body: payload
  });
}

export function updateAdminUser(userId: number, payload: UpdateUserPayload) {
  return apiRequest<User>(`${USERS_PATH}/${userId}`, {
    method: 'PUT',
    body: payload
  });
}

export function enableAdminUser(userId: number) {
  return apiRequest<void>(`${USERS_PATH}/${userId}/enable`, {
    method: 'POST'
  });
}

export function disableAdminUser(userId: number) {
  return apiRequest<void>(`${USERS_PATH}/${userId}/disable`, {
    method: 'POST'
  });
}

export function deleteAdminUser(userId: number) {
  return apiRequest<void>(`${USERS_PATH}/${userId}`, {
    method: 'DELETE'
  });
}

export function listAdminProviders() {
  return apiRequest<ProviderProfile[]>(PROVIDERS_PATH);
}

export function createAdminProvider(payload: CreateProviderPayload) {
  return apiRequest<ProviderProfile>(PROVIDERS_PATH, {
    method: 'POST',
    body: payload
  });
}

export function updateAdminProvider(providerId: number, payload: UpdateProviderPayload) {
  return apiRequest<ProviderProfile>(`${PROVIDERS_PATH}/${providerId}`, {
    method: 'PUT',
    body: payload
  });
}

export function activateAdminProvider(providerId: number) {
  return apiRequest<void>(`${PROVIDERS_PATH}/${providerId}/activate`, {
    method: 'POST'
  });
}

export function deactivateAdminProvider(providerId: number) {
  return apiRequest<void>(`${PROVIDERS_PATH}/${providerId}/deactivate`, {
    method: 'POST'
  });
}

export function deleteAdminProvider(providerId: number) {
  return apiRequest<void>(`${PROVIDERS_PATH}/${providerId}`, {
    method: 'DELETE'
  });
}
