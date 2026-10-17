# Quickstart validation: HU-02 Detailed View

**Feature**: `003-hu02-detailed-view` | **Date**: 2026-10-16

Contracts: [pokemon-detail-api.yaml](./contracts/pokemon-detail-api.yaml),
[env-config.md](./contracts/env-config.md). Data model: [data-model.md](./data-model.md).

## Prerequisites

- Feature 002 stack runnable (`docker compose`, JDK 21)
- Network access to PokeAPI **or** WireMock stubs for offline IT
- No new env keys (reuse `POKEAPI_BASE_URL` from 002)

## 1. Configure & start

```bash
cp .env.example .env
docker compose up --build -d
docker compose ps
```

Expected: postgres, backend, frontend healthy/up.

## 2. Happy path — Bulbasaur (SC-001)

```bash
curl -s -w "\n%{http_code}\n" "http://localhost:8080/api/v1/pokemon/1"
```

Expected: `200`; identity fields; `heightM` / `weightKg` one decimal;
`stats.total` = sum of six bases; `flavorText.text` normalised English;
`evolution` tree with `isCurrent: true` on national number 1; `imageUrl`
preferring official artwork when present.

Via proxy:

```bash
curl -s -w "\n%{http_code}\n" "http://localhost:3000/api/v1/pokemon/1"
```

## 3. Branching lineage — Eevee (SC-002)

```bash
curl -s "http://localhost:8080/api/v1/pokemon/133" | python3 -m json.tool | head -80
```

Expected: root Eevee with **multiple** children (trigger / minLevel / itemSlug
from first `evolution_details`); each child node has name + image; requesting
133 marks Eevee `isCurrent: true`.

## 4. Single-stage species

```bash
# e.g. Tauros — adjust id if catalog differs
curl -s "http://localhost:8080/api/v1/pokemon/128" | python3 -c \
  'import sys,json; e=json.load(sys.stdin)["evolution"]; print(e["nationalNumber"], e["isCurrent"], len(e["children"]))'
```

Expected: `isCurrent` true; `children` empty (or length 0).

## 5. Errors (SC-005)

```bash
curl -s -i "http://localhost:8080/api/v1/pokemon/0"
curl -s -i "http://localhost:8080/api/v1/pokemon/abc"
curl -s -i "http://localhost:8080/api/v1/pokemon/99999"
```

Expected: `0` and `abc` → **400** problem+json; `99999` (or other missing
National Dex id) → **404** problem+json; never stack traces or upstream URLs.

Catalog down (point `POKEAPI_BASE_URL` at a closed port; restart backend):

Expected: **502** problem+json with generic detail.

## 6. Cache (SC-003, SC-004)

Manual timing only (no automated wall-clock asserts).

```bash
curl -s -o /dev/null -w "%{http_code} %{time_total}\n" \
  "http://localhost:8080/api/v1/pokemon/133"
curl -s -o /dev/null -w "%{http_code} %{time_total}\n" \
  "http://localhost:8080/api/v1/pokemon/133"
```

Expected: first uncached ≤ **3 s** when PokeAPI is healthy (Eevee worst case);
immediate repeat ≤ **0.2 s**. Zero extra upstream calls proven in
infrastructure / IT WireMock tests.

Recorded (fill in during quickstart validation task):

| Scenario | `time_total` |
|---|---|
| Uncached Eevee (133) | ___ s |
| Cached repeat | ___ s |

## 7. OpenAPI

```bash
curl -s http://localhost:8080/v3/api-docs | python3 -c \
  'import sys,json; p=json.load(sys.stdin)["paths"]; print("/api/v1/pokemon/{id}" in p)'
curl -s -o /dev/null -w "%{http_code}\n" \
  http://localhost:8080/swagger-ui/index.html
```

Expected: path present; responses document 200/400/404/502; Swagger Try it out
works for a valid id.

## 8. Backend verify

```bash
cd backend && ./mvnw -B verify
```

Expected: all modules green; JaCoCo ≥ 80% per module; aggregate HTML under
`web/target/site/jacoco-aggregate` (visibility only).

## Tear down

```bash
docker compose down -v
```
