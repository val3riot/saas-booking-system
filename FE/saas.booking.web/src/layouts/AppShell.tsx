import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { Button } from '../components/Button';
import { useAuth } from '../features/auth/hooks/useAuth';

const navLinkClass = ({ isActive }: { isActive: boolean }) =>
  [
    'rounded-md px-3 py-2 text-sm font-semibold no-underline transition-colors',
    isActive ? 'bg-slate-100 text-slate-950' : 'text-slate-500 hover:bg-slate-50 hover:text-slate-900'
  ].join(' ');

export function AppShell() {
  const { session, logout } = useAuth();
  const navigate = useNavigate();
  const catalogLabel = session?.role === 'PROVIDER' ? 'Servizi' : 'Catalogo';

  function handleLogout() {
    logout();
    navigate('/login');
  }

  return (
    <div className="min-h-screen">
      <header className="sticky top-0 z-10 flex min-h-16 flex-col items-start gap-4 border-b border-slate-200 bg-white px-5 py-4 md:flex-row md:items-center md:justify-between md:px-7">
        <NavLink
          to="/"
          className="flex items-center gap-3 font-bold text-slate-950 no-underline"
          aria-label="SaaS Booking"
        >
          <span className="grid size-9 place-items-center rounded-lg bg-slate-950 text-xs font-bold text-white">
            SB
          </span>
          <span className="whitespace-nowrap">SaaS Booking</span>
        </NavLink>

        <nav className="flex items-center gap-1" aria-label="Navigazione principale">
          {session?.role === 'ADMIN' ? (
            <NavLink to="/admin" className={navLinkClass}>
              Admin
            </NavLink>
          ) : (
            <>
              <NavLink to="/catalog" className={navLinkClass}>
                {catalogLabel}
              </NavLink>
              <NavLink to="/bookings" className={navLinkClass}>
                Prenotazioni
              </NavLink>
            </>
          )}
        </nav>

        <div className="flex flex-wrap items-center gap-3 text-sm text-slate-500">
          {session?.email && <span className="max-w-56 truncate">{session.email}</span>}
          <Button variant="ghost" type="button" onClick={handleLogout}>
            Esci
          </Button>
        </div>
      </header>

      <main className="px-5 py-5 md:px-7 md:py-7">
        <Outlet />
      </main>
    </div>
  );
}
