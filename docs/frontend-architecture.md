# Frontend Architecture

## Contesto

Il frontend e una applicazione React + Vite + TypeScript per una Booking Platform con API REST Spring Boot e autenticazione JWT.

Ruoli applicativi:

- `CUSTOMER`
- `PROVIDER`
- `ADMIN`

## Problemi rilevati nella struttura precedente

- Alcuni file core erano in cartelle generiche: layout e route guard stavano in `components`.
- Auth context era in `app`, mentre appartiene al dominio `auth`.
- Le pagine di feature erano direttamente nella root della feature, insieme a API e tipi.
- Il client HTTP era in `lib`, mescolando infrastruttura applicativa e utility generiche.
- `ProviderServicesPage` conteneva troppa orchestration async oltre al rendering.
- Le funzioni pure di mapping form/API erano inizialmente dentro componenti o hook.

## Struttura attuale

```txt
src/
  app/
    App.tsx
  components/
    Button.tsx
    EmptyState.tsx
    FormField.tsx
    Modal.tsx
    StatusMessage.tsx
  config/
    env.ts
  features/
    admin/
      api/
      hooks/
      pages/
      utils/
    auth/
      api/
      context/
      hooks/
      pages/
    bookings/
      api/
      hooks/
      pages/
    catalog/
      api/
      components/
      hooks/
      pages/
      types.ts
      utils/
  layouts/
    AppShell.tsx
    AuthLayout.tsx
  lib/
    apiErrors.ts
    formatters.ts
  routes/
    ProtectedRoute.tsx
  services/
    http/
      apiClient.ts
      sessionStorage.ts
  types/
    api.ts
  utils/
    dateUtils.ts
```

## Regole di fattorizzazione

- `app`: bootstrap logico dell'applicazione e composizione route principale.
- `layouts`: strutture di pagina condivise, come shell autenticata e layout auth.
- `routes`: route guard e configurazione routing riusabile.
- `components`: componenti UI generici e riusabili, senza logica di dominio.
- `features/<domain>/pages`: pagine route-level della feature.
- `features/<domain>/components`: componenti specifici della feature.
- `features/<domain>/hooks`: orchestration React, stato, loading, errori e workflow async.
- `features/<domain>/api`: funzioni API specifiche della feature.
- `features/<domain>/utils`: mapper e funzioni pure specifiche della feature.
- `services/http`: client HTTP centralizzato, JWT/session storage e infrastruttura remota.
- `types`: tipi condivisi da piu feature, in particolare request/response API.
- `lib`: helper trasversali non legati a una feature specifica.
- `utils`: funzioni pure trasversali usabili da piu feature, ad esempio date input helpers.
- `config`: lettura e normalizzazione configurazioni ambiente.

## API layer

Il client centralizzato e `src/services/http/apiClient.ts`.

Responsabilita:

- base URL da `src/config/env.ts`;
- serializzazione JSON;
- header `Authorization: Bearer <token>`;
- verifica del JWT persistito prima delle richieste autenticate;
- pulizia sessione su token scaduto o risposte `401`/`403`;
- gestione risposte `204`;
- normalizzazione errori con `ApiError`;
- helper `queryString`.

Le feature non usano direttamente `fetch`: espongono funzioni dedicate nei rispettivi file `api`.

## Auth

La gestione auth vive in `features/auth/context/AuthContext.tsx`.

Responsabilita:

- leggere token persistito;
- scrivere token dopo login/registrazione;
- logout;
- esporre sessione e ruolo;
- reagire alla cancellazione sessione emessa dal client HTTP quando il backend respinge il token.

La persistenza JWT vive in `services/http/sessionStorage.ts`.

`sessionStorage.ts` decodifica il JWT solo per estrarre metadati UI (`sub`, `email`, `role`, `exp`). La validazione reale resta responsabilita del backend.

Il hook `useAuth` vive in `features/auth/hooks/useAuth.ts`, separato dal provider per mantenere Fast Refresh pulito.

## Routing

`app/App.tsx` compone le route pubbliche e protette.

- Route pubbliche: login e registrazione.
- Route protette: catalogo/servizi e prenotazioni.
- Route admin: `/admin`, protetta con `allowedRoles={['ADMIN']}`.
- `routes/ProtectedRoute.tsx` protegge le route autenticate e puo limitare l'accesso per ruolo.
- Il comportamento role-based customer/provider rimane dentro `CatalogPage` e `BookingsPage`; l'area admin ha una route dedicata.

## Provider workflow

La pagina `features/catalog/pages/ProviderServicesPage.tsx` e ora un componente di composizione.

La logica async e di stato e in `features/catalog/hooks/useProviderServices.ts`, inclusi:

- caricamento servizi;
- caricamento disponibilita;
- caricamento indisponibilita;
- creazione/modifica/disattivazione/rimozione servizi;
- creazione/modifica/disattivazione/rimozione disponibilita;
- gestione indisponibilita multi-servizio;
- gestione conflitti con modale.

Le conversioni form/API sono in `features/catalog/utils/providerFormMappers.ts`.

## Customer catalog workflow

La pagina `features/catalog/pages/CatalogPage.tsx` decide se mostrare area customer o provider in base al ruolo.

La logica customer e in `features/catalog/hooks/useCustomerCatalog.ts`, inclusi:

- ricerca provider;
- paginazione risultati;
- selezione provider;
- caricamento servizi;
- caricamento slot prenotabili;
- conferma booking;
- gestione loading, errori e messaggi.

La pagina resta responsabile della composizione UI, lista slot e modale di conferma.

## Admin workflow

La feature admin vive in `features/admin`.

Struttura:

- `api/adminApi.ts`: chiamate a `/api/users` e `/api/providers`;
- `hooks/useAdminDashboard.ts`: stato, caricamento, selezione e azioni CRUD;
- `pages/AdminPage.tsx`: console visuale per utenti e provider;
- `utils/adminFormMappers.ts`: conversioni form/API.

La route `/admin` e disponibile solo per `ADMIN`.

Funzionalita implementate:

- riepilogo utenti/provider;
- lista utenti;
- filtri utenti per testo, ruolo e stato;
- creazione e modifica utenti non-provider;
- abilitazione/disabilitazione utenti;
- rimozione utenti;
- lista provider;
- filtri provider per testo e stato;
- creazione provider in un unico passaggio con account e profilo attivita;
- modifica provider;
- attivazione/disattivazione provider;
- rimozione provider.

Le azioni amministrative sensibili passano da modale di conferma prima di chiamare il backend.

### Regola account/provider

Nel dominio, `User` rappresenta identita e accesso; `ProviderProfile` rappresenta l'attivita. Un provider operativo richiede entrambe le parti.

Per questo la console admin non crea un utente con ruolo `PROVIDER` e poi un profilo separato. Il flusso corretto e:

1. l'admin apre `Nuovo provider`;
2. inserisce email/password account e dati attivita;
3. il FE invia `POST /api/providers`;
4. il backend crea in transazione account `PROVIDER` e profilo provider.

La modale `Nuovo utente` non espone il ruolo `PROVIDER`. Anche la modifica utente blocca conversioni da o verso `PROVIDER`: eventuali migrazioni di ruolo richiedono un workflow dedicato, perche possono coinvolgere servizi, disponibilita, agenda e prenotazioni.

## Utility globali

Le utility riusabili tra piu feature vanno in `src/utils`.

Esempi:

- `todayDateInputValue`
- `toDateTimeLocal`

Le utility specifiche di dominio restano invece nella feature, ad esempio `features/catalog/utils/providerFormMappers.ts`.

## Configurazione ambiente

Creare `.env` partendo da `.env.example`:

```txt
VITE_API_BASE_URL=http://localhost:8080
```

## Qualita

Script disponibili:

```txt
npm run lint
npm run format
npm run format:check
npm run test
npm run typecheck
npm run build
```

Tooling configurato:

- React 18;
- TypeScript;
- Vite 5;
- React Router 6;
- Tailwind CSS;
- Fetch API tramite client HTTP centralizzato;
- ESLint flat config per TypeScript e React Hooks;
- Prettier con `.prettierignore` per asset generati;
- Vitest con jsdom;
- React Testing Library e jest-dom pronti per test componenti;
- test base sulle utility globali e sui mapper catalog/provider.

Nota audit: `npm audit` segnala 2 vulnerabilita moderate su `esbuild` transitivamente via Vite 5. La fix automatica richiede upgrade major a Vite 8; va pianificata come aggiornamento dedicato per evitare regressioni sul dev server/tooling.

## Prossimi miglioramenti consigliati

- Aggiungere test React Testing Library per modali e workflow booking.
- Valutare alias TypeScript/Vite (`@/features/...`) quando la profondita degli import cresce.
- Pianificare upgrade Vite major per chiudere il warning audit su esbuild.
