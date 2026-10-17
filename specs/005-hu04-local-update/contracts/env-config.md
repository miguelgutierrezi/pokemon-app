# Contract: Environment configuration (005)

**Feature**: `005-hu04-local-update`

Extends `specs/004-hu03-data-sync/contracts/env-config.md`.

## Keys added in 005

**None.** Update/delete use the existing Postgres DataSource and Flyway
(V4 adds `version` only). No PokeAPI calls on update/delete.

| Key / property | Source |
|----------------|--------|
| `POSTGRES_*` | 001 — DataSource for JPA + Flyway |
| Jackson fail-on-unknown | `application.yml` —
  `spring.jackson.deserialization.fail-on-unknown-properties=true`
  (app config, not env) |

No Compose or `.env.example` changes.
