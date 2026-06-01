# Booking Platform - Project Design

## Obiettivo del progetto

Booking Platform è una web application full stack pensata per la prenotazione di servizi professionali.

Il dominio scelto è quello della prenotazione di servizi offerti da professionisti o strutture, ad esempio:

- personal trainer;
- fisioterapisti;
- consulenti;
- studi professionali;
- coworking;
- servizi generici su appuntamento.

---

## Stack tecnologico previsto

### Backend

- Java 21
- Spring Boot 3
- Spring Security
- JWT Authentication
- Spring Data JPA / Hibernate
- PostgreSQL
- Flyway
- JUnit 5
- Mockito
- Swagger / OpenAPI

### Frontend (fase finale)

- React
- Vite
- React Router
- Axios / Fetch API
- Componenti riutilizzabili

### Infrastruttura e servizi esterni

- Docker
- Docker Compose
- Redis
- Apache Kafka
- GitHub Actions o pipeline CI/CD equivalente

---

## Attori principali

### Customer

Utente che utilizza la piattaforma per cercare professionisti e prenotare servizi.

Funzionalità previste:

- registrazione;
- login;
- gestione profilo;
- ricerca provider;
- visualizzazione servizi disponibili;
- creazione prenotazione;
- modifica prenotazione;
- annullamento prenotazione;
- consultazione storico prenotazioni;
- ricezione notifiche.

### Provider

Professionista o struttura che offre servizi prenotabili.

Funzionalità previste:

- creazione profilo provider;
- modifica dati professionali;
- gestione servizi offerti;
- configurazione disponibilità;
- gestione agenda;
- visualizzazione prenotazioni ricevute;
- accettazione o rifiuto prenotazioni;
- annullamento prenotazioni.

### Admin

Utente amministratore della piattaforma.

Funzionalità previste:

- gestione utenti;
- gestione provider;
- gestione categorie;
- visualizzazione statistiche base;
- abilitazione o disabilitazione account.

---

## Dominio applicativo

### User

Rappresenta un utente registrato nel sistema.

Ruoli previsti:

- CUSTOMER;
- PROVIDER;
- ADMIN.

Responsabilità:

- autenticazione;
- autorizzazione;
- gestione dati personali;
- associazione ai ruoli applicativi.

### Provider

Rappresenta un professionista o una struttura che offre servizi prenotabili.

Informazioni principali:

- nome attività;
- descrizione;
- categoria;
- città;
- indirizzo;
- utente associato.

### Service

Rappresenta un servizio offerto da un provider.

Esempi:

- consulenza di 60 minuti;
- seduta di fisioterapia;
- allenamento individuale;
- prenotazione postazione coworking.

Informazioni principali:

- nome;
- descrizione;
- durata;
- prezzo;
- stato attivo/non attivo.

### Availability

Rappresenta le disponibilità configurate dal provider.

Esempi:

- lunedì 09:00 - 13:00;
- mercoledì 15:00 - 19:00;
- periodo ferie o indisponibilità.

### Booking

Rappresenta una prenotazione effettuata da un customer presso un provider.

Stati previsti:

```text
PENDING
CONFIRMED
REJECTED
CANCELLED
COMPLETED
```

Informazioni principali:

- customer;
- provider;
- servizio;
- data e ora inizio;
- data e ora fine;
- stato prenotazione;
- timestamp di creazione e modifica.

### Notification

Rappresenta una notifica generata dal sistema.

Esempi:

- conferma prenotazione;
- modifica prenotazione;
- annullamento prenotazione;
- promemoria appuntamento.

### Audit Log

Rappresenta la registrazione degli eventi rilevanti del sistema.

Esempi:

- creazione prenotazione;
- annullamento prenotazione;
- modifica disponibilità;
- cambio stato prenotazione.

---

## Architettura generale

Schema logico iniziale:

```text
Spring Boot REST API
      |
      v
PostgreSQL
```

Evoluzione architetturale prevista:

```text
React Frontend
      |
      v
Spring Boot REST API
      |
      v
PostgreSQL

Redis
- cache provider
- cache disponibilità

Kafka
- eventi prenotazione
- notifiche asincrone
- audit log asincrono
```

---

## Principali vincoli di business

### Gestione doppia prenotazione

Il sistema deve impedire che due utenti prenotino lo stesso slot per lo stesso provider.

La validazione deve essere gestita lato backend e supportata da logica transazionale e/o vincoli database.

Aspetti da valutare durante lo sviluppo:

- controllo sovrapposizione oraria;
- transazioni;
- locking ottimistico o pessimistico;
- vincoli di unicità dove applicabili;
- test su casi concorrenti.

### Gestione stati prenotazione

Una prenotazione deve seguire un ciclo di vita chiaro.

Esempio:

```text
PENDING -> CONFIRMED -> COMPLETED
PENDING -> REJECTED
PENDING -> CANCELLED
CONFIRMED -> CANCELLED
```

Le transizioni non valide devono essere bloccate dal backend.

### Validità delle disponibilità

Una prenotazione può essere creata solo se:

- il provider esiste;
- il servizio è attivo;
- lo slot richiesto rientra nelle disponibilità configurate;
- non esistono altre prenotazioni sovrapposte;
- l'utente è autorizzato a effettuare l'operazione.

---

# Roadmap di sviluppo

La roadmap è pensata per far evolvere il progetto in modo progressivo. Le entity principali nascono insieme alla migration Flyway iniziale; eventuali nuove entity verranno introdotte con migration successive.

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

Obiettivo: esporre provider e servizi già presenti nel modello dati e introdurre il CRUD booking base.

Attività backend:

- implementare CRUD provider;
- implementare CRUD servizi;
- aggiungere autorizzazioni per provider e admin.
- implementare CRUD booking base collegato a Customer, Provider e OfferedService;
- impedire prenotazioni sovrapposte sullo stesso provider.

Risultato atteso:

- il sistema distingue customer e provider;
- un provider può pubblicare servizi;
- le API espongono provider e servizi in modo coerente.
- un customer può creare e gestire le proprie prenotazioni.

---

## Step 3 - Disponibilità e agenda

Obiettivo: esporre la gestione reale degli slot prenotabili.

Attività backend:

- permettere al provider di configurare giorni e fasce orarie;
- gestire ferie o blocchi temporanei;
- esporre endpoint per consultare disponibilità;
- validare sovrapposizioni o configurazioni incoerenti.

Risultato atteso:

- i provider possono configurare quando sono prenotabili;
- le API permettono di consultare le disponibilità prima della prenotazione.

---

## Step 4 - Prenotazione avanzata

Obiettivo: rendere la prenotazione aderente alle regole di dominio.

Attività backend:

- calcolare automaticamente data/ora fine in base alla durata del servizio;
- implementare transizioni di stato valide;
- impedire doppie prenotazioni;
- aggiungere controllo disponibilità;
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

# Obiettivi tecnici del progetto

Il progetto è pensato per ripassare e dimostrare competenze su:

- progettazione REST API;
- Spring Boot 3;
- Java 21;
- Spring Security;
- JWT;
- JPA/Hibernate;
- PostgreSQL;
- validazione e gestione errori;
- test unitari e di integrazione;
- caching con Redis;
- event-driven architecture con Kafka;
- Docker e Docker Compose;
- pipeline CI/CD base;
- frontend React.

---
