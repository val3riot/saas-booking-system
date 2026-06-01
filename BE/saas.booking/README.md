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
In questa fase iniziale `V1__init_schema.sql` rappresenta il modello base pulito del progetto.

Se esiste un database locale non allineato alla migration corrente, va resettato prima dell'avvio:

```bash
docker compose -f ../../infra/docker/dev/docker-compose.yml down -v
docker compose -f ../../infra/docker/dev/docker-compose.yml up -d postgres
```

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
- `GET /api/booking-slots?providerId=&serviceId=&from=&to=`
- `GET /api/bookings`
- `POST /api/bookings`
- `GET /api/bookings/{bookingId}`
- `POST /api/bookings/{bookingId}/cancel`

Gli endpoint `/api/users/**` e gli endpoint amministrativi `/api/providers/**` richiedono ruolo `ADMIN`.
Gli endpoint `/api/providers/me/**` richiedono ruolo `PROVIDER`.
Gli endpoint `/api/bookings/**` richiedono ruolo `CUSTOMER`.
Gli endpoint `/api/catalog/**` e `/api/booking-slots` richiedono autenticazione e restituiscono solo provider/servizi attivi.
