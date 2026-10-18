# Quickstart validation: Authentication API (TR-API-AUTH)

**Feature**: `006-auth` | **Date**: 2026-10-17

Contracts: [auth-api.yaml](./contracts/auth-api.yaml),
[route-policy.md](./contracts/route-policy.md),
[env-config.md](./contracts/env-config.md). Data model:
[data-model.md](./data-model.md).

## Prerequisites

- JDK 21, Docker Compose, `jq` (optional)
- Fresh stack migrated through Flyway **V5** (`app_user` + demo seed)
- `JWT_SECRET` set (≥ 32 bytes) — see `.env.example`

Suggested demo credential (confirm hash at implement; labelled **demo**
in README):

| Field | Value |
|-------|--------|
| Email | `demo@pokefolio.dev` |
| Password | `Demo-Passw0rd!` |

## 1. Configure & start

```bash
cp .env.example .env
# set JWT_SECRET=… (≥ 32 bytes), e.g. openssl rand -base64 48
docker compose up --build -d
docker compose ps
curl -sS 'http://localhost:8080/actuator/health'
```

Expected: healthy stack; migrations through **V5**. Missing `JWT_SECRET`
→ compose/backend fail fast (not a silent default).

## 2. Login as demo → token

```bash
TOKEN=$(curl -sS -X POST 'http://localhost:8080/api/v1/auth/login' \
  -H 'Content-Type: application/json' \
  -d '{"email":"demo@pokefolio.dev","password":"Demo-Passw0rd!"}' \
  | jq -r '.accessToken')
echo "$TOKEN"
```

Expected: **200**; `tokenType` `Bearer`; `expiresIn` ~3600; `user.email`
`demo@pokefolio.dev`; password never in body. Login-only ≤ **1 s**
locally (SC-004).

## 3. Import (protected POST) with token

```bash
curl -sS -i -X POST 'http://localhost:8080/api/v1/collection' \
  -H 'Authorization: Bearer '"$TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"nationalNumber":25}'
```

Expected: **201** (or **409** conflict if already present); proceeds under
existing 004 rules. Shared collection — not scoped to the demo user.

## 4. PUT proprietary fields (protected)

```bash
# read current version first if needed
curl -sS 'http://localhost:8080/api/v1/collection/25' | jq '{nationalNumber, version}'

curl -sS -i -X PUT 'http://localhost:8080/api/v1/collection/25' \
  -H 'Authorization: Bearer '"$TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"localizedName":"Sparky","location":"Viridian Forest","tags":["electric","demo"],"version":0}'
```

Expected: **200** with updated proprietary fields and incremented
`version` (adjust `version` to the value from GET). Without a valid
token → **401** (see §6).

## 5. DELETE (protected)

```bash
curl -sS -i -X DELETE 'http://localhost:8080/api/v1/collection/25' \
  -H 'Authorization: Bearer '"$TOKEN"
```

Expected: **204** (or current 005 delete success status); record gone from
collection; catalog unchanged.

## 6. No token on write → 401

```bash
curl -sS -i -X POST 'http://localhost:8080/api/v1/collection' \
  -H 'Content-Type: application/json' \
  -d '{"nationalNumber":1}'
```

Expected: **401**; `type` `urn:problem-type:unauthorized`;
`WWW-Authenticate: Bearer`; problem+json with no stack/package leaks.

## 7. Stale / expired Bearer on public GET → 200

Pagination is **1-based** (`page` default `1`); `page=0` → **400**. There is
no `limit` query parameter — use `size`.

```bash
curl -sS -i 'http://localhost:8080/api/v1/pokemon?page=1&size=1' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.e30.invalid'

curl -sS -i 'http://localhost:8080/api/v1/collection?page=1&size=5' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.e30.invalid'
```

Expected: **200** (subject to existing list rules). Public routes
**ignore** Bearer — a stale client token must not block catalog or
collection reads.

## 8. Deny-by-default (optional smoke)

```bash
# no token → 401
curl -sS -i 'http://localhost:8080/api/v1/does-not-exist'

# valid token → 403
curl -sS -i 'http://localhost:8080/api/v1/does-not-exist' \
  -H 'Authorization: Bearer '"$TOKEN"
```

Expected: **401** then **403** `urn:problem-type:forbidden`.

## 9. Register (optional)

```bash
curl -sS -i -X POST 'http://localhost:8080/api/v1/auth/register' \
  -H 'Content-Type: application/json' \
  -d '{"name":"Misty","email":"misty@example.com","password":"Staryu123!"}'
```

Expected: **201** with AuthSession shape; second register same email
(any casing) → **409** `urn:problem-type:conflict`.

## Results (2026-10-17)

Manual run against `docker compose` with `JWT_SECRET` in local `.env`:

| Check | Result |
|-------|--------|
| Flyway | **V5** applied (`app_user` + demo seed) |
| Demo login | **200** in **0.13 s** (SC-004 ≤ 1 s); `Cache-Control: no-store`; password absent from body |
| Import `#25` | **201** (0.41 s) |
| PUT `#25` | **200**; `version` **0 → 1** |
| DELETE `#25` | **204**; subsequent GET → **404** |
| POST without token | **401** + `WWW-Authenticate: Bearer` + clean `application/problem+json` |
| Stale Bearer on public GETs (§7) | **200** |
| `/api/v1/does-not-exist` | **401** anonymous / **403** authenticated |
| Register | **201**; uppercase duplicate → **409** without echoing the email |
| Login via nginx `:3000` proxy | **200** |
| Generated-password log line | None |
| Swagger UI | Authorize + padlocks only on collection **POST/PUT/DELETE**; empty browser console |
| Compose with empty `JWT_SECRET` | Fails fast |
