import { Navigate, Route, Routes } from 'react-router-dom';
import { AppShell } from '../layouts/AppShell';
import { AdminPage } from '../features/admin/pages/AdminPage';
import { AuthForm } from '../features/auth/pages/AuthForm';
import { useAuth } from '../features/auth/hooks/useAuth';
import { BookingsPage } from '../features/bookings/pages/BookingsPage';
import { CatalogPage } from '../features/catalog/pages/CatalogPage';
import { ProtectedRoute } from '../routes/ProtectedRoute';

export function App() {
  return (
    <Routes>
      <Route path="/login" element={<AuthForm mode="login" />} />
      <Route path="/register" element={<AuthForm mode="customer" />} />
      <Route path="/register-provider" element={<AuthForm mode="provider" />} />

      <Route element={<ProtectedRoute />}>
        <Route element={<AppShell />}>
          <Route index element={<HomeRedirect />} />
          <Route path="/catalog" element={<CatalogPage />} />
          <Route path="/bookings" element={<BookingsPage />} />
        </Route>
      </Route>

      <Route element={<ProtectedRoute allowedRoles={['ADMIN']} />}>
        <Route element={<AppShell />}>
          <Route path="/admin" element={<AdminPage />} />
        </Route>
      </Route>

      <Route path="*" element={<Navigate to="/catalog" replace />} />
    </Routes>
  );
}

function HomeRedirect() {
  const { session } = useAuth();
  return <Navigate to={session?.role === 'ADMIN' ? '/admin' : '/catalog'} replace />;
}
