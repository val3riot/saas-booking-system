# SaaS Booking System

Backend base per una booking platform con autenticazione JWT, ruoli applicativi, onboarding customer/provider e CRUD amministrativi.

## Stack V1

- Spring Boot 3
- Java 21
- PostgreSQL
- JWT
- Docker Compose
- Swagger/OpenAPI
- JUnit

## Avvio locale

```bash
docker compose -f ../../infra/docker/dev/docker-compose.yml up -d postgres
./mvnw spring-boot:run
```

Se vuoi far avviare PostgreSQL automaticamente da Spring Boot Docker Compose:

```bash
SPRING_DOCKER_COMPOSE_ENABLED=true ./mvnw spring-boot:run
```

Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

Swagger e OpenAPI sono disponibili solo con il profilo `dev`:

```bash
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

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
