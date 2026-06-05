import { apiRequest } from '../../../services/http/apiClient';
import type { AuthResponse, LoginPayload, ProviderRegistrationPayload } from '../../../types/api';

export function login(payload: LoginPayload) {
  return apiRequest<AuthResponse>('/api/auth/login', {
    method: 'POST',
    body: payload,
    auth: false
  });
}

export function registerCustomer(payload: LoginPayload) {
  return apiRequest<AuthResponse>('/api/auth/register', {
    method: 'POST',
    body: payload,
    auth: false
  });
}

export function registerProvider(payload: ProviderRegistrationPayload) {
  return apiRequest<AuthResponse>('/api/auth/register/provider', {
    method: 'POST',
    body: payload,
    auth: false
  });
}
