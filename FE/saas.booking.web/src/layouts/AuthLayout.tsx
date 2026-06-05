import type { PropsWithChildren } from 'react';

export function AuthLayout({ children }: PropsWithChildren) {
  return (
    <main className="grid min-h-screen place-items-center bg-slate-50 p-6">
      <section
        className="w-full max-w-xl rounded-lg border border-slate-200 bg-white p-7"
        aria-label="Accesso piattaforma"
      >
        <div className="mb-7 flex items-center gap-4">
          <span className="grid size-9 place-items-center rounded-lg bg-slate-950 text-xs font-bold text-white">
            SB
          </span>
          <div>
            <h1 className="text-2xl font-bold leading-tight text-slate-950">SaaS Booking</h1>
            <p className="text-slate-500">Prenotazioni, agenda e servizi in un unico flusso.</p>
          </div>
        </div>
        {children}
      </section>
    </main>
  );
}
