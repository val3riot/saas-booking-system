import { useCallback, useEffect, useMemo, useState } from 'react';
import type { PropsWithChildren } from 'react';
import { clearToken, onSessionCleared, readSession, storeToken, type Session } from '../../../services/http/sessionStorage';
import { AuthContext, type AuthContextValue } from './authContextValue';

export function AuthProvider({ children }: PropsWithChildren) {
  const [session, setSession] = useState<Session | null>(() => readSession());

  const setToken = useCallback((token: string) => {
    storeToken(token);
    setSession(readSession());
  }, []);

  const logout = useCallback(() => {
    clearToken();
    setSession(null);
  }, []);

  useEffect(() => onSessionCleared(() => setSession(null)), []);

  const value = useMemo<AuthContextValue>(
    () => ({
      session,
      isAuthenticated: session !== null,
      setToken,
      logout
    }),
    [logout, session, setToken]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
