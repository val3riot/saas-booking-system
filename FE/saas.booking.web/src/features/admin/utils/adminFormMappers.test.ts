import { describe, expect, it } from 'vitest';
import {
  toCreateProviderPayload,
  toCreateUserPayload,
  toUpdateProviderPayload,
  toUpdateUserPayload
} from './adminFormMappers';

describe('adminFormMappers', () => {
  it('maps a user form to a create payload', () => {
    expect(
      toCreateUserPayload({
        email: ' admin@example.com ',
        password: 'Password123!',
        role: 'ADMIN',
        enabled: false
      })
    ).toEqual({
      email: 'admin@example.com',
      password: 'Password123!',
      role: 'ADMIN'
    });
  });

  it('maps a user form to an update payload without password', () => {
    expect(
      toUpdateUserPayload({
        email: ' user@example.com ',
        password: 'ignored',
        role: 'CUSTOMER',
        enabled: true
      })
    ).toEqual({
      email: 'user@example.com',
      role: 'CUSTOMER',
      enabled: true
    });
  });

  it('maps a provider form to a create payload', () => {
    expect(
      toCreateProviderPayload({
        userId: '',
        email: ' provider@example.com ',
        password: 'Password1!',
        businessName: ' Studio ',
        description: ' ',
        category: 'Fisio',
        city: 'Milano',
        address: 'Via Roma',
        active: false
      })
    ).toEqual({
      email: 'provider@example.com',
      password: 'Password1!',
      businessName: 'Studio',
      description: undefined,
      category: 'Fisio',
      city: 'Milano',
      address: 'Via Roma'
    });
  });

  it('maps a provider form to an update payload with the linked user id', () => {
    expect(
      toUpdateProviderPayload({
        userId: '12',
        email: 'ignored@example.com',
        password: 'ignored',
        businessName: ' Studio ',
        description: ' ',
        category: 'Fisio',
        city: 'Milano',
        address: 'Via Roma',
        active: false
      })
    ).toEqual({
      userId: 12,
      businessName: 'Studio',
      description: undefined,
      category: 'Fisio',
      city: 'Milano',
      address: 'Via Roma',
      active: false
    });
  });
});
