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

## Stack tecnologico di riferimento

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

### Frontend

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

- registrazione autonoma come fornitore;
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
- moderazione o modifica critica dei provider;
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

Rappresenta le disponibilità ricorrenti configurate dal provider per uno specifico servizio.

Esempi:

- consulenza: lunedì 09:00 - 13:00;
- visita specialistica: mercoledì 15:00 - 19:00;

Le disponibilità ricorrenti definiscono quando un servizio può generare slot prenotabili.
Le indisponibilità puntuali sono gestite separatamente, perché rappresentano eccezioni al calendario ordinario.

### Availability Exception

Rappresenta un blocco puntuale impostato dal provider.

Esempi:

- ferie;
- permesso personale;
- chiusura straordinaria;
- indisponibilità di un singolo servizio in una fascia oraria.

Regole principali:

- può bloccare tutti i servizi del provider;
- può bloccare un solo servizio specifico;
- può coprire una fascia oraria o una giornata intera;
- non può sovrapporsi a prenotazioni attive;
- gli slot coperti dal blocco vengono restituiti come `BLOCKED`;
- una prenotazione non può essere creata su uno slot bloccato.

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
- eventuale data di cancellazione;
- eventuale utente che ha cancellato;
- eventuale motivo di cancellazione;
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
- conferma prenotazione;
- rifiuto prenotazione;
- annullamento prenotazione;
- modifica disponibilità;
- cambio stato prenotazione.

L'audit log ha finalità di tracciamento business e accountability. Non sostituisce logging tecnico, metriche o tracing applicativo.

---

## Architettura generale

Schema logico iniziale:

```text
Spring Boot REST API
      |
      v
PostgreSQL
```

Architettura target:

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

Transizioni principali:

```text
PENDING -> CONFIRMED
PENDING -> REJECTED
PENDING -> CANCELLED
CONFIRMED -> CANCELLED
CONFIRMED -> COMPLETED
```

Le transizioni non valide devono essere bloccate dal backend.
Una prenotazione già cancellata non può essere cancellata di nuovo.

### Regole di disponibilità e slot

Gli slot prenotabili sono generati dal backend a partire da:

- servizio scelto;
- durata del servizio;
- disponibilità ricorrenti del servizio;
- booking già presenti;
- indisponibilità puntuali del provider.

Uno slot può essere:

```text
AVAILABLE
BOOKED
BLOCKED
```

Gli stati `PENDING` e `CONFIRMED` bloccano lo slot. Gli stati `CANCELLED`, `REJECTED` e `COMPLETED` non devono impedire nuove prenotazioni sullo stesso intervallo.

### Audit business

Gli eventi di dominio rilevanti devono essere tracciati in `audit_logs`.

Eventi booking principali:

```text
BOOKING_CREATED
BOOKING_CONFIRMED
BOOKING_REJECTED
BOOKING_CANCELLED
```

Ogni record di audit contiene:

- utente attore;
- tipo evento;
- tipo entità;
- id entità;
- payload descrittivo;
- timestamp.

### Validità delle disponibilità

Una prenotazione può essere creata solo se:

- il provider esiste;
- il servizio è attivo;
- lo slot richiesto rientra nelle disponibilità configurate;
- lo slot richiesto non è coperto da indisponibilità puntuali;
- non esistono altre prenotazioni sovrapposte;
- l'utente è autorizzato a effettuare l'operazione.

### Versionamento database

- Tool: Flyway.
- Directory: `BE/saas.booking/src/main/resources/db/migration`.
- Naming: `V{numero}__descrizione.sql`.
- `V1__init_schema.sql`: schema iniziale consolidato.
- Migration applicata: immutabile.
- Nuove modifiche schema: nuova migration progressiva.
- Entity JPA: allineate allo schema versionato.
- Validazione test: Flyway attivo e Hibernate `ddl-auto=validate`.
- Tipologiche di dominio: rappresentate nel codice con enum o value object dedicati.
- Tipologiche persistite: vincolate su database tramite lookup table o check constraint.

---

## Pianificazione evolutiva

La roadmap operativa è mantenuta in [roadmap.md](./roadmap.md).

---

## Obiettivi tecnici del progetto

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
