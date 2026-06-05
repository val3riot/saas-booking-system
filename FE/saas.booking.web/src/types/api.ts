export type UserRole = 'CUSTOMER' | 'PROVIDER' | 'ADMIN';

export type AuthResponse = {
  token: string;
  tokenType: 'Bearer';
};

export type LoginPayload = {
  email: string;
  password: string;
};

export type User = {
  id: number;
  email: string;
  role: UserRole;
  enabled: boolean;
  createdAt: string;
  updatedAt: string;
};

export type CreateUserPayload = LoginPayload & {
  role: UserRole;
};

export type UpdateUserPayload = {
  email: string;
  role: UserRole;
  enabled: boolean;
};

export type ProviderRegistrationPayload = LoginPayload & {
  businessName: string;
  description?: string;
  category: string;
  city: string;
  address?: string;
};

export type ApiFieldError = {
  code: string;
  message: string;
};

export type ApiErrorResponse = {
  code: string;
  message: string;
  status: number;
  path: string;
  timestamp: string;
  fields: Record<string, ApiFieldError>;
};

export type PageResponse<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
};

export type CatalogProvider = {
  id: number;
  businessName: string;
  description?: string | null;
  category: string;
  city: string;
  address?: string | null;
};

export type ProviderProfile = CatalogProvider & {
  userId: number;
  active: boolean;
  createdAt: string;
  updatedAt: string;
};

export type ProviderProfilePayload = {
  businessName: string;
  description?: string;
  category: string;
  city: string;
  address?: string;
};

export type CreateProviderPayload = ProviderRegistrationPayload;

export type UpdateProviderPayload = ProviderProfilePayload & {
  userId: number;
  active: boolean;
};

export type CatalogService = {
  id: number;
  providerId: number;
  name: string;
  description?: string | null;
  durationMinutes: number;
  priceCents: number;
};

export type OfferedService = CatalogService & {
  active: boolean;
  createdAt: string;
  updatedAt: string;
};

export type OfferedServicePayload = {
  name: string;
  description?: string;
  durationMinutes: number;
  priceCents: number;
};

export type UpdateOfferedServicePayload = OfferedServicePayload & {
  active: boolean;
};

export type Availability = {
  id: number;
  providerId: number;
  serviceId: number;
  dayOfWeek: number;
  startTime: string;
  endTime: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
};

export type AvailabilityPayload = {
  dayOfWeek: number;
  startTime: string;
  endTime: string;
};

export type UpdateAvailabilityPayload = AvailabilityPayload & {
  active: boolean;
};

export type AvailabilityException = {
  id: number;
  providerId: number;
  serviceId?: number | null;
  startsAt: string;
  endsAt: string;
  reason?: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
};

export type AvailabilityExceptionPayload = {
  serviceId?: number | null;
  startsAt: string;
  endsAt: string;
  reason?: string;
};

export type UpdateAvailabilityExceptionPayload = AvailabilityExceptionPayload & {
  active: boolean;
};

export type BookingSlotStatus = 'AVAILABLE' | 'BOOKED' | 'BLOCKED';

export type BookingSlot = {
  providerId: number;
  serviceId: number;
  date: string;
  startsAt: string;
  endsAt: string;
  status: BookingSlotStatus;
};

export type BookingStatus = 'PENDING' | 'CONFIRMED' | 'REJECTED' | 'CANCELLED' | 'COMPLETED';

export type Booking = {
  id: number;
  customerId: number;
  providerId: number;
  providerBusinessName: string;
  serviceId: number;
  serviceName: string;
  startsAt: string;
  endsAt: string;
  status: BookingStatus;
  cancelledAt?: string | null;
  cancelledByUserId?: number | null;
  cancellationReason?: string | null;
  createdAt: string;
  updatedAt: string;
};
