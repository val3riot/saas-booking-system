import { FormEvent, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Button } from '../../../components/Button';
import { FormField } from '../../../components/FormField';
import { StatusMessage } from '../../../components/StatusMessage';
import { AuthLayout } from '../../../layouts/AuthLayout';
import { apiErrorMessage } from '../../../lib/apiErrors';
import { readSession } from '../../../services/http/sessionStorage';
import type { ProviderRegistrationPayload, UserRole } from '../../../types/api';
import { login, registerCustomer, registerProvider } from '../api/authApi';
import { useAuth } from '../hooks/useAuth';

type AuthMode = 'login' | 'customer' | 'provider';

type AuthFormProps = {
  mode: AuthMode;
};

const initialProviderPayload: ProviderRegistrationPayload = {
  email: '',
  password: '',
  businessName: '',
  description: '',
  category: '',
  city: '',
  address: ''
};

export function AuthForm({ mode }: AuthFormProps) {
  const isLogin = mode === 'login';
  const isProvider = mode === 'provider';
  const [payload, setPayload] = useState<ProviderRegistrationPayload>(initialProviderPayload);
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const { setToken } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setIsSubmitting(true);

    try {
      const response = isLogin
        ? await login({ email: payload.email, password: payload.password })
        : isProvider
          ? await registerProvider(payload)
          : await registerCustomer({ email: payload.email, password: payload.password });

      setToken(response.token);
      const nextSession = readSession();
      const from = location.state?.from?.pathname;
      const redirectTo = redirectAfterAuth(nextSession?.role, from);
      navigate(redirectTo, { replace: true });
    } catch (requestError) {
      setError(apiErrorMessage(requestError));
    } finally {
      setIsSubmitting(false);
    }
  }

  function updateField(field: keyof ProviderRegistrationPayload, value: string) {
    setPayload((current) => ({ ...current, [field]: value }));
  }

  return (
    <AuthLayout>
      <form className="grid gap-4" onSubmit={handleSubmit}>
        <div>
          <h2 className="text-lg font-bold leading-tight text-slate-950">{titleForMode(mode)}</h2>
          <p className="text-slate-500">{subtitleForMode(mode)}</p>
        </div>

        {error && <StatusMessage tone="danger">{error}</StatusMessage>}

        <FormField
          label="Email"
          name="email"
          type="email"
          autoComplete="email"
          value={payload.email}
          onChange={(event) => updateField('email', event.target.value)}
          required
        />
        <FormField
          label="Password"
          name="password"
          type="password"
          autoComplete={isLogin ? 'current-password' : 'new-password'}
          value={payload.password}
          onChange={(event) => updateField('password', event.target.value)}
          required
        />

        {isProvider && (
          <>
            <FormField
              label="Nome attivita'"
              name="businessName"
              value={payload.businessName}
              onChange={(event) => updateField('businessName', event.target.value)}
              required
            />
            <div className="grid gap-3 md:grid-cols-2">
              <FormField
                label="Categoria"
                name="category"
                value={payload.category}
                onChange={(event) => updateField('category', event.target.value)}
                required
              />
              <FormField
                label="Citta'"
                name="city"
                value={payload.city}
                onChange={(event) => updateField('city', event.target.value)}
                required
              />
            </div>
            <FormField
              label="Indirizzo"
              name="address"
              value={payload.address}
              onChange={(event) => updateField('address', event.target.value)}
            />
            <FormField
              label="Descrizione"
              name="description"
              value={payload.description}
              onChange={(event) => updateField('description', event.target.value)}
              multiline
              rows={3}
            />
          </>
        )}

        <Button fullWidth type="submit" disabled={isSubmitting}>
          {isSubmitting ? 'Invio...' : submitLabelForMode(mode)}
        </Button>

        <div className="flex flex-wrap justify-between gap-3 text-sm">
          {isLogin ? (
            <>
              <Link className="text-slate-500 no-underline hover:text-slate-950" to="/register">
                Registrati come customer
              </Link>
              <Link className="text-slate-500 no-underline hover:text-slate-950" to="/register-provider">
                Registrati come provider
              </Link>
            </>
          ) : (
            <Link className="text-slate-500 no-underline hover:text-slate-950" to="/login">
              Hai gia' un account?
            </Link>
          )}
        </div>
      </form>
    </AuthLayout>
  );
}

function titleForMode(mode: AuthMode) {
  if (mode === 'provider') return 'Registrazione provider';
  if (mode === 'customer') return 'Registrazione customer';
  return 'Accesso';
}

function subtitleForMode(mode: AuthMode) {
  if (mode === 'provider') return 'Crea il profilo professionale e prepara i servizi prenotabili.';
  if (mode === 'customer') return 'Crea un account per cercare provider e prenotare appuntamenti.';
  return 'Accedi per gestire prenotazioni e catalogo.';
}

function submitLabelForMode(mode: AuthMode) {
  if (mode === 'login') return 'Accedi';
  return 'Crea account';
}

function redirectAfterAuth(role?: UserRole, from?: string) {
  if (from && isAllowedRedirectForRole(role, from)) {
    return from;
  }

  return role === 'ADMIN' ? '/admin' : '/catalog';
}

function isAllowedRedirectForRole(role: UserRole | undefined, path: string) {
  if (role === 'ADMIN') {
    return path === '/admin';
  }

  return path !== '/admin';
}
