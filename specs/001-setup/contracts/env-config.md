# Contract: Environment configuration

**Feature**: `001-setup`

The machine-readable list of keys is `.env.example` at the repository root.
This document defines the rules those keys must satisfy.

## Rules

1. Every key below MUST appear in `.env.example` with a non-secret placeholder.
2. Real secrets MUST NOT be committed; `.env` MUST be git-ignored.
3. Compose MUST fail fast when a required key is missing, naming the key
   (`${VAR:?...}` syntax).
4. Each later feature adds only the keys it uses (constitution IX): e.g.
   `POKEAPI_BASE_URL` in 002, `JWT_SECRET` in 006.

## Keys (001)

| Key | Example placeholder | Consumed by |
|-----|---------------------|-------------|
| `POSTGRES_USER` | `pokemon` | postgres; backend via Compose |
| `POSTGRES_PASSWORD` | `change-me` | postgres; backend via Compose |
| `POSTGRES_DB` | `pokemon` | postgres; backend via Compose |

Derived in `docker-compose.yml` (not in `.env`):

| Backend variable | Value |
|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://postgres:5432/${POSTGRES_DB}` |
| `SPRING_DATASOURCE_USERNAME` | `${POSTGRES_USER}` |
| `SPRING_DATASOURCE_PASSWORD` | `${POSTGRES_PASSWORD}` |

## Ports

| Service | Host port | Notes |
|---|---|---|
| frontend (Nginx) | 3000 | UI + `/api/` proxy |
| backend | 8080 | Direct API + `/actuator/health` |
| postgres | 5432 | Published for local backend runs and DB tools |

## Frontend proxy contract

- Browser calls to `/api/**` on port 3000 are forwarded by Nginx to
  `http://backend:8080/api/**` with the **path unchanged**.
- Business endpoints live under `/api/v1` in the backend (constitution IV).
- `/actuator/**` is not proxied; health checks go to port 8080 directly.
