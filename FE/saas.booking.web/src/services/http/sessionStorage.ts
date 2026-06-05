import type { UserRole } from '../../types/api';

const TOKEN_KEY = 'booking.token';
const SESSION_CLEARED_EVENT = 'booking.session-cleared';

type JwtPayload = {
  sub?: string;
  email?: string;
  role?: UserRole;
  exp?: number;
};

export type Session = {
  token: string;
  userId?: number;
  email?: string;
  role?: UserRole;
};

export function getStoredToken(): string | null {
  return window.localStorage.getItem(TOKEN_KEY);
}

export function getValidStoredToken(): string | null {
  const token = getStoredToken();
  if (!token) {
    return null;
  }

  if (isTokenExpired(token)) {
    clearSession();
    return null;
  }

  return token;
}

export function storeToken(token: string): void {
  window.localStorage.setItem(TOKEN_KEY, token);
}

export function clearToken(): void {
  window.localStorage.removeItem(TOKEN_KEY);
}

export function clearSession(): void {
  clearToken();
  window.dispatchEvent(new Event(SESSION_CLEARED_EVENT));
}

export function onSessionCleared(listener: () => void): () => void {
  window.addEventListener(SESSION_CLEARED_EVENT, listener);
  return () => window.removeEventListener(SESSION_CLEARED_EVENT, listener);
}

export function readSession(): Session | null {
  const token = getStoredToken();
  if (!token) {
    return null;
  }

  const payload = decodeJwt(token);
  if (isTokenExpired(token)) {
    clearSession();
    return null;
  }

  return {
    token,
    userId: payload?.sub ? Number(payload.sub) : undefined,
    email: payload?.email,
    role: payload?.role
  };
}

function isTokenExpired(token: string): boolean {
  const payload = decodeJwt(token);
  return Boolean(payload?.exp && payload.exp * 1000 < Date.now());
}

function decodeJwt(token: string): JwtPayload | null {
  const [, payload] = token.split('.');
  if (!payload) {
    return null;
  }

  try {
    const normalized = payload.replace(/-/g, '+').replace(/_/g, '/');
    const json = window.atob(normalized.padEnd(Math.ceil(normalized.length / 4) * 4, '='));
    return JSON.parse(json) as JwtPayload;
  } catch {
    return null;
  }
}
