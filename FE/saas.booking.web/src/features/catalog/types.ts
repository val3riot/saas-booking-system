export type ServiceFormState = {
  name: string;
  description: string;
  durationMinutes: string;
  price: string;
};

export type AvailabilityFormState = {
  dayOfWeek: string;
  startTime: string;
  endTime: string;
};

export type ExceptionFormState = {
  appliesToAll: boolean;
  serviceIds: string[];
  startsAt: string;
  endsAt: string;
  reason: string;
};

export type ProviderProfileFormState = {
  businessName: string;
  description: string;
  category: string;
  city: string;
  address: string;
};

export type CatalogSearchState = {
  query: string;
  category: string;
  city: string;
  availableOn: string;
  sort: 'BUSINESS_NAME' | 'CITY' | 'CATEGORY';
  direction: 'ASC' | 'DESC';
  size: string;
};
