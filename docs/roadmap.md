# Booking Platform - Roadmap

La roadmap descrive l'evoluzione incrementale del progetto. Il documento di analisi del dominio resta in [project-design.md](./project-design.md).

---

## Stato attuale

Aggiornato al 2026-06-05.

- Step 1 completato.
- Step 2 completato nei flussi principali.
- Step 3 completato lato backend nei punti principali: indisponibilità, slot bloccati, agenda provider, cancellazioni tracciate.
- Step 4 sostanzialmente completato lato backend per stati booking, conferma/rifiuto/cancellazione provider e audit log sincrono.
- Step 5 parzialmente coperto dal catalogo e dai filtri disponibili, ma va ancora consolidato come ricerca paginabile/ordinabile.
- Step 6 implementato con frontend React collegato al backend per auth, catalogo, booking, gestione provider e admin.

Prima di passare a caching/eventi asincroni conviene mantenere stabile l'invariante operativo appena consolidato: un provider e i suoi servizi sono prenotabili solo quando il profilo provider è attivo e l'account utente collegato è abilitato.

Nota per le notifiche: oggi le cancellazioni automatiche causate da disabilitazione customer/provider aggiornano il booking e scrivono audit log nello stesso flusso sincrono. Quando verrà introdotto lo step eventi/notifiche, questa parte andrà rivista per pubblicare eventi di dominio e generare notifiche verso customer e provider senza accoppiare direttamente i service principali al canale di notifica.

---

## Step 1 - Base backend

Obiettivo: stabilizzare la base backend e definire il modello dati iniziale.

Attività:

- rivedere struttura dei package;
- separare controller, service, repository, DTO, mapper ed exception;
- pulire eventuale codice generato o ridondante;
- verificare configurazione Spring Security;
- consolidare autenticazione JWT;
- introdurre ruoli CUSTOMER, PROVIDER e ADMIN;
- proteggere il CRUD utenti con ruolo ADMIN;
- uniformare naming di classi, endpoint e DTO;
- aggiungere gestione errori centralizzata con `@ControllerAdvice`;
- introdurre validazione input con Bean Validation;
- configurare Swagger / OpenAPI;
- configurare Flyway per versionare lo schema database;
- creare schema iniziale per User, Provider, OfferedService, Availability, Booking, Notification e AuditLog;
- introdurre entity JPA coerenti con lo schema Flyway;
- scrivere test sugli endpoint principali;
- configurare profili Spring per sviluppo e test.

Risultato atteso:

- backend avviabile;
- autenticazione funzionante;
- CRUD utenti funzionante;
- struttura progetto leggibile;
- API documentate;
- errori gestiti in modo uniforme;
- database versionato con schema iniziale completo;
- primi test automatici presenti.

---

## Step 2 - API provider, servizi e booking

Obiettivo: abilitare l'onboarding autonomo dei provider, esporre catalogo e servizi, configurare disponibilità per servizio, e introdurre il booking customer base.

Attività backend:

- implementare registrazione autonoma provider;
- implementare gestione self-service del profilo provider;
- mantenere endpoint admin per modifiche critiche e moderazione;
- implementare CRUD servizi offerti dal provider;
- implementare CRUD disponibilità per singolo servizio offerto;
- esporre endpoint catalogo read-only per consultare provider e servizi attivi;
- esporre endpoint per generare slot prenotabili su intervallo data;
- aggiungere autorizzazioni per provider e admin;
- implementare booking customer base collegato a Customer, Provider e OfferedService;
- calcolare automaticamente data/ora fine in base alla durata del servizio;
- validare che la data richiesta cada nel giorno e nella fascia oraria del servizio scelto;
- impedire prenotazioni sovrapposte sullo stesso provider.

Risultato atteso:

- il sistema distingue customer e provider;
- customer e provider possono registrarsi autonomamente;
- un provider può gestire profilo, servizi e disponibilità;
- le API espongono provider e servizi attivi tramite catalogo;
- un customer può consultare slot disponibili e creare o annullare le proprie prenotazioni.

---

## Step 3 - Agenda provider e indisponibilità

Obiettivo: rendere l'agenda più aderente a un caso reale di produzione.

Attività backend:

- introdurre una entity per indisponibilità puntuali del provider;
- supportare blocchi per intera giornata o fascia oraria;
- permettere blocchi validi per tutti i servizi o per un singolo servizio;
- aggiornare la generazione slot per restituire come bloccati gli slot coperti da indisponibilità;
- impedire la creazione di booking su slot bloccati;
- esporre endpoint provider per gestione indisponibilità;
- esporre endpoint provider per agenda giornaliera/settimanale;
- aggiungere metadati di cancellazione booking;
- bloccare transizioni di cancellazione non valide;
- aggiungere test su blocchi orari, booking già presenti, slot disponibili e cancellazioni.

Risultato atteso:

- i provider possono controllare agenda e blocchi temporanei;
- il customer vede slot calcolati tenendo conto anche delle indisponibilità puntuali;
- la cancellazione di una prenotazione è tracciata e pronta per generare eventi futuri.

---

## Step 4 - Prenotazione avanzata

Obiettivo: rendere la prenotazione aderente alle regole di dominio.

Attività backend:

- implementare transizioni di stato valide;
- introdurre eventuale modello a conferma manuale con stato iniziale `PENDING`;
- permettere al provider di confermare o rifiutare prenotazioni se il modello non è a conferma automatica;
- esporre endpoint provider per gestire le prenotazioni ricevute;
- tracciare gli eventi principali in audit log;
- aggiungere modifica prenotazione o richiesta di ripianificazione;
- rafforzare test su casi concorrenti;
- aggiungere test sui casi principali e sui casi limite.

Risultato atteso:

- il flusso di prenotazione diventa il cuore dell'applicazione;
- le regole principali sono protette da validazioni backend e test.

---

## Step 5 - Ricerca e filtri

Obiettivo: preparare API di ricerca provider efficienti e paginabili.

Attività backend:

- endpoint di ricerca provider;
- filtro per categoria;
- filtro per città;
- filtro per disponibilità;
- paginazione e ordinamento.

Risultato atteso:

- le API consentono ricerche realistiche sui provider;
- il backend espone contratti pronti per la futura interfaccia utente.

---

## Step 6 - Frontend React

Obiettivo: costruire l'interfaccia utente dopo avere contratti backend principali stabili e prima degli strati architetturali.

Attività:

- inizializzare progetto React con Vite;
- definire struttura cartelle per feature e componenti condivisi;
- configurare routing;
- creare layout principale;
- creare pagina login;
- creare pagina registrazione;
- creare dashboard customer;
- creare dashboard provider;
- visualizzare provider, servizi e disponibilità;
- implementare creazione e gestione prenotazioni;
- aggiungere sezione admin;
- configurare client HTTP per comunicare con il backend;
- gestire salvataggio token JWT lato frontend.

Risultato atteso:

- frontend avviabile;
- flussi principali collegati al backend;
- interfaccia pronta per mostrare le successive evoluzioni architetturali.

---

## Step 7 - Redis caching

Obiettivo: introdurre caching solo dopo avere endpoint stabili e sensati da ottimizzare.

Possibili casi d'uso:

- cache lista provider;
- cache dettaglio provider;
- cache disponibilità provider.

Attività:

- configurare Redis in Docker Compose;
- integrare Redis nel backend;
- applicare caching sugli endpoint più letti;
- gestire invalidazione cache quando cambiano provider, servizi o disponibilità;
- documentare le scelte nel README.

Risultato atteso:

- Redis viene usato per un motivo concreto;
- la cache non è introdotta come tecnologia isolata.

---

## Step 8 - Kafka ed eventi asincroni

Obiettivo: separare alcune operazioni secondarie dal flusso sincrono della prenotazione.

Eventi previsti:

```text
BookingCreatedEvent
BookingUpdatedEvent
BookingCancelledEvent
BookingConfirmedEvent
BookingRejectedEvent
```

Consumer previsti:

- Notification Consumer;
- Audit Log Consumer.

Attività:

- configurare Kafka in Docker Compose;
- pubblicare eventi dal dominio Booking;
- creare consumer per notifiche;
- creare consumer per audit log;
- gestire retry o error handling di base;
- testare il flusso end-to-end.

Risultato atteso:

- la creazione/modifica prenotazione genera eventi;
- notifiche e audit log non sono accoppiati direttamente al service principale.

---

## Step 9 - Containerizzazione completa

Obiettivo: rendere il progetto facilmente avviabile da repository.

Servizi previsti:

- frontend React;
- backend Spring Boot;
- PostgreSQL;
- Redis;
- Kafka.

Attività:

- creare Dockerfile backend;
- creare Dockerfile frontend;
- completare Docker Compose;
- configurare variabili ambiente;
- documentare comandi di avvio;
- verificare setup da clone pulito.

Risultato atteso:

- il progetto può essere avviato con pochi comandi;
- il repository è più credibile e facile da valutare.

---

## Step 10 - CI/CD e rifinitura finale

Obiettivo: aggiungere una pipeline minima e rendere il progetto presentabile.

Attività:

- configurare GitHub Actions;
- eseguire build backend;
- eseguire test automatici;
- eventualmente buildare immagini Docker;
- migliorare README;
- aggiungere screenshot frontend;
- documentare architettura e scelte tecniche;
- aggiungere esempi di request/response API;
- completare Swagger.

Risultato atteso:

- progetto presentabile in colloquio;
- repository leggibile;
- pipeline minima funzionante.
