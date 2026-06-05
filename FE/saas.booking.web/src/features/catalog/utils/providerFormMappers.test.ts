import { describe, expect, it } from 'vitest';
import type { OfferedService } from '../../../types/api';
import { toExceptionPayloadItems, toServicePayload } from './providerFormMappers';

const service: OfferedService = {
  id: 7,
  providerId: 3,
  name: 'Taglio',
  description: 'Servizio base',
  durationMinutes: 45,
  priceCents: 2500,
  active: true,
  createdAt: '2026-06-03T09:00:00Z',
  updatedAt: '2026-06-03T09:00:00Z'
};

describe('providerFormMappers', () => {
  it('maps a service form to an API payload', () => {
    expect(
      toServicePayload({
        name: ' Taglio ',
        description: ' ',
        durationMinutes: '45',
        price: '25.50'
      })
    ).toEqual({
      name: 'Taglio',
      description: undefined,
      durationMinutes: 45,
      priceCents: 2550
    });
  });

  it('creates one exception payload for each selected service', () => {
    expect(
      toExceptionPayloadItems(
        {
          appliesToAll: false,
          serviceIds: ['7'],
          startsAt: '2026-06-03T09:00',
          endsAt: '2026-06-03T10:00',
          reason: ' Ferie '
        },
        [service]
      )
    ).toEqual([
      {
        label: 'Taglio',
        payload: {
          serviceId: 7,
          startsAt: new Date('2026-06-03T09:00').toISOString(),
          endsAt: new Date('2026-06-03T10:00').toISOString(),
          reason: 'Ferie'
        }
      }
    ]);
  });
});
