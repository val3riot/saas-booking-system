# SaaS Booking Web

Frontend React per la Booking Platform.

## Stack

- React 18
- TypeScript
- Vite 5
- React Router 6
- Tailwind CSS
- Fetch API con client HTTP centralizzato
- Vitest + jsdom
- React Testing Library + jest-dom
- ESLint flat config + Prettier

## Avvio

```bash
npm install
npm run dev
```

Configura l'URL del backend con:

```bash
VITE_API_BASE_URL=http://localhost:8080
```

Se la variabile non e' valorizzata, il frontend usa `http://localhost:8080`.

## Script

```bash
npm run lint
npm run format:check
npm run test
npm run typecheck
npm run build
```

## Architettura

Il codice e organizzato per feature:

- `features/auth`: login, registrazione customer/provider, sessione JWT;
- `features/catalog`: catalogo customer e dashboard provider;
- `features/bookings`: prenotazioni customer e agenda provider;
- `features/admin`: console admin per utenti e provider;
- `services/http`: client API, token storage e gestione sessione scaduta;
- `components`, `layouts`, `routes`, `lib`, `utils`: parti condivise.

La documentazione di dettaglio e in [`../../docs/frontend-architecture.md`](../../docs/frontend-architecture.md).

## Catalogo customer

La pagina catalogo usa `/api/catalog/providers/search` e permette ricerca per testo, categoria, citta, disponibilita su data, ordinamento, direzione e numero di risultati per pagina.

Il dettaglio provider carica i servizi prenotabili e gli slot disponibili per servizio/data. La conferma slot crea una prenotazione customer tramite `/api/bookings`.

## Regole di dominio rilevanti

Un provider non viene creato come semplice utente con ruolo `PROVIDER`.

La creazione admin del provider e un singolo passaggio: la modale provider invia email/password dell'account e dati dell'attivita a `POST /api/providers`. Il backend crea in transazione account `PROVIDER` e profilo provider.

La creazione/modifica utenti da `/api/users` non permette conversioni da o verso `PROVIDER`: quelle richiedono un workflow provider dedicato per non lasciare servizi, disponibilita e prenotazioni in stati incoerenti.
