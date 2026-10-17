# Contract: Environment configuration (004)

**Feature**: `004-hu03-data-sync`

Extends `specs/001-setup/contracts/env-config.md`,
`specs/002-hu01-pokemon-enumeration/contracts/env-config.md`, and
`specs/003-hu02-detailed-view/contracts/env-config.md`.

## Keys added in 004

**None.** Collection import reuses Postgres (Flyway V2/V3) and the existing
PokeAPI client configuration for `fetchDetail` during import.

| Key / property | Source |
|----------------|--------|
| `POSTGRES_*` | 001 — DataSource for JPA + Flyway |
| `POKEAPI_BASE_URL` / timeouts / cache | 002/003 — used only on import |

No Compose or `.env.example` changes are required for HU-03 beyond what
already runs the database and backend.

## Notes

- Seed data applies at migration time (V3); no seed-related env keys.
- JPA uses the same DataSource as JDBC/Flyway; no second database URL.
