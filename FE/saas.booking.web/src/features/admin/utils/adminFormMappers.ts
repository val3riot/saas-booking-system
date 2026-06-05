import type {
  CreateProviderPayload,
  CreateUserPayload,
  ProviderProfile,
  UpdateProviderPayload,
  UpdateUserPayload,
  User
} from '../../../types/api';
import type { AdminProviderFormState, AdminUserFormState } from '../types';

export const emptyAdminUserForm: AdminUserFormState = {
  email: '',
  password: '',
  role: 'CUSTOMER',
  enabled: true
};

export const emptyAdminProviderForm: AdminProviderFormState = {
  userId: '',
  email: '',
  password: '',
  businessName: '',
  description: '',
  category: '',
  city: '',
  address: '',
  active: true
};

export function toAdminUserForm(user: User): AdminUserFormState {
  return {
    email: user.email,
    password: '',
    role: user.role,
    enabled: user.enabled
  };
}

export function toCreateUserPayload(form: AdminUserFormState): CreateUserPayload {
  return {
    email: form.email.trim(),
    password: form.password,
    role: form.role
  };
}

export function toUpdateUserPayload(form: AdminUserFormState): UpdateUserPayload {
  return {
    email: form.email.trim(),
    role: form.role,
    enabled: form.enabled
  };
}

export function toAdminProviderForm(provider: ProviderProfile): AdminProviderFormState {
  return {
    userId: String(provider.userId),
    email: '',
    password: '',
    businessName: provider.businessName,
    description: provider.description ?? '',
    category: provider.category,
    city: provider.city,
    address: provider.address ?? '',
    active: provider.active
  };
}

export function toCreateProviderPayload(form: AdminProviderFormState): CreateProviderPayload {
  return {
    email: form.email.trim(),
    password: form.password,
    businessName: form.businessName.trim(),
    description: form.description.trim() || undefined,
    category: form.category.trim(),
    city: form.city.trim(),
    address: form.address.trim() || undefined
  };
}

export function toUpdateProviderPayload(form: AdminProviderFormState): UpdateProviderPayload {
  return {
    userId: Number(form.userId),
    businessName: form.businessName.trim(),
    description: form.description.trim() || undefined,
    category: form.category.trim(),
    city: form.city.trim(),
    address: form.address.trim() || undefined,
    active: form.active
  };
}
