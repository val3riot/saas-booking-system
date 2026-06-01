# SaaS Booking System

Backend base per una booking platform con autenticazione JWT, ruoli applicativi e CRUD utenti amministrativo.

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
- `POST /api/auth/login`
- `GET /api/users`
- `POST /api/users`
- `GET /api/users/{id}`
- `PUT /api/users/{id}`
- `POST /api/users/{id}/enable`
- `POST /api/users/{id}/disable`
- `DELETE /api/users/{id}`

Gli endpoint `/api/users/**` richiedono ruolo `ADMIN`.
