#!/usr/bin/env python3
"""Seed demo users, providers, services and availabilities through the public API.

Usage:
    python3 scripts/seed-demo-data.py
    python3 scripts/seed-demo-data.py --api-base-url http://localhost:8080
"""

from __future__ import annotations

import argparse
import json
import sys
import urllib.error
import urllib.request
from dataclasses import dataclass
from typing import Any


DEFAULT_API_BASE_URL = "http://localhost:8080"
DEFAULT_PASSWORD = "Password1!"


@dataclass(frozen=True)
class DemoService:
    name: str
    description: str
    duration_minutes: int
    price_cents: int


@dataclass(frozen=True)
class DemoProvider:
    email: str
    business_name: str
    description: str
    category: str
    city: str
    address: str
    services: tuple[DemoService, ...]


PROVIDERS: tuple[DemoProvider, ...] = (
    DemoProvider(
        "provider01@example.test",
        "Studio Aurora",
        "Consulenza business per piccole imprese e startup.",
        "consulting",
        "Milano",
        "Via Torino 12",
        (
            DemoService("Consulenza strategica", "Sessione per obiettivi, priorita e piano operativo.", 60, 9000),
            DemoService("Business review", "Analisi di processi, costi e opportunita di crescita.", 90, 13000),
            DemoService("Check-up startup", "Validazione rapida di modello, pitch e metriche.", 45, 7000),
        ),
    ),
    DemoProvider(
        "provider02@example.test",
        "Wellness Forma",
        "Servizi wellness e trattamenti personalizzati.",
        "wellness",
        "Roma",
        "Via Appia 45",
        (
            DemoService("Massaggio rilassante", "Trattamento defaticante e antistress.", 60, 6500),
            DemoService("Percorso postura", "Valutazione e routine personalizzata.", 75, 8500),
            DemoService("Sessione mindfulness", "Pratica guidata individuale.", 45, 5000),
        ),
    ),
    DemoProvider(
        "provider03@example.test",
        "Clinica Verde",
        "Prestazioni sanitarie e visite specialistiche di base.",
        "medical",
        "Torino",
        "Corso Francia 88",
        (
            DemoService("Visita generale", "Controllo medico generale.", 30, 5500),
            DemoService("Consulto nutrizionale", "Prima valutazione nutrizionale.", 60, 8000),
            DemoService("Follow-up", "Controllo successivo alla prima visita.", 30, 4000),
        ),
    ),
    DemoProvider(
        "provider04@example.test",
        "Legal Lab",
        "Supporto legale per contratti, privacy e societa.",
        "legal",
        "Bologna",
        "Via Indipendenza 20",
        (
            DemoService("Revisione contratto", "Analisi di clausole e rischi principali.", 60, 12000),
            DemoService("Consulenza privacy", "Check GDPR e indicazioni operative.", 75, 15000),
            DemoService("Parere rapido", "Inquadramento iniziale del problema.", 30, 6000),
        ),
    ),
    DemoProvider(
        "provider05@example.test",
        "Design Nodo",
        "Brand identity, UX e direzione creativa.",
        "design",
        "Firenze",
        "Via dei Servi 9",
        (
            DemoService("UX audit", "Analisi di usabilita e priorita di miglioramento.", 90, 14000),
            DemoService("Brand workshop", "Sessione guidata su identita e posizionamento.", 120, 22000),
            DemoService("Review landing page", "Feedback operativo su contenuti e layout.", 45, 7500),
        ),
    ),
    DemoProvider(
        "provider06@example.test",
        "Tech Bridge",
        "Consulenza tecnica per web app e integrazioni.",
        "technology",
        "Napoli",
        "Via Toledo 101",
        (
            DemoService("Code review", "Revisione tecnica di architettura e codice.", 90, 16000),
            DemoService("API planning", "Definizione contratti API e integrazioni.", 60, 11000),
            DemoService("Debug session", "Sessione guidata su un problema tecnico.", 45, 9000),
        ),
    ),
    DemoProvider(
        "provider07@example.test",
        "Fit Studio Delta",
        "Allenamento individuale e piani fitness.",
        "fitness",
        "Genova",
        "Via XX Settembre 33",
        (
            DemoService("Personal training", "Allenamento individuale in studio.", 60, 6000),
            DemoService("Piano fitness", "Valutazione e programma personalizzato.", 75, 8000),
            DemoService("Check tecnica", "Correzione movimenti e postura.", 30, 3500),
        ),
    ),
    DemoProvider(
        "provider08@example.test",
        "Lingua Viva",
        "Lezioni individuali e preparazione esami.",
        "education",
        "Padova",
        "Piazza Garibaldi 6",
        (
            DemoService("Lezione inglese", "Lezione one-to-one su obiettivi personali.", 60, 4500),
            DemoService("Preparazione colloquio", "Simulazione e feedback per interview.", 45, 4000),
            DemoService("Assessment livello", "Valutazione competenze linguistiche.", 30, 2500),
        ),
    ),
    DemoProvider(
        "provider09@example.test",
        "Casa Chiara",
        "Servizi per casa, organizzazione e manutenzione.",
        "home",
        "Verona",
        "Via Mazzini 14",
        (
            DemoService("Home organizing", "Consulenza per organizzare spazi domestici.", 90, 9000),
            DemoService("Sopralluogo manutenzione", "Valutazione interventi e priorita.", 60, 5000),
            DemoService("Consulenza arredo", "Suggerimenti per layout e funzionalita.", 75, 8500),
        ),
    ),
    DemoProvider(
        "provider10@example.test",
        "Foto Prisma",
        "Servizi fotografici per persone e attivita.",
        "photography",
        "Palermo",
        "Via Liberta 72",
        (
            DemoService("Ritratto professionale", "Sessione fotografica per profili e CV.", 60, 9500),
            DemoService("Foto prodotto", "Mini shooting per prodotti e cataloghi.", 120, 18000),
            DemoService("Consulenza visual", "Analisi immagine coordinata e contenuti.", 45, 6500),
        ),
    ),
)


CUSTOMER_EMAILS = tuple(f"customer{index:02d}@example.test" for index in range(1, 11))


class ApiClient:
    def __init__(self, base_url: str) -> None:
        self.base_url = base_url.rstrip("/")

    def request(
        self,
        method: str,
        path: str,
        body: dict[str, Any] | None = None,
        token: str | None = None,
    ) -> tuple[int, Any]:
        data = None if body is None else json.dumps(body).encode("utf-8")
        headers = {"Content-Type": "application/json"}
        if token:
            headers["Authorization"] = f"Bearer {token}"

        request = urllib.request.Request(
            f"{self.base_url}{path}",
            data=data,
            headers=headers,
            method=method,
        )

        try:
            with urllib.request.urlopen(request, timeout=20) as response:
                content = response.read().decode("utf-8")
                return response.status, json.loads(content) if content else None
        except urllib.error.HTTPError as error:
            content = error.read().decode("utf-8")
            try:
                payload = json.loads(content) if content else None
            except json.JSONDecodeError:
                payload = {"message": content}
            return error.code, payload


def auth_token(response: Any) -> str:
    if not isinstance(response, dict) or "token" not in response:
        raise RuntimeError(f"Auth response does not contain a token: {response}")
    return str(response["token"])


def register_or_login_customer(api: ApiClient, email: str) -> str:
    status, response = api.request(
        "POST",
        "/api/auth/register",
        {"email": email, "password": DEFAULT_PASSWORD},
    )
    if status == 200:
        print(f"created customer {email}")
        return auth_token(response)

    status, response = api.request(
        "POST",
        "/api/auth/login",
        {"email": email, "password": DEFAULT_PASSWORD},
    )
    if status != 200:
        raise RuntimeError(f"Cannot create or login customer {email}: {response}")

    print(f"customer already exists {email}")
    return auth_token(response)


def register_or_login_provider(api: ApiClient, provider: DemoProvider) -> str:
    status, response = api.request(
        "POST",
        "/api/auth/register/provider",
        {
            "email": provider.email,
            "password": DEFAULT_PASSWORD,
            "businessName": provider.business_name,
            "description": provider.description,
            "category": provider.category,
            "city": provider.city,
            "address": provider.address,
        },
    )
    if status == 200:
        print(f"created provider {provider.email} ({provider.business_name})")
        return auth_token(response)

    status, response = api.request(
        "POST",
        "/api/auth/login",
        {"email": provider.email, "password": DEFAULT_PASSWORD},
    )
    if status != 200:
        raise RuntimeError(f"Cannot create or login provider {provider.email}: {response}")

    print(f"provider already exists {provider.email} ({provider.business_name})")
    return auth_token(response)


def list_services(api: ApiClient, token: str) -> list[dict[str, Any]]:
    status, response = api.request("GET", "/api/providers/me/services", token=token)
    if status != 200:
        raise RuntimeError(f"Cannot list provider services: {response}")
    return list(response)


def ensure_service(api: ApiClient, token: str, service: DemoService) -> int:
    existing = next((item for item in list_services(api, token) if item["name"] == service.name), None)
    if existing:
        print(f"  service already exists: {service.name}")
        return int(existing["id"])

    status, response = api.request(
        "POST",
        "/api/providers/me/services",
        {
            "name": service.name,
            "description": service.description,
            "durationMinutes": service.duration_minutes,
            "priceCents": service.price_cents,
        },
        token=token,
    )
    if status != 201:
        raise RuntimeError(f"Cannot create service {service.name}: {response}")

    print(f"  created service: {service.name}")
    return int(response["id"])


def list_availabilities(api: ApiClient, token: str, service_id: int) -> list[dict[str, Any]]:
    status, response = api.request(
        "GET",
        f"/api/providers/me/services/{service_id}/availabilities",
        token=token,
    )
    if status != 200:
        raise RuntimeError(f"Cannot list service availabilities: {response}")
    return list(response)


def ensure_availability(
    api: ApiClient,
    token: str,
    service_id: int,
    day_of_week: int,
    start_time: str,
    end_time: str,
) -> None:
    existing = list_availabilities(api, token, service_id)
    if any(
        item["dayOfWeek"] == day_of_week
        and item["startTime"] == start_time
        and item["endTime"] == end_time
        and item["active"]
        for item in existing
    ):
        return

    status, response = api.request(
        "POST",
        f"/api/providers/me/services/{service_id}/availabilities",
        {
            "dayOfWeek": day_of_week,
            "startTime": start_time,
            "endTime": end_time,
        },
        token=token,
    )
    if status != 201:
        raise RuntimeError(f"Cannot create availability for service {service_id}: {response}")


def seed_provider_services(api: ApiClient, token: str, provider: DemoProvider) -> None:
    for index, service in enumerate(provider.services):
        service_id = ensure_service(api, token, service)
        if index == 0:
            ranges = ((1, "09:00", "13:00"), (3, "14:00", "18:00"), (5, "09:00", "13:00"))
        elif index == 1:
            ranges = ((2, "10:00", "14:00"), (4, "15:00", "19:00"))
        else:
            ranges = ((1, "15:00", "18:00"), (6, "09:00", "12:00"))

        for day_of_week, start_time, end_time in ranges:
            ensure_availability(api, token, service_id, day_of_week, start_time, end_time)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Seed demo data through the SaaS Booking API.")
    parser.add_argument("--api-base-url", default=DEFAULT_API_BASE_URL, help="Backend API base URL.")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    api = ApiClient(args.api_base_url)

    print(f"Seeding demo data on {args.api_base_url}")
    print(f"Demo password for every generated user: {DEFAULT_PASSWORD}")

    try:
        for email in CUSTOMER_EMAILS:
            register_or_login_customer(api, email)

        for provider in PROVIDERS:
            token = register_or_login_provider(api, provider)
            seed_provider_services(api, token, provider)
    except (RuntimeError, urllib.error.URLError) as error:
        print(f"Seed failed: {error}", file=sys.stderr)
        return 1

    print("Seed completed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
