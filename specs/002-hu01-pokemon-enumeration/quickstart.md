# Quickstart validation: HU-01 Pokemon Enumeration

**Feature**: `002-hu01-pokemon-enumeration` | **Date**: 2026-10-16

Contracts: [pokemon-list-api.yaml](./contracts/pokemon-list-api.yaml),
[env-config.md](./contracts/env-config.md). Data model: [data-model.md](./data-model.md).

## Prerequisites

- Feature 001 stack runnable (`docker compose`, JDK 21, Node not required for API checks)
- Network access to PokeAPI **or** WireMock stubs for offline IT

## 1. Configure environment

```bash
cp .env.example .env
# Ensure POKEAPI_BASE_URL=https://pokeapi.co/api/v2 (or WireMock base)
```

Expected: `POSTGRES_*` and `POKEAPI_BASE_URL` present. Missing
`POKEAPI_BASE_URL` in Compose fails fast naming the key (per env contract).

## 2. Start the stack

```bash
docker compose up --build -d
docker compose ps
```

Expected: postgres, backend, frontend healthy/up as in 001.

## 3. Happy path — first page (SC-001, SC-002)

```bash
curl -s -w "\n%{http_code}\n" "http://localhost:8080/api/v1/pokemon"
```

Expected: `200`; JSON page with `size` default 9 (or fewer only if total &lt; 9);
entries in National Dex order; fields per contract; `totalElements` matches
live catalog count (not a hard-coded 1025 in code); `totalPages` consistent.

Via proxy (path unchanged):

```bash
curl -s -w "\n%{http_code}\n" "http://localhost:3000/api/v1/pokemon?page=1&size=9"
```

## 4. Domain-visible mapping samples

```bash
# Bulbasaur weight 69 hg → 6.9 kg; English name/genus present
curl -s "http://localhost:8080/api/v1/pokemon?page=1&size=1"
```

Expected: `weightKg` one decimal; `name` English display form; `category`
English genus or null; ability `name` values are slugs.

## 5. Validation / paging errors (SC-005)

```bash
curl -s -i "http://localhost:8080/api/v1/pokemon?page=0"
curl -s -i "http://localhost:8080/api/v1/pokemon?size=21"
curl -s -i "http://localhost:8080/api/v1/pokemon?page=99999&size=20"
```

Expected: each `400` with `Content-Type` problem+json (RFC 7807), no stack
trace body.

## 6. Catalog failure (SC-005)

Point `POKEAPI_BASE_URL` at a closed port or stop WireMock; restart backend;
request a page.

Expected: `502` Problem Detail; never a 200 with partial content.

## 7. Cache (SC-003, SC-004)

```bash
# First uncached page — allow ≤ 3s when PokeAPI is healthy
time curl -s -o /dev/null -w "%{http_code} %{time_total}\n" \
  "http://localhost:8080/api/v1/pokemon?page=1&size=9"
# Immediate repeat — expect ≤ 0.2s and no new upstream traffic (observe via
# WireMock request journal in IT, or app metrics/logs if exposed)
time curl -s -o /dev/null -w "%{http_code} %{time_total}\n" \
  "http://localhost:8080/api/v1/pokemon?page=1&size=9"
```

## 8. OpenAPI

```bash
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/v3/api-docs
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/swagger-ui.html
```

Expected: docs available (springdoc 3.1.1). Exact UI path may redirect; follow
redirects.

## 9. Backend verify (TR-UNIT / R6)

```bash
cd backend && ./mvnw -B verify
```

Expected: domain/application/infrastructure/web tests green; JaCoCo check ≥
80% **per module**; optional aggregate report under `web/target/site/jacoco-aggregate`
(or equivalent) for visibility only.

## Tear down

```bash
docker compose down -v
```
