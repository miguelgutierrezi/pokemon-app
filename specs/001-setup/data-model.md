# Data Model: Setup (project skeleton)

**Feature**: `001-setup` | **Date**: 2026-10-15 (revised)

No business entities in this feature. The model covers runtime configuration,
migration bookkeeping, and health signaling only.

## Entities

### EnvironmentConfiguration

Settings required to run the stack. Not persisted; supplied via the process
environment / Compose `.env`. Full contract:
[contracts/env-config.md](./contracts/env-config.md).

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `POSTGRES_USER` | string | yes | Non-empty |
| `POSTGRES_PASSWORD` | string | yes | Non-empty; never commit real values |
| `POSTGRES_DB` | string | yes | Non-empty |

The backend's `SPRING_DATASOURCE_*` values are derived from these in
`docker-compose.yml` (and default to `localhost:5432` in `application.yml` for
local runs). Keys for JWT (006) and PokeAPI (002) are added by those features.

**State**: immutable for a process lifetime.

---

### BaselineMigration

Flyway version `1` applied to an empty database.

| Field | Value |
|-------|-------|
| version | `1` (`V1__baseline.sql`) |
| description | `baseline` — creates no tables |
| checksum | Flyway-managed, stored in `flyway_schema_history` |

**State transitions**: `[missing] --(app start)--> [applied]`. Later features
append `V2__...` onward.

---

### HealthStatus

Readiness signal from `/actuator/health`.

| Field | Type | Notes |
|-------|------|-------|
| status | `UP` \| `DOWN` \| `OUT_OF_SERVICE` \| `UNKNOWN` | HTTP 200 when `UP`, 503 when `DOWN`/`OUT_OF_SERVICE` |
| components.db.status | same enum | Visible because `show-components=always` |

**State transitions**: `[DOWN] --(DB reachable + app started)--> [UP]`,
`[UP] --(DB lost)--> [DOWN]`.

## Out of scope

- Pokémon entity and proprietary fields (HU-03, feature 004)
- User collection for authentication (TR-API-AUTH / TR-DB, feature 006)
- Any JPA `@Entity` or cache model
