# SaaS Booking System

Root del progetto booking platform.

## Struttura

```text
BE/saas.booking/    Spring Boot API
FE/saas.booking.web/ React frontend
docs/                Analisi progetto e roadmap evolutiva
infra/docker/dev/    Servizi locali per sviluppo
```

## Documentazione

- [Project design](docs/project-design.md)
- [Frontend architecture](docs/frontend-architecture.md)
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

## Frontend

```bash
cd FE/saas.booking.web
npm install
npm run dev
```

Il frontend usa `http://localhost:8080` come backend di default.
Per cambiare URL:

```bash
VITE_API_BASE_URL=http://localhost:8080 npm run dev
```
