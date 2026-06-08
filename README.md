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
docker compose -f infra/docker/dev/docker-compose.yml up -d postgres redis
cd BE/saas.booking
cp .env.example .env
./mvnw spring-boot:run
```

Swagger:

```text
http://localhost:8080/swagger-ui.html
```

Il `.env.example` attiva il profilo `dev`; Swagger è esposto solo con quel profilo:

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

## Dati demo

Con il backend avviato puoi popolare dati di test via API:

```bash
python3 scripts/seed-demo-data.py
```

Lo script crea 10 customer, 10 provider e 3 servizi con disponibilita per ogni provider. Tutti gli utenti demo usano password `Password1!`.

Account esempio:

```text
customer01@example.test
provider01@example.test
```

Per usare un backend diverso:

```bash
python3 scripts/seed-demo-data.py --api-base-url http://localhost:8080
```
