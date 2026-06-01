# SaaS Booking System

Root del progetto booking platform.

## Struttura

```text
BE/saas.booking/    Spring Boot API
docs/                Documentazione architetturale e appunti futuri
infra/docker/dev/    Servizi locali per sviluppo
```

## Backend

```bash
docker compose -f infra/docker/dev/docker-compose.yml up -d postgres
cd BE/saas.booking
./mvnw spring-boot:run
```

Swagger:

```text
http://localhost:8080/swagger-ui.html
```
