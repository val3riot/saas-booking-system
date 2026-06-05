import { createContext } from 'react';
import type { Session } from '../../../services/http/sessionStorage';

export type AuthContextValue = {
  session: Session | null;
  isAuthenticated: boolean;
  setToken: (token: string) => void;
  logout: () => void;
};

export const AuthContext = createContext<AuthContextValue | undefined>(undefined);
