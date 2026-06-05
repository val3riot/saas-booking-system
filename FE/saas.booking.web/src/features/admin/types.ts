import type { UserRole } from '../../types/api';

export type AdminUserFormState = {
  email: string;
  password: string;
  role: UserRole;
  enabled: boolean;
};

export type AdminProviderFormState = {
  userId: string;
  email: string;
  password: string;
  businessName: string;
  description: string;
  category: string;
  city: string;
  address: string;
  active: boolean;
};
