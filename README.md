# SaaS Booking System

Root del progetto booking platform.

## Struttura

```text
BE/saas.booking/    Spring Boot API
docs/                Analisi progetto e roadmap evolutiva
infra/docker/dev/    Servizi locali per sviluppo
```

## Documentazione

- [Project design](docs/project-design.md)
- [Roadmap](docs/roadmap.md)

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

Swagger è esposto solo con profilo `dev`:

```bash
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```
