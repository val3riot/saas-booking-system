import { useMemo, useState } from 'react';
import type { FormEvent } from 'react';
import { Button } from '../../../components/Button';
import { EmptyState } from '../../../components/EmptyState';
import { FormField } from '../../../components/FormField';
import { Modal } from '../../../components/Modal';
import { StatusMessage } from '../../../components/StatusMessage';
import { useAuth } from '../../../features/auth/hooks/useAuth';
import { formatDateTime } from '../../../lib/formatters';
import type { ProviderProfile, User, UserRole } from '../../../types/api';
import { useAdminDashboard } from '../hooks/useAdminDashboard';
import type { AdminProviderFormState, AdminUserFormState } from '../types';

const roleOptions: UserRole[] = ['CUSTOMER', 'PROVIDER', 'ADMIN'];

type UserStatusFilter = 'ALL' | 'ENABLED' | 'DISABLED';
type ProviderStatusFilter = 'ALL' | 'ACTIVE' | 'INACTIVE';
type AdminAction =
  | { type: 'toggle-user'; user: User }
  | { type: 'remove-user'; user: User }
  | { type: 'toggle-provider'; provider: ProviderProfile }
  | { type: 'remove-provider'; provider: ProviderProfile };

export function AdminPage() {
  const { session } = useAuth();
  const admin = useAdminDashboard(session?.userId);
  const [userSearch, setUserSearch] = useState('');
  const [userRole, setUserRole] = useState<UserRole | 'ALL'>('ALL');
  const [userStatus, setUserStatus] = useState<UserStatusFilter>('ALL');
  const [providerSearch, setProviderSearch] = useState('');
  const [providerStatus, setProviderStatus] = useState<ProviderStatusFilter>('ALL');
  const [pendingAction, setPendingAction] = useState<AdminAction | null>(null);
  const [isConfirming, setIsConfirming] = useState(false);
  const [isUserModalOpen, setIsUserModalOpen] = useState(false);
  const [isProviderModalOpen, setIsProviderModalOpen] = useState(false);

  const currentUserId = session?.userId;

  const filteredUsers = useMemo(
    () =>
      admin.users.filter((user) => {
        const matchesSearch =
          user.email.toLowerCase().includes(userSearch.trim().toLowerCase()) || String(user.id).includes(userSearch);
        const matchesRole = userRole === 'ALL' || user.role === userRole;
        const matchesStatus =
          userStatus === 'ALL' ||
          (userStatus === 'ENABLED' && user.enabled) ||
          (userStatus === 'DISABLED' && !user.enabled);

        return matchesSearch && matchesRole && matchesStatus;
      }),
    [admin.users, userRole, userSearch, userStatus]
  );

  const filteredProviders = useMemo(
    () =>
      admin.providers.filter((provider) => {
        const normalizedSearch = providerSearch.trim().toLowerCase();
        const matchesSearch =
          provider.businessName.toLowerCase().includes(normalizedSearch) ||
          provider.category.toLowerCase().includes(normalizedSearch) ||
          provider.city.toLowerCase().includes(normalizedSearch) ||
          String(provider.id).includes(providerSearch) ||
          String(provider.userId).includes(providerSearch);
        const matchesStatus =
          providerStatus === 'ALL' ||
          (providerStatus === 'ACTIVE' && provider.active) ||
          (providerStatus === 'INACTIVE' && !provider.active);

        return matchesSearch && matchesStatus;
      }),
    [admin.providers, providerSearch, providerStatus]
  );

  const userEnabledById = useMemo(
    () => new Map(admin.users.map((user) => [user.id, user.enabled])),
    [admin.users]
  );

  function openCreateUserModal() {
    admin.startCreateUser();
    setIsUserModalOpen(true);
  }

  function openEditUserModal(user: User) {
    admin.selectUser(user);
    setIsUserModalOpen(true);
  }

  function openCreateProviderModal() {
    admin.startCreateProvider();
    setIsProviderModalOpen(true);
  }

  function openEditProviderModal(provider: ProviderProfile) {
    admin.selectProvider(provider);
    setIsProviderModalOpen(true);
  }

  async function submitUser(event: FormEvent) {
    const didSave = await admin.handleUserSubmit(event);
    if (didSave) {
      setIsUserModalOpen(false);
    }
  }

  async function submitProvider(event: FormEvent) {
    const didSave = await admin.handleProviderSubmit(event);
    if (didSave) {
      setIsProviderModalOpen(false);
    }
  }

  async function confirmPendingAction() {
    if (!pendingAction) {
      return;
    }

    setIsConfirming(true);
    try {
      if (pendingAction.type === 'toggle-user') {
        await admin.toggleUser(pendingAction.user);
      } else if (pendingAction.type === 'remove-user') {
        await admin.removeUser(pendingAction.user);
      } else if (pendingAction.type === 'toggle-provider') {
        await admin.toggleProvider(pendingAction.provider);
      } else {
        await admin.removeProvider(pendingAction.provider);
      }
      setPendingAction(null);
    } finally {
      setIsConfirming(false);
    }
  }

  return (
    <div className="mx-auto grid max-w-6xl gap-5">
      <section className="flex flex-col items-start justify-between gap-4 md:flex-row md:items-center">
        <div>
          <p className="mb-1 text-xs font-bold uppercase text-brand-600">Admin</p>
          <h1 className="text-2xl font-bold leading-tight text-slate-950">Console piattaforma</h1>
        </div>
        <Button variant="secondary" type="button" onClick={() => void admin.loadDashboard()} disabled={admin.isLoading}>
          {admin.isLoading ? 'Aggiornamento...' : 'Aggiorna'}
        </Button>
      </section>

      {admin.error && <StatusMessage tone="danger">{admin.error}</StatusMessage>}
      {admin.message && <StatusMessage tone="success">{admin.message}</StatusMessage>}

      <section className="grid gap-3 md:grid-cols-4">
        <Metric label="Utenti" value={admin.users.length} />
        <Metric label="Utenti abilitati" value={admin.enabledUserCount} />
        <Metric label="Provider" value={admin.providers.length} />
        <Metric label="Provider attivi" value={admin.activeProviderCount} />
      </section>

      <section className="grid gap-5 lg:grid-cols-2">
        <AdminUsersPanel
          currentUserId={currentUserId}
          isLoading={admin.isLoading}
          onCreate={openCreateUserModal}
          onEdit={openEditUserModal}
          onToggle={(user) => setPendingAction({ type: 'toggle-user', user })}
          selectedUserId={admin.selectedUserId}
          userRole={userRole}
          userSearch={userSearch}
          userStatus={userStatus}
          users={filteredUsers}
          totalUsers={admin.users.length}
          onRoleChange={setUserRole}
          onSearchChange={setUserSearch}
          onStatusChange={setUserStatus}
        />
        <AdminProvidersPanel
          isLoading={admin.isLoading}
          onCreate={openCreateProviderModal}
          onEdit={openEditProviderModal}
          onToggle={(provider) => setPendingAction({ type: 'toggle-provider', provider })}
          providerSearch={providerSearch}
          providerStatus={providerStatus}
          providers={filteredProviders}
          providerUserEnabledById={userEnabledById}
          selectedProviderId={admin.selectedProviderId}
          totalProviders={admin.providers.length}
          onSearchChange={setProviderSearch}
          onStatusChange={setProviderStatus}
        />
      </section>

      {isUserModalOpen && (
        <AdminUserModal
          currentUserId={currentUserId}
          form={admin.userForm}
          isSaving={admin.isSavingUser}
          selectedUser={admin.selectedUser}
          onChange={admin.setUserForm}
          onClose={() => setIsUserModalOpen(false)}
          onRemove={(user) => setPendingAction({ type: 'remove-user', user })}
          onSubmit={(event) => void submitUser(event)}
        />
      )}

      {isProviderModalOpen && (
        <AdminProviderModal
          form={admin.providerForm}
          isSaving={admin.isSavingProvider}
          selectedProvider={admin.selectedProvider}
          onChange={admin.setProviderForm}
          onClose={() => setIsProviderModalOpen(false)}
          onRemove={(provider) => setPendingAction({ type: 'remove-provider', provider })}
          onSubmit={(event) => void submitProvider(event)}
        />
      )}

      {pendingAction && (
        <AdminConfirmModal
          action={pendingAction}
          isSaving={isConfirming}
          onCancel={() => setPendingAction(null)}
          onConfirm={() => void confirmPendingAction()}
        />
      )}
    </div>
  );
}

function Metric({ label, value }: { label: string; value: number }) {
  return (
    <div className="rounded-lg border border-slate-200 bg-white p-4">
      <p className="text-sm font-semibold text-slate-500">{label}</p>
      <strong className="text-2xl text-slate-950">{value}</strong>
    </div>
  );
}

function AdminUsersPanel({
  currentUserId,
  isLoading,
  onCreate,
  onEdit,
  onRoleChange,
  onSearchChange,
  onStatusChange,
  onToggle,
  selectedUserId,
  totalUsers,
  userRole,
  userSearch,
  userStatus,
  users
}: {
  currentUserId?: number;
  isLoading: boolean;
  onCreate: () => void;
  onEdit: (user: User) => void;
  onRoleChange: (role: UserRole | 'ALL') => void;
  onSearchChange: (value: string) => void;
  onStatusChange: (status: UserStatusFilter) => void;
  onToggle: (user: User) => void;
  selectedUserId: number | null;
  totalUsers: number;
  userRole: UserRole | 'ALL';
  userSearch: string;
  userStatus: UserStatusFilter;
  users: User[];
}) {
  return (
    <section className="grid content-start gap-4 rounded-lg border border-slate-200 bg-white p-4">
      <div className="flex items-center justify-between gap-3">
        <div>
          <p className="mb-1 text-xs font-bold uppercase text-brand-600">Identity</p>
          <h2 className="text-lg font-bold text-slate-950">Utenti</h2>
        </div>
        <Button variant="ghost" type="button" onClick={onCreate}>
          Nuovo utente
        </Button>
      </div>

      <div className="grid gap-3 md:grid-cols-[minmax(0,1fr)_140px_150px]">
        <FormField
          label="Cerca"
          name="adminUserSearch"
          value={userSearch}
          onChange={(event) => onSearchChange(event.target.value)}
          placeholder="Email o ID"
        />
        <label className="grid gap-1.5 text-sm font-semibold text-slate-950">
          <span>Ruolo</span>
          <select
            className="min-h-10 w-full rounded-md border border-slate-200 bg-white px-3 py-2 text-slate-950 outline-none transition focus:border-brand-600 focus:ring-4 focus:ring-brand-600/15"
            value={userRole}
            onChange={(event) => onRoleChange(event.target.value as UserRole | 'ALL')}
          >
            <option value="ALL">Tutti</option>
            {roleOptions.map((role) => (
              <option key={role} value={role}>
                {role}
              </option>
            ))}
          </select>
        </label>
        <label className="grid gap-1.5 text-sm font-semibold text-slate-950">
          <span>Stato</span>
          <select
            className="min-h-10 w-full rounded-md border border-slate-200 bg-white px-3 py-2 text-slate-950 outline-none transition focus:border-brand-600 focus:ring-4 focus:ring-brand-600/15"
            value={userStatus}
            onChange={(event) => onStatusChange(event.target.value as UserStatusFilter)}
          >
            <option value="ALL">Tutti</option>
            <option value="ENABLED">Abilitati</option>
            <option value="DISABLED">Disabilitati</option>
          </select>
        </label>
      </div>

      {users.length === 0 ? (
        <EmptyState
          title={isLoading ? 'Utenti in caricamento' : 'Nessun utente'}
          description={
            totalUsers === 0 ? 'Gli account registrati appariranno qui.' : 'Nessun utente corrisponde ai filtri.'
          }
        />
      ) : (
        <div className="grid gap-2">
          {users.map((user) => {
            const isCurrentUser = user.id === currentUserId;
            return (
              <div
                className={[
                  'grid w-full gap-3 rounded-md border p-3 text-left',
                  selectedUserId === user.id ? 'border-brand-600 bg-teal-50' : 'border-slate-200 bg-white'
                ].join(' ')}
                key={user.id}
              >
                <div className="flex flex-wrap items-start justify-between gap-2">
                  <div className="min-w-0">
                    <strong className="break-words text-slate-950">{user.email}</strong>
                    <div className="text-sm text-slate-500">
                      #{user.id} · {user.role} · {formatDateTime(user.createdAt)}
                    </div>
                  </div>
                  <small className={user.enabled ? 'text-emerald-700' : 'text-slate-500'}>
                    {user.enabled ? 'Abilitato' : 'Disabilitato'}
                    {isCurrentUser ? ' · tu' : ''}
                  </small>
                </div>
                <div className="flex flex-wrap gap-2">
                  <Button variant="ghost" type="button" onClick={() => onEdit(user)}>
                    Modifica
                  </Button>
                  <Button
                    variant={user.enabled ? 'secondary' : 'ghost'}
                    type="button"
                    onClick={() => onToggle(user)}
                    disabled={isCurrentUser && user.enabled}
                  >
                    {user.enabled ? 'Disabilita' : 'Abilita'}
                  </Button>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </section>
  );
}

function AdminProvidersPanel({
  isLoading,
  onCreate,
  onEdit,
  onSearchChange,
  onStatusChange,
  onToggle,
  providerSearch,
  providerStatus,
  providers,
  providerUserEnabledById,
  selectedProviderId,
  totalProviders
}: {
  isLoading: boolean;
  onCreate: () => void;
  onEdit: (provider: ProviderProfile) => void;
  onSearchChange: (value: string) => void;
  onStatusChange: (status: ProviderStatusFilter) => void;
  onToggle: (provider: ProviderProfile) => void;
  providerSearch: string;
  providerStatus: ProviderStatusFilter;
  providers: ProviderProfile[];
  providerUserEnabledById: Map<number, boolean>;
  selectedProviderId: number | null;
  totalProviders: number;
}) {
  return (
    <section className="grid content-start gap-4 rounded-lg border border-slate-200 bg-white p-4">
      <div className="flex items-center justify-between gap-3">
        <div>
          <p className="mb-1 text-xs font-bold uppercase text-brand-600">Marketplace</p>
          <h2 className="text-lg font-bold text-slate-950">Provider</h2>
        </div>
        <Button variant="ghost" type="button" onClick={onCreate}>
          Nuovo provider
        </Button>
      </div>

      <div className="grid gap-3 md:grid-cols-[minmax(0,1fr)_150px]">
        <FormField
          label="Cerca"
          name="adminProviderSearch"
          value={providerSearch}
          onChange={(event) => onSearchChange(event.target.value)}
          placeholder="Nome, categoria, citta o ID"
        />
        <label className="grid gap-1.5 text-sm font-semibold text-slate-950">
          <span>Stato</span>
          <select
            className="min-h-10 w-full rounded-md border border-slate-200 bg-white px-3 py-2 text-slate-950 outline-none transition focus:border-brand-600 focus:ring-4 focus:ring-brand-600/15"
            value={providerStatus}
            onChange={(event) => onStatusChange(event.target.value as ProviderStatusFilter)}
          >
            <option value="ALL">Tutti</option>
            <option value="ACTIVE">Attivi</option>
            <option value="INACTIVE">Non attivi</option>
          </select>
        </label>
      </div>

      {providers.length === 0 ? (
        <EmptyState
          title={isLoading ? 'Provider in caricamento' : 'Nessun provider'}
          description={
            totalProviders === 0
              ? 'I profili provider registrati appariranno qui.'
              : 'Nessun provider corrisponde ai filtri.'
          }
        />
      ) : (
        <div className="grid gap-2">
          {providers.map((provider) => {
            const accountEnabled = providerUserEnabledById.get(provider.userId) ?? false;
            const isOperational = provider.active && accountEnabled;
            return (
              <div
                className={[
                  'grid w-full gap-3 rounded-md border p-3 text-left',
                  selectedProviderId === provider.id ? 'border-brand-600 bg-teal-50' : 'border-slate-200 bg-white'
                ].join(' ')}
                key={provider.id}
              >
                <div className="flex flex-wrap items-start justify-between gap-2">
                  <div className="min-w-0">
                    <strong className="break-words text-slate-950">{provider.businessName}</strong>
                    <div className="text-sm text-slate-500">
                      #{provider.id} · User #{provider.userId} · {provider.category} · {provider.city}
                    </div>
                  </div>
                  <small className={isOperational ? 'text-emerald-700' : 'text-slate-500'}>
                    {isOperational ? 'Attivo' : accountEnabled ? 'Non attivo' : 'Account disabilitato'}
                  </small>
                </div>
                <div className="flex flex-wrap gap-2">
                  <Button variant="ghost" type="button" onClick={() => onEdit(provider)}>
                    Modifica
                  </Button>
                  <Button
                    variant={provider.active ? 'secondary' : 'ghost'}
                    type="button"
                    onClick={() => onToggle(provider)}
                    disabled={!accountEnabled && !provider.active}
                  >
                    {provider.active ? 'Disattiva' : 'Attiva'}
                  </Button>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </section>
  );
}

function AdminUserModal({
  currentUserId,
  form,
  isSaving,
  onChange,
  onClose,
  onRemove,
  onSubmit,
  selectedUser
}: {
  currentUserId?: number;
  form: AdminUserFormState;
  isSaving: boolean;
  onChange: (form: AdminUserFormState) => void;
  onClose: () => void;
  onRemove: (user: User) => void;
  onSubmit: (event: FormEvent) => void;
  selectedUser: User | null;
}) {
  const isCurrentUser = selectedUser?.id === currentUserId;
  const canEditRole = !selectedUser || (selectedUser.role !== 'PROVIDER' && !isCurrentUser);
  const selectableRoles = selectedUser?.role === 'PROVIDER'
    ? roleOptions
    : selectedUser
      ? roleOptions.filter((role) => role !== 'PROVIDER')
      : roleOptions.filter((role) => role !== 'PROVIDER');

  return (
    <Modal
      title={selectedUser ? `Modifica utente #${selectedUser.id}` : 'Nuovo utente'}
      eyebrow="Utenti"
      description={selectedUser?.email}
      onClose={onClose}
      footer={
        <div className="flex flex-wrap justify-end gap-2">
          {selectedUser && (
            <Button variant="ghost" type="button" onClick={() => onRemove(selectedUser)} disabled={isSaving || isCurrentUser}>
              Rimuovi utente
            </Button>
          )}
          <Button variant="ghost" type="button" onClick={onClose} disabled={isSaving}>
            Annulla
          </Button>
          <Button type="submit" form="admin-user-form" disabled={isSaving}>
            {isSaving ? 'Salvataggio...' : selectedUser ? 'Salva utente' : 'Crea utente'}
          </Button>
        </div>
      }
    >
      <form id="admin-user-form" className="grid gap-3" onSubmit={onSubmit}>
        {isCurrentUser && (
          <StatusMessage tone="warning">
            Non puoi disabilitare, rimuovere o togliere il ruolo admin al tuo account mentre lo stai usando.
          </StatusMessage>
        )}
        {selectedUser?.role === 'PROVIDER' && (
          <StatusMessage tone="warning">
            Il ruolo provider non viene convertito da qui: servizi, agenda e prenotazioni richiedono una gestione dedicata.
          </StatusMessage>
        )}
        {selectedUser?.role === 'CUSTOMER' && selectedUser.enabled && !form.enabled && (
          <StatusMessage tone="warning">
            Disabilitando questo customer verranno cancellate le sue prenotazioni attive future o in corso.
          </StatusMessage>
        )}
        <FormField
          label="Email"
          name="adminUserEmail"
          type="email"
          value={form.email}
          onChange={(event) => onChange({ ...form, email: event.target.value })}
          required
        />
        {!selectedUser && (
          <FormField
            label="Password"
            name="adminUserPassword"
            type="password"
            value={form.password}
            onChange={(event) => onChange({ ...form, password: event.target.value })}
            required
          />
        )}
        <label className="grid gap-1.5 text-sm font-semibold text-slate-950">
          <span>Ruolo</span>
          <select
            className="min-h-10 w-full rounded-md border border-slate-200 bg-white px-3 py-2 text-slate-950 outline-none transition focus:border-brand-600 focus:ring-4 focus:ring-brand-600/15 disabled:bg-slate-100 disabled:text-slate-500"
            value={form.role}
            onChange={(event) => onChange({ ...form, role: event.target.value as UserRole })}
            disabled={!canEditRole}
          >
            {selectableRoles.map((role) => (
              <option key={role} value={role}>
                {role}
              </option>
            ))}
          </select>
        </label>
        <label className="flex items-center gap-2 text-sm font-semibold text-slate-950">
          <input
            className="size-4 accent-brand-600 disabled:accent-slate-400"
            type="checkbox"
            checked={form.enabled}
            onChange={(event) => onChange({ ...form, enabled: event.target.checked })}
            disabled={isCurrentUser}
          />
          <span>Account abilitato</span>
        </label>
      </form>
    </Modal>
  );
}

function AdminProviderModal({
  form,
  isSaving,
  onChange,
  onClose,
  onRemove,
  onSubmit,
  selectedProvider
}: {
  form: AdminProviderFormState;
  isSaving: boolean;
  onChange: (form: AdminProviderFormState) => void;
  onClose: () => void;
  onRemove: (provider: ProviderProfile) => void;
  onSubmit: (event: FormEvent) => void;
  selectedProvider: ProviderProfile | null;
}) {
  return (
    <Modal
      title={selectedProvider ? `Modifica provider #${selectedProvider.id}` : 'Nuovo provider'}
      eyebrow="Provider"
      description={selectedProvider?.businessName}
      onClose={onClose}
      footer={
        <div className="flex flex-wrap justify-end gap-2">
          {selectedProvider && (
            <Button variant="ghost" type="button" onClick={() => onRemove(selectedProvider)} disabled={isSaving}>
              Rimuovi provider
            </Button>
          )}
          <Button variant="ghost" type="button" onClick={onClose} disabled={isSaving}>
            Annulla
          </Button>
          <Button type="submit" form="admin-provider-form" disabled={isSaving}>
            {isSaving ? 'Salvataggio...' : selectedProvider ? 'Salva provider' : 'Crea provider'}
          </Button>
        </div>
      }
    >
      <form id="admin-provider-form" className="grid gap-3" onSubmit={onSubmit}>
        {selectedProvider ? (
          <StatusMessage tone="warning">
            Account collegato: User #{selectedProvider.userId}. Il collegamento account/provider non si cambia da questa
            modifica.
          </StatusMessage>
        ) : (
          <>
            <FormField
              label="Email account"
              name="adminProviderEmail"
              type="email"
              value={form.email}
              onChange={(event) => onChange({ ...form, email: event.target.value })}
              required
            />
            <FormField
              label="Password account"
              name="adminProviderPassword"
              type="password"
              value={form.password}
              onChange={(event) => onChange({ ...form, password: event.target.value })}
              required
            />
          </>
        )}
        {selectedProvider?.active && !form.active && (
          <StatusMessage tone="warning">
            Disattivando questo provider verranno cancellate le prenotazioni attive future o in corso. I servizi restano
            configurati, ma non saranno prenotabili finche il provider resta inattivo.
          </StatusMessage>
        )}
        <FormField
          label="Nome attivita"
          name="adminProviderBusinessName"
          value={form.businessName}
          onChange={(event) => onChange({ ...form, businessName: event.target.value })}
          required
        />
        <div className="grid gap-3 md:grid-cols-2">
          <FormField
            label="Categoria"
            name="adminProviderCategory"
            value={form.category}
            onChange={(event) => onChange({ ...form, category: event.target.value })}
            required
          />
          <FormField
            label="Citta"
            name="adminProviderCity"
            value={form.city}
            onChange={(event) => onChange({ ...form, city: event.target.value })}
            required
          />
        </div>
        <FormField
          label="Indirizzo"
          name="adminProviderAddress"
          value={form.address}
          onChange={(event) => onChange({ ...form, address: event.target.value })}
        />
        <FormField
          label="Descrizione"
          name="adminProviderDescription"
          value={form.description}
          onChange={(event) => onChange({ ...form, description: event.target.value })}
          multiline
          rows={3}
        />
        <label className="flex items-center gap-2 text-sm font-semibold text-slate-950">
          <input
            className="size-4 accent-brand-600"
            type="checkbox"
            checked={form.active}
            onChange={(event) => onChange({ ...form, active: event.target.checked })}
          />
          <span>Provider attivo</span>
        </label>
      </form>
    </Modal>
  );
}

function AdminConfirmModal({
  action,
  isSaving,
  onCancel,
  onConfirm
}: {
  action: AdminAction;
  isSaving: boolean;
  onCancel: () => void;
  onConfirm: () => void;
}) {
  const content = adminActionContent(action);

  return (
    <Modal
      title={content.title}
      eyebrow="Conferma operazione"
      description={content.description}
      onClose={onCancel}
      footer={
        <div className="flex flex-wrap justify-end gap-2">
          <Button variant="ghost" type="button" onClick={onCancel} disabled={isSaving}>
            Annulla
          </Button>
          <Button type="button" onClick={onConfirm} disabled={isSaving}>
            {isSaving ? 'Elaborazione...' : content.confirmLabel}
          </Button>
        </div>
      }
    >
      <StatusMessage tone={content.tone}>{content.warning}</StatusMessage>
    </Modal>
  );
}

function adminActionContent(action: AdminAction): {
  title: string;
  description: string;
  warning: string;
  confirmLabel: string;
  tone: 'warning' | 'danger';
} {
  if (action.type === 'toggle-user') {
    const verb = action.user.enabled ? 'disabilitare' : 'abilitare';
    const customerBookingWarning =
      action.user.enabled && action.user.role === 'CUSTOMER'
        ? ' Le prenotazioni attive future o in corso del customer verranno cancellate.'
        : '';
    return {
      title: `${action.user.enabled ? 'Disabilita' : 'Abilita'} utente`,
      description: action.user.email,
      warning: `Confermi di voler ${verb} questo account? L'accesso alla piattaforma cambiera immediatamente.${customerBookingWarning}`,
      confirmLabel: action.user.enabled ? 'Disabilita' : 'Abilita',
      tone: 'warning'
    };
  }

  if (action.type === 'remove-user') {
    return {
      title: 'Rimuovi utente',
      description: action.user.email,
      warning:
        'Operazione amministrativa sensibile: il backend puo rimuovere o disabilitare definitivamente questo account.',
      confirmLabel: 'Rimuovi utente',
      tone: 'danger'
    };
  }

  if (action.type === 'toggle-provider') {
    const verb = action.provider.active ? 'disattivare' : 'attivare';
    const providerBookingWarning = action.provider.active
      ? ' Le prenotazioni attive future o in corso verranno cancellate; i servizi resteranno configurati ma non prenotabili.'
      : '';
    return {
      title: `${action.provider.active ? 'Disattiva' : 'Attiva'} provider`,
      description: action.provider.businessName,
      warning: `Confermi di voler ${verb} questo provider? La visibilita nel catalogo cambiera immediatamente.${providerBookingWarning}`,
      confirmLabel: action.provider.active ? 'Disattiva' : 'Attiva',
      tone: 'warning'
    };
  }

  return {
    title: 'Rimuovi provider',
    description: action.provider.businessName,
    warning:
      'Operazione amministrativa sensibile: il backend puo rimuovere o disattivare definitivamente questo provider.',
    confirmLabel: 'Rimuovi provider',
    tone: 'danger'
  };
}
