# Quickstart validation: HU-02 Detailed View

**Feature**: `003-hu02-detailed-view` | **Date**: 2026-10-16
(validated 2026-10-17 against real PokeAPI)

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
`stats.total` = sum of six bases; `flavorText.text` normalised English
(including `POKéMON` → `Pokémon`); `evolution` tree with `isCurrent: true`
on national number 1; `imageUrl` preferring official artwork when present;
evolution node images are front sprites.

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

Also: `GET /api/v1/pokemon/197` marks only Umbreon `isCurrent: true`.

## 4. Single-stage species

```bash
# Tauros
curl -s "http://localhost:8080/api/v1/pokemon/128" | python3 -c \
  'import sys,json; e=json.load(sys.stdin)["evolution"]; print(e["nationalNumber"], e["isCurrent"], len(e["children"]))'
# Pecharunt (no evolutions)
curl -s "http://localhost:8080/api/v1/pokemon/1025" | python3 -c \
  'import sys,json; e=json.load(sys.stdin)["evolution"]; print(e["nationalNumber"], e["isCurrent"], len(e["children"]))'
```

Expected: `isCurrent` true; `children` empty (length 0).

## 5. Errors (SC-005)

```bash
curl -s -i "http://localhost:8080/api/v1/pokemon/0"
curl -s -i "http://localhost:8080/api/v1/pokemon/abc"
curl -s -i "http://localhost:8080/api/v1/pokemon/9999"
curl -s -i "http://localhost:8080/api/v1/pokemon/10001"
```

Expected: `0` and `abc` → **400** problem+json whose detail names `id`;
`9999` and `10001` → **404** problem+json; never stack traces or upstream URLs.

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

### Recorded results (2026-10-17, real PokeAPI)

Measured by Claude Code review (validated by Miguel):

| Scenario | `time_total` | Notes |
|---|---|---|
| Uncached Bulbasaur (1) | **0.62 s** | SC-001 / SC-003 |
| Uncached Eevee (133) | **0.47 s** | worst-case lineage; SC-003 ≤ 3 s |
| Cached repeat | **3 ms** | also via Nginx `:3000`; SC-004 ≤ 0.2 s |

Behaviour checks (same session):

- Eevee: **8** evolution children in catalog order; Umbreon request marks only
  Umbreon `isCurrent`
- Artwork on detail `imageUrl`; front-sprite thumbnails on evolution nodes
- Tauros (128) and Pecharunt (1025): evolution with **no** children
- `9999` and `10001` → **404**; `0` and `abc` → **400** naming `id`
- OpenAPI documents `/api/v1/pokemon/{id}` with 200/400/404/502

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
