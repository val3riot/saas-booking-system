# SaaS Booking System

Backend base per una booking platform con autenticazione JWT, ruoli applicativi, onboarding customer/provider e CRUD amministrativi.

## Stack V1

- Spring Boot 3
- Java 21
- PostgreSQL
- Redis
- JWT
- Docker Compose
- Swagger/OpenAPI
- JUnit

## Avvio locale

Creare la configurazione locale, che resta esclusa da Git:

```bash
cp .env.example .env
```

Il backend importa automaticamente `.env` quando viene avviato da
`BE/saas.booking`. Il template attiva il profilo `dev` e contiene solo valori
adatti allo sviluppo locale.

Avviare l'infrastruttura e l'applicazione:

```bash
docker compose -f ../../infra/docker/dev/docker-compose.yml up -d postgres redis
./mvnw spring-boot:run
```

Se vuoi far avviare i servizi Docker Compose automaticamente da Spring Boot:

```bash
SPRING_DOCKER_COMPOSE_ENABLED=true ./mvnw spring-boot:run
```

Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

Swagger e OpenAPI sono disponibili solo con il profilo `dev`, già configurato
nel `.env.example`:

```bash
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

Fuori dallo sviluppo locale `SPRING_DATASOURCE_URL`,
`SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `REDIS_HOST` e
`JWT_SECRET` sono obbligatorie. Non usare il secret JWT del template in ambienti
condivisi o di produzione.

Test:

```bash
./mvnw test
```

## Struttura package

Il backend usa package-by-feature: le classi che appartengono allo stesso dominio restano vicine.

```text
it.booking
  auth
  booking
  catalog
  provider
  offering
  availability
  user
  notification
  audit
  common
  config
```

Le modifiche schema DB sono gestite da Flyway in `src/main/resources/db/migration`.
`V1__init_schema.sql` rappresenta il modello iniziale; le evoluzioni successive devono essere aggiunte con nuove migration versionate (`V2`, `V3`, ...), senza modificare migration già applicate.

## Endpoint iniziali

- `GET /api/health`
- `POST /api/auth/register`
- `POST /api/auth/register/provider`
- `POST /api/auth/login`
- `GET /api/catalog/providers`
- `GET /api/catalog/providers/search?query=&category=&city=&availableOn=&page=&size=&sort=&direction=`
- `GET /api/catalog/providers/{providerId}`
- `GET /api/catalog/providers/{providerId}/services`
- `GET /api/catalog/providers/{providerId}/services/{serviceId}`
- `GET /api/users`
- `POST /api/users`
- `GET /api/users/{id}`
- `PUT /api/users/{id}`
- `POST /api/users/{id}/enable`
- `POST /api/users/{id}/disable`
- `DELETE /api/users/{id}`
- `GET /api/providers`
- `POST /api/providers`
- `GET /api/providers/{id}`
- `PUT /api/providers/{id}`
- `POST /api/providers/{id}/activate`
- `POST /api/providers/{id}/deactivate`
- `DELETE /api/providers/{id}`
- `GET /api/providers/me`
- `POST /api/providers/me`
- `PUT /api/providers/me`
- `GET /api/providers/me/services`
- `POST /api/providers/me/services`
- `GET /api/providers/me/services/{serviceId}`
- `PUT /api/providers/me/services/{serviceId}`
- `POST /api/providers/me/services/{serviceId}/activate`
- `POST /api/providers/me/services/{serviceId}/deactivate`
- `DELETE /api/providers/me/services/{serviceId}`
- `GET /api/providers/me/services/{serviceId}/availabilities`
- `POST /api/providers/me/services/{serviceId}/availabilities`
- `GET /api/providers/me/services/{serviceId}/availabilities/{availabilityId}`
- `PUT /api/providers/me/services/{serviceId}/availabilities/{availabilityId}`
- `POST /api/providers/me/services/{serviceId}/availabilities/{availabilityId}/activate`
- `POST /api/providers/me/services/{serviceId}/availabilities/{availabilityId}/deactivate`
- `DELETE /api/providers/me/services/{serviceId}/availabilities/{availabilityId}`
- `GET /api/providers/me/availability-exceptions`
- `POST /api/providers/me/availability-exceptions`
- `GET /api/providers/me/availability-exceptions/{exceptionId}`
- `PUT /api/providers/me/availability-exceptions/{exceptionId}`
- `POST /api/providers/me/availability-exceptions/{exceptionId}/activate`
- `POST /api/providers/me/availability-exceptions/{exceptionId}/deactivate`
- `DELETE /api/providers/me/availability-exceptions/{exceptionId}`
- `GET /api/providers/me/agenda?from=&to=`
- `GET /api/providers/me/bookings`
- `GET /api/providers/me/bookings/{bookingId}`
- `POST /api/providers/me/bookings/{bookingId}/confirm`
- `POST /api/providers/me/bookings/{bookingId}/reject`
- `POST /api/providers/me/bookings/{bookingId}/cancel`
- `GET /api/booking-slots?providerId=&serviceId=&from=&to=`
- `GET /api/bookings`
- `POST /api/bookings`
- `GET /api/bookings/{bookingId}`
- `POST /api/bookings/{bookingId}/cancel`

Gli endpoint `/api/users/**` e gli endpoint amministrativi `/api/providers/**` richiedono ruolo `ADMIN`.
Gli endpoint `/api/providers/me/**` richiedono ruolo `PROVIDER`.
Gli endpoint `/api/bookings/**` richiedono ruolo `CUSTOMER`.
Gli endpoint `/api/catalog/**` e `/api/booking-slots` richiedono autenticazione e restituiscono solo provider/servizi attivi.

`POST /api/providers` e il workflow admin per creare un provider in modo atomico. Il payload include credenziali account e dati attivita:

```json
{
  "email": "studio@example.com",
  "password": "Password1!",
  "businessName": "Studio Fisio",
  "description": "Fisioterapia e riabilitazione",
  "category": "wellness",
  "city": "Milano",
  "address": "Via Roma 1"
}
```

Il backend crea nella stessa transazione un `AppUser` con ruolo `PROVIDER` e il relativo profilo `Provider`.

`POST /api/users` non accetta il ruolo `PROVIDER` e `PUT /api/users/{id}` non consente conversioni da o verso `PROVIDER`. Le modifiche di ruolo provider richiedono un workflow dedicato, per evitare account provider senza attivita o profili con servizi, disponibilita e prenotazioni incoerenti.

Disabilitare un customer ha effetto operativo immediato:

- i token gia emessi non vengono piu accettati perche il filtro JWT ricontrolla l'utente persistito e il flag `enabled`;
- le prenotazioni del customer in stato `PENDING` o `CONFIRMED` con `endsAt` futuro vengono cancellate;
- la cancellazione registra `cancelledBy` sull'admin che ha eseguito l'operazione e un audit `BOOKING_CANCELLED`;
- le prenotazioni passate non vengono riscritte.

Disattivare un provider ha effetto operativo immediato:

- il provider non viene piu restituito dal catalogo e i suoi servizi non sono piu bookable;
- le prenotazioni del provider in stato `PENDING` o `CONFIRMED` con `endsAt` futuro vengono cancellate;
- i servizi non vengono disattivati fisicamente: restano configurati per consentire riattivazione del provider senza perdere catalogo operativo;
- disabilitare l'account utente di un provider disattiva anche il relativo profilo provider;
- le prenotazioni passate non vengono riscritte.

La ricerca catalogo provider supporta:

- `query`: ricerca su nome attività e descrizione;
- `category`: filtro esatto case-insensitive sulla categoria;
- `city`: filtro esatto case-insensitive sulla città;
- `availableOn`: filtro per provider con almeno una disponibilità attiva nel giorno richiesto;
- `page` e `size`: paginazione, con size massima 100;
- `sort`: `BUSINESS_NAME`, `CITY`, `CATEGORY`;
- `direction`: `ASC` o `DESC`.

La risposta paginata usa un contratto stabile con `content`, `page`, `size`, `totalElements`, `totalPages`, `first` e `last`.

## Cache Redis

Redis viene usato solo per letture pubbliche del catalogo con riuso prevedibile:

- ricerca provider iniziale del frontend, senza filtri (`5m`);
- dettaglio provider (`15m`);
- servizi prenotabili del provider (`10m`);
- dettaglio servizio (`10m`).

Le ricerche filtrate non vengono memorizzate. Slot, prenotazioni, agenda,
disponibilita amministrative e dati utente restano calcolati o letti live.
Le scritture invalidano le cache coinvolte; il TTL resta una protezione aggiuntiva.
PostgreSQL e la source of truth: Redis contiene soltanto copie temporanee dei dati.

La cache opera in modalita fail-open. Se Redis non e disponibile, le letture
proseguono interrogando PostgreSQL e gli errori di scrittura o invalidazione della
cache non annullano le operazioni applicative. Un dato non invalidato durante il
guasto puo restare in cache fino alla scadenza del relativo TTL dopo il ripristino.
I timeout di connessione e comando sono configurabili con
`REDIS_CONNECT_TIMEOUT` e `REDIS_COMMAND_TIMEOUT`.

I TTL sono configurabili tramite `CACHE_TTL_PROVIDER_SEARCH`,
`CACHE_TTL_PROVIDER_DETAILS`, `CACHE_TTL_PROVIDER_SERVICES` e
`CACHE_TTL_SERVICE_DETAILS`.

Le availability exception permettono al provider di bloccare fasce temporali puntuali:

- `serviceId` valorizzato: blocco valido solo per quel servizio;
- `serviceId` nullo: blocco valido per tutti i servizi del provider.

Gli slot coperti da un blocco vengono restituiti come `BLOCKED` e non sono prenotabili.
Un blocco attivo non può sovrapporsi a prenotazioni `PENDING` o `CONFIRMED`.

Le prenotazioni create dal customer nascono in stato `PENDING`.
Il provider può confermare o rifiutare le prenotazioni ricevute.
Gli stati `PENDING` e `CONFIRMED` bloccano lo slot; gli stati `REJECTED` e `CANCELLED` liberano lo slot.

Le prenotazioni possono essere cancellate dal customer tramite `POST /api/bookings/{bookingId}/cancel`.
La cancellazione è permessa solo dagli stati `PENDING` e `CONFIRMED`; cancellazioni ripetute o stati finali restituiscono `BOOK_006`.
Il payload può includere un motivo opzionale:

```json
{
  "reason": "Cambio programma"
}
```

La tabella `audit_logs` traccia gli eventi business principali del dominio booking:

- `BOOKING_CREATED`;
- `BOOKING_CONFIRMED`;
- `BOOKING_REJECTED`;
- `BOOKING_CANCELLED`.

L'audit trail registra actor, entity, entity id, event type, payload e timestamp. Non sostituisce i log tecnici applicativi.
Le tipologiche dell'audit sono modellate nel codice tramite enum e vincolate a database tramite check constraint Flyway.
