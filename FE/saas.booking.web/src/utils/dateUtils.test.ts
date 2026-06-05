import { describe, expect, it } from 'vitest';
import { toDateTimeLocal, todayDateInputValue } from './dateUtils';

describe('dateUtils', () => {
  it('formats the current day as an input date value', () => {
    expect(todayDateInputValue()).toMatch(/^\d{4}-\d{2}-\d{2}$/);
  });

  it('formats an ISO timestamp for datetime-local inputs', () => {
    expect(toDateTimeLocal('2026-06-03T10:30:00.000Z')).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/);
  });
});
