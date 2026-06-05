import { FormEvent, useEffect, useMemo, useState } from 'react';
import { apiErrorMessage } from '../../../lib/apiErrors';
import type { ProviderProfile, User } from '../../../types/api';
import {
  activateAdminProvider,
  createAdminProvider,
  createAdminUser,
  deactivateAdminProvider,
  deleteAdminProvider,
  deleteAdminUser,
  disableAdminUser,
  enableAdminUser,
  listAdminProviders,
  listAdminUsers,
  updateAdminProvider,
  updateAdminUser
} from '../api/adminApi';
import type { AdminProviderFormState, AdminUserFormState } from '../types';
import {
  emptyAdminProviderForm,
  emptyAdminUserForm,
  toAdminProviderForm,
  toAdminUserForm,
  toCreateProviderPayload,
  toCreateUserPayload,
  toUpdateProviderPayload,
  toUpdateUserPayload
} from '../utils/adminFormMappers';

export function useAdminDashboard(currentUserId?: number) {
  const [users, setUsers] = useState<User[]>([]);
  const [providers, setProviders] = useState<ProviderProfile[]>([]);
  const [selectedUserId, setSelectedUserId] = useState<number | null>(null);
  const [selectedProviderId, setSelectedProviderId] = useState<number | null>(null);
  const [userForm, setUserForm] = useState<AdminUserFormState>(emptyAdminUserForm);
  const [providerForm, setProviderForm] = useState<AdminProviderFormState>(emptyAdminProviderForm);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [isSavingUser, setIsSavingUser] = useState(false);
  const [isSavingProvider, setIsSavingProvider] = useState(false);

  const selectedUser = useMemo(() => users.find((user) => user.id === selectedUserId) ?? null, [selectedUserId, users]);
  const selectedProvider = useMemo(
    () => providers.find((provider) => provider.id === selectedProviderId) ?? null,
    [providers, selectedProviderId]
  );
  const enabledUserIds = useMemo(
    () => new Set(users.filter((user) => user.enabled).map((user) => user.id)),
    [users]
  );
  const activeProviderCount = providers.filter((provider) => provider.active && enabledUserIds.has(provider.userId)).length;
  const enabledUserCount = users.filter((user) => user.enabled).length;

  useEffect(() => {
    void loadDashboard();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function loadDashboard() {
    setIsLoading(true);
    setError(null);

    try {
      const [nextUsers, nextProviders] = await Promise.all([listAdminUsers(), listAdminProviders()]);
      setUsers(nextUsers);
      setProviders(nextProviders);
      keepSelections(nextUsers, nextProviders);
    } catch (requestError) {
      setError(apiErrorMessage(requestError));
    } finally {
      setIsLoading(false);
    }
  }

  function keepSelections(nextUsers: User[], nextProviders: ProviderProfile[]) {
    const currentUser = selectedUserId ? (nextUsers.find((user) => user.id === selectedUserId) ?? null) : null;
    const currentProvider = selectedProviderId
      ? (nextProviders.find((provider) => provider.id === selectedProviderId) ?? null)
      : null;

    if (currentUser) {
      selectUser(currentUser, nextUsers);
    }

    if (currentProvider) {
      selectProvider(currentProvider, nextProviders);
    }
  }

  function startCreateUser() {
    setSelectedUserId(null);
    setUserForm(emptyAdminUserForm);
    setMessage(null);
    setError(null);
  }

  function selectUser(user: User, source = users) {
    const current = source.find((item) => item.id === user.id) ?? user;
    setSelectedUserId(current.id);
    setUserForm(toAdminUserForm(current));
    setMessage(null);
  }

  async function handleUserSubmit(event: FormEvent): Promise<boolean> {
    event.preventDefault();
    setMessage(null);
    setError(null);

    if (selectedUser?.id === currentUserId && (!userForm.enabled || userForm.role !== 'ADMIN')) {
      setError('Non puoi disabilitare o togliere il ruolo admin al tuo account.');
      return false;
    }

    setIsSavingUser(true);

    try {
      const savedUser = selectedUser
        ? await updateAdminUser(selectedUser.id, toUpdateUserPayload(userForm))
        : await createAdminUser(toCreateUserPayload(userForm));
      setMessage(selectedUser ? 'Utente aggiornato.' : 'Utente creato.');
      await refreshUsers(savedUser.id);
      return true;
    } catch (requestError) {
      setError(apiErrorMessage(requestError));
      return false;
    } finally {
      setIsSavingUser(false);
    }
  }

  async function toggleUser(user: User) {
    setMessage(null);
    setError(null);

    if (user.id === currentUserId && user.enabled) {
      setError('Non puoi disabilitare il tuo account mentre lo stai usando.');
      return;
    }

    try {
      if (user.enabled) {
        await disableAdminUser(user.id);
        setMessage('Utente disabilitato.');
      } else {
        await enableAdminUser(user.id);
        setMessage('Utente abilitato.');
      }
      await refreshUsers(user.id);
    } catch (requestError) {
      setError(apiErrorMessage(requestError));
    }
  }

  async function removeUser(user: User) {
    setMessage(null);
    setError(null);

    if (user.id === currentUserId) {
      setError('Non puoi rimuovere il tuo account mentre lo stai usando.');
      return;
    }

    try {
      await deleteAdminUser(user.id);
      setMessage('Utente rimosso o disabilitato.');
      await refreshUsers(null);
    } catch (requestError) {
      setError(apiErrorMessage(requestError));
    }
  }

  async function refreshUsers(nextSelectedId: number | null) {
    const nextUsers = await listAdminUsers();
    setUsers(nextUsers);

    const nextSelectedUser = nextSelectedId ? (nextUsers.find((user) => user.id === nextSelectedId) ?? null) : null;
    if (nextSelectedUser) {
      selectUser(nextSelectedUser, nextUsers);
    } else {
      setSelectedUserId(null);
      setUserForm(emptyAdminUserForm);
    }
  }

  function startCreateProvider() {
    setSelectedProviderId(null);
    setProviderForm(emptyAdminProviderForm);
    setMessage(null);
    setError(null);
  }

  function selectProvider(provider: ProviderProfile, source = providers) {
    const current = source.find((item) => item.id === provider.id) ?? provider;
    setSelectedProviderId(current.id);
    setProviderForm(toAdminProviderForm(current));
    setMessage(null);
  }

  async function handleProviderSubmit(event: FormEvent): Promise<boolean> {
    event.preventDefault();
    setMessage(null);
    setError(null);
    setIsSavingProvider(true);

    try {
      const savedProvider = selectedProvider
        ? await updateAdminProvider(selectedProvider.id, toUpdateProviderPayload(providerForm))
        : await createAdminProvider(toCreateProviderPayload(providerForm));
      setMessage(selectedProvider ? 'Provider aggiornato.' : 'Provider creato.');
      await refreshProviders(savedProvider.id);
      return true;
    } catch (requestError) {
      setError(apiErrorMessage(requestError));
      return false;
    } finally {
      setIsSavingProvider(false);
    }
  }

  async function toggleProvider(provider: ProviderProfile) {
    setMessage(null);
    setError(null);

    try {
      if (provider.active) {
        await deactivateAdminProvider(provider.id);
        setMessage('Provider disattivato.');
      } else {
        await activateAdminProvider(provider.id);
        setMessage('Provider attivato.');
      }
      await refreshProviders(provider.id);
    } catch (requestError) {
      setError(apiErrorMessage(requestError));
    }
  }

  async function removeProvider(provider: ProviderProfile) {
    setMessage(null);
    setError(null);

    try {
      await deleteAdminProvider(provider.id);
      setMessage('Provider rimosso o disattivato.');
      await refreshProviders(null);
    } catch (requestError) {
      setError(apiErrorMessage(requestError));
    }
  }

  async function refreshProviders(nextSelectedId: number | null) {
    const nextProviders = await listAdminProviders();
    setProviders(nextProviders);

    const nextSelectedProvider = nextSelectedId
      ? (nextProviders.find((provider) => provider.id === nextSelectedId) ?? null)
      : null;
    if (nextSelectedProvider) {
      selectProvider(nextSelectedProvider, nextProviders);
    } else {
      setSelectedProviderId(null);
      setProviderForm(emptyAdminProviderForm);
    }
  }

  return {
    activeProviderCount,
    enabledUserCount,
    error,
    handleProviderSubmit,
    handleUserSubmit,
    isLoading,
    isSavingProvider,
    isSavingUser,
    loadDashboard,
    message,
    providerForm,
    providers,
    removeProvider,
    removeUser,
    selectProvider,
    selectUser,
    selectedProvider,
    selectedProviderId,
    selectedUser,
    selectedUserId,
    setProviderForm,
    setUserForm,
    startCreateProvider,
    startCreateUser,
    toggleProvider,
    toggleUser,
    userForm,
    users
  };
}
